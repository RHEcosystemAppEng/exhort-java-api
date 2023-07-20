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
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Concrete implementation of the {@link Provider} used for converting dependency trees
 * for npm projects (package.json) into a SBOM content for Stack analysis or Component analysis.
 **/
public final class JavaScriptNpmProvider extends Provider {
  public static void main(String[] args) {
    JavaScriptNpmProvider provider = new JavaScriptNpmProvider("npm");

    try {
      byte[] manifestContent = Files.readAllBytes(Path.of("/tmp/ecommerce-store/backend/package.json"));
      String sbomForComponent = new String(provider.provideComponent(manifestContent).buffer);
      String sbomForStackAnalysis = new String(provider.provideStack(Path.of("/tmp/ecommerce-store/backend/package.json")).buffer);
      System.out.print("hey");
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
     Map<String,Object> jsonDependenciesNpm = getDependencyTree(manifestPath,true);
    InputRepresentation inputRepresentation = new InputRepresentation(ecosystem);
    RepresentationResponse response = inputRepresentation.sendInputForProcessing(jsonDependenciesNpm);
    return new Content(response.getActualContent().getBytes(StandardCharsets.UTF_8), "application/json");
  }

  private Map<String, Object> getDependencyTree(Path manifestPath,boolean includeAllDeps) throws IOException {
    var npm = Operations.getCustomPathOrElse("npm");
    // clean command used to clean build target
    Path packageLockJson = Path.of(manifestPath.getParent().toString(), "package-lock.json");
    if (!packageLockJson.toFile().exists())
    {
      var createPackageLock = new String[]{npm, "i","--package-lock-only", "--prefix", manifestPath.getParent().toString()};
      // execute the clean command
      Operations.runProcess(createPackageLock);
    }

    var npmAllDeps = new String[]{npm, "ls", includeAllDeps ? "--all" : "", "--omit=dev" , "--package-lock-only", "--json", "--prefix", manifestPath.getParent().toString() };
    // execute the clean command
    String npmOutput = Operations.runProcessGetOutput(npmAllDeps);
    Map<String, Object> jsonDependenciesNpm = objectMapper.readValue(npmOutput, new TypeReference<HashMap<String, Object>>() {});

    return jsonDependenciesNpm;
  }



  @Override
  public Content provideComponent(byte[] manifestContent) throws IOException {
    // check for custom npm executable
    Map<String,Object> jsonDependenciesNpm = getDependencyTree(manifestContent);
    InputRepresentation inputRepresentation = new InputRepresentation(ecosystem);
    RepresentationResponse response = inputRepresentation.sendInputForProcessing(jsonDependenciesNpm);
    return new Content(response.getActualContent().getBytes(StandardCharsets.UTF_8), "application/json");
  }

  private Map<String, Object> getDependencyTree(byte[] manifestContent) {
    Map<String,Object> jsonDependenciesNpm;
    try {
      Path path = Paths.get(Paths.get(".").toAbsolutePath().normalize().toString(), "package.json");
      Files.deleteIfExists(path);
      Path manifestPath = Files.createFile(path);
      Files.write(manifestPath,manifestContent);
      jsonDependenciesNpm = getDependencyTree(manifestPath,false);

      Files.delete(manifestPath);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return jsonDependenciesNpm;
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
  }
