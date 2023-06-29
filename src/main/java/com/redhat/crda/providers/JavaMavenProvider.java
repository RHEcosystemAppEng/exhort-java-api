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
package com.redhat.crda.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.crda.Provider;
import com.redhat.crda.tools.Operations;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Concrete implementation of the {@link Provider} used for converting dependency trees
 * for Java Maven projects (pom.xml) into a content Dot Graphs for Stack analysis or Json for
 * Component analysis.
 **/
public final class JavaMavenProvider extends Provider {
  public JavaMavenProvider(final String ecosystem) {
    super(ecosystem);
  }

  @Override
  public Content provideStack(final Path manifestPath) throws IOException {
    // check for custom mvn executable
    var mvn = Operations.getCustomPathOrElse("mvn");
    // clean command used to clean build target
    var mvnCleanCmd = new String[]{mvn, "-q", "clean", "-f", manifestPath.toString()};
    // execute the clean command
    Operations.runProcess(mvnCleanCmd);
    // create a temp file for storing the dependency tree in
    var tmpFile = Files.createTempFile("crda_dot_graph_", null);
    // the tree command will build the project and create the dependency tree in the temp file
    var mvnTreeCmd = new ArrayList<String>() {{
      add(mvn);
      add("-q");
      add("dependency:tree");
      add("-DoutputType=dot");
      add(String.format("-DoutputFile=%s", tmpFile.toString()));
      add("-f");
      add(manifestPath.toString());
    }};
    // if we have dependencies marked as ignored, exclude them from the tree command
    var ignored = this.getDependencies(manifestPath).stream()
      .filter(d -> d.ignored)
      .map(DependencyAggregator::toString)
      .collect(Collectors.joining(","));
    if (ignored.length() > 0) {
      mvnTreeCmd.add(String.format("-Dexcludes=%s", ignored));
    }
    // execute the tree command
    Operations.runProcess(mvnTreeCmd.toArray(String[]::new));
    // build and return content for constructing request to the backend
    return new Content(Files.readAllBytes(tmpFile), "text/vnd.graphviz");
  }

  @Override
  public Content provideComponent(byte[] manifestContent) throws IOException {
    // check for custom mvn executable
    var mvn = Operations.getCustomPathOrElse("mvn");
    // save content in temporary file
    var originPom = Files.createTempFile("crda_orig_pom_", ".xml");
    Files.write(originPom, manifestContent);
    // create a temp file for storing the effective pom in
    var tmpEffPom = Files.createTempFile("crda_eff_pom_", ".xml");
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
    var packages = Objects.requireNonNull(this.getDependencies(tmpEffPom)).stream()
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
   * on a {@literal <!--crdaignore-->} comment attached to the dependency.
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
              && "crdaignore".equals(reader.getText().strip())
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
      return this.ignored == that.ignored &&
        Objects.equals(this.groupId, that.groupId) &&
        Objects.equals(this.artifactId, that.artifactId) &&
        Objects.equals(this.version, that.version);
    }
  }
}
