/*
 * Copyright © 2023 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.exhort.providers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.exhort.Provider;
import com.redhat.exhort.representation.InputRepresentation;
import com.redhat.exhort.representation.RepresentationResponse;
import com.redhat.exhort.tools.Operations;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Concrete implementation of the {@link Provider} used for converting dependency trees
 * for Java Maven projects (pom.xml) into a content Dot Graphs for Stack analysis or Json for
 * Component analysis.
 **/
public final class JavaScriptNpmProvider extends Provider {
  public static void main(String[] args) {
    JavaScriptNpmProvider provider = new JavaScriptNpmProvider("npm");
    try {
      provider.provideStack(Path.of("/tmp/ecommerce-store/backend/package.json"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public JavaScriptNpmProvider(final String ecosystem) {
    super(ecosystem);
  }

  @Override
  public Content provideStack(final Path manifestPath) throws IOException {
    // check for custom npm executable
     Map<String,Object> jsonDependenciesNpm = getResult(manifestPath);
    InputRepresentation inputRepresentation = new InputRepresentation(ecosystem);
    RepresentationResponse response = inputRepresentation.sendInputForProcessing(jsonDependenciesNpm);
    return new Content(response.getActualContent().getBytes(StandardCharsets.UTF_8), "application/json");
  }

  private Map<String, Object> getResult(Path manifestPath) throws IOException {
    var npm = Operations.getCustomPathOrElse("npm");
    // clean command used to clean build target
    Path packageLockJson = Path.of(manifestPath.getParent().toString(), "package-lock.json");
    if (!packageLockJson.toFile().exists())
    {
      var createPackageLock = new String[]{npm, "npm ", "i","--package-lock-only", "--prefix", manifestPath.getParent().toString()};
      // execute the clean command
      Operations.runProcess(createPackageLock);
    }

    var npmAllDeps = new String[]{npm, "ls", "--all", "--omit=dev" , "--package-lock-only", "--json", "--prefix", manifestPath.getParent().toString() };
    // execute the clean command
    String npmOutput = Operations.runProcessGetOutput(npmAllDeps);
    Map<String, Object> jsonDependenciesNpm = objectMapper.readValue(npmOutput, new TypeReference<HashMap<String, Object>>() {});

    return jsonDependenciesNpm;
  }



  @Override
  public Content provideComponent(byte[] manifestContent) throws IOException {
    // check for custom mvn executable
    var mvn = Operations.getCustomPathOrElse("mvn");
    // save content in temporary file
    var originPom = Files.createTempFile("exhort_orig_pom_", ".xml");
    Files.write(originPom, manifestContent);
    // create a temp file for storing the effective pom in
    var tmpEffPom = Files.createTempFile("exhort_eff_pom_", ".xml");
    // build effective pom command
    var mvnEffPomCmd = new String[]{
      mvn,
      "-q",
      "clean",
      "help:effective-pom",
      String.format("-Doutput=%s", tmpEffPom.toString()),
      "-f", originPom.toString()
    };
    // execute the effective pom command
    Operations.runProcess(mvnEffPomCmd);
    // if we have dependencies marked as ignored grab ignored dependencies from the original pom
    // the effective-pom goal doesn't carry comments
    var ignored = this.getDependencies(originPom).stream()
      .filter(i -> i.ignored)
      .collect(Collectors.toList());
    // get all dependencies from effective pom as packages excluding the ignored ones
    var packages = this.getDependencies(tmpEffPom).stream()
      .dropWhile(ignored::contains)
      .map(DependencyAggregator::toPackage)
      .toArray(PackageAggregator[]::new);
    // serialize packages to json array of as a byte array
    var packagesJson = new ObjectMapper().writeValueAsBytes(packages);
    // build and return content for constructing request to the backend
    return new Content(packagesJson, "application/json");
  }

  /**
   * Get a list of dependencies including a field marking if it's this dependency is ignored based
   * on a {@literal <!--exhortignore-->} comment attached to the dependency.
   *
   * @param manifestPath the Path for the manifest file
   * @return a list of DependencyAggregator, implemented toString will return format suited for the
   *    dependency:tree goal's excludes property.
   *    ie. group-id:artifact-id:*:version (the * marks any type, if no version specified,
   *    * will be used.
   * @throws IOException when failed to load or parse the manifest file
   */
  private List<DependencyAggregator> getDependencies(final Path manifestPath) throws IOException {
    var dependencyAggregators = new ArrayList<DependencyAggregator>();

    XMLStreamReader reader = null;
    try {
      //get a xml stream reader for the manifest file
      reader = XMLInputFactory.newInstance().createXMLStreamReader(Files.newInputStream(manifestPath));
      // the following dependencyIgnore object is used to aggregate dependency data over iterations
      // when a "dependency" tag starts, it will be initiated,
      // when a "dependency" tag ends, it will be parsed, act upon, and reset
      DependencyAggregator dependencyAggregator = null;
      while (reader.hasNext()) {
        reader.next(); // get the next event
        if (reader.isStartElement() && "dependency".equals(reader.getLocalName())) {
          // starting "dependency" tag, initiate aggregator
          dependencyAggregator = new DependencyAggregator();
          continue;
        }

        // if dependency aggregator haven't been initiated,
        // we're currently not iterating over a "dependency" tag - no need for further parsing
        if (!Objects.isNull(dependencyAggregator)) {
          // if we hit an ignore comment, mark aggregator to be ignored
          if (reader.getEventType() == XMLStreamConstants.COMMENT
              && "exhortignore".equals(reader.getText().strip())
          ) {
            dependencyAggregator.ignored = true;
            continue;
          }

          if (reader.isStartElement()) {
            // NOTE if we want to include "scope" tags in ignore,
            // add a case here and a property in DependencyIgnore
            switch (reader.getLocalName()) {
              case "groupId": // starting "groupId" tag, get next event and set to aggregator
                reader.next();
                dependencyAggregator.groupId = reader.getText();
                break;
              case "artifactId": // starting "artifactId" tag, get next event and set to aggregator
                reader.next();
                dependencyAggregator.artifactId = reader.getText();
                break;
              case "version": // starting "version" tag, get next event and set to aggregator
                reader.next();
                dependencyAggregator.version = reader.getText();
                break;
            }
          }

          if (reader.isEndElement() && "dependency".equals(reader.getLocalName())) {
            // add object to list and reset dependency aggregator
            dependencyAggregators.add(dependencyAggregator);
            dependencyAggregator = null;
          }
        }
      }
    } catch (XMLStreamException exc) {
      throw new IOException(exc);
    } finally {
      if (!Objects.isNull(reader)) {
        try {
          reader.close(); // close stream if open
        } catch (XMLStreamException e) {
          //
        }
      }
    }

    return dependencyAggregators;
  }

  // NOTE if we want to include "scope" tags in ignore,
  // add property here and a case in the start-element-switch in the getIgnored method
  /** Aggregator class for aggregating Dependency data over stream iterations, **/
  private final static class DependencyAggregator {
    private String groupId = "";
    private String artifactId = "";
    private String version = "*";
    boolean ignored = false;

    /**
     * Get the string representation of the dependency to use as excludes
     * @return an exclude string for the dependency:tree plugin, ie. group-id:artifact-id:*:version
     */
    @Override
    public String toString() {
      // NOTE if you add scope, don't forget to replace the * with its value
      return String.format("%s:%s:*:%s", groupId, artifactId, version);
    }

    /**
     * Convert the {@link DependencyAggregator} object to a {@link PackageAggregator}
     * @return a new instance of {@link PackageAggregator}
     */
    public PackageAggregator toPackage() {
      return new PackageAggregator(String.format("%s:%s", groupId, artifactId), version);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof DependencyAggregator)) return false;
      var that = (DependencyAggregator) o;
      // NOTE we do not compare the ignored field
      // This is required for comparing pom.xml with effective_pom.xml as the latter doesn't
      // contain comments indicating ignore
      return Objects.equals(this.groupId, that.groupId) &&
        Objects.equals(this.artifactId, that.artifactId) &&
        Objects.equals(this.version, that.version);
    }
  }
}
