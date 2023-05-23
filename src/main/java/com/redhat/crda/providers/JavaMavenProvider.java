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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.redhat.crda.Provider;
import com.redhat.crda.tools.Operations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete implementation of the {@link Provider} used for converting dependency trees
 * for Java Maven projects (pom.xml) into a content Dot Graphs.
 * The text/vnd.graphviz is used to relay the content type.
 **/
public final class JavaMavenProvider implements Provider {
  @Override
  public Content ProvideFor(final Path manifestPath) throws IOException {
    // clean command used to clean build target
    var mvnCleanCmd = new String[]{"mvn", "-q", "clean", "-f", manifestPath.toString()};
    // execute the clean command
    Operations.runProcess(mvnCleanCmd);
    // create a temp file for storing the dependency tree in
    var tmpFile = Files.createTempFile(null, null);
    // the tree command will build the project and create the dependency tree in the temp file
    var mvnTreeCmd = new ArrayList<String>() {{
      add("mvn");
      add("-q");
      add("dependency:tree");
      add("-DoutputType=dot");
      add(String.format("-DoutputFile=%s", tmpFile.toString()));
      add("-f");
      add(manifestPath.toString());
    }};
    // if we have dependencies marked as ignored, exclude them from the tree command
    var ignored = this.getIgnored(manifestPath);
    if (!ignored.isEmpty()) {
      mvnTreeCmd.add(String.format("-Dexcludes=%s", String.join(",", ignored)));
    }
    // execute the tree command
    Operations.runProcess(mvnTreeCmd.toArray(String[]::new));

    return new Content(Files.readAllBytes(tmpFile), "text/vnd.graphviz");
  }

  /**
   * Get a list of dependencies to be ignored from the report based on a
   * {@literal <!--crdaignore-->} comment attached to the dependency.
   *
   * @param manifestPath the Path for the manifest file
   * @return a list of string in a format suited for the dependency:tree goal's excludes property.
   *    ie. group-id:artifact-id:*:version (the * marks any type, if not version specified,
   *    * will be used.
   * @throws IOException when failed to load the manifest file
   */
  private List<String> getIgnored(final Path manifestPath) throws IOException {
    record Dependency(String groupId, String artifactId, String version){}
    record Pom(Dependency[] dependencies){}

    var ignoredList = new ArrayList<String>();

    var mapper = new XmlMapper()
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    var con = mapper.readValue(Files.newInputStream(manifestPath), Pom.class);
    for (var dep : con.dependencies()) {
      if (false) { // TODO replace this with condition for crdaignore comment
        var version = dep.version != null ? dep.version : "*";
        ignoredList.add(String.format("%s:%s:*:%s", dep.groupId, dep.artifactId, version));
      }
    }

    return ignoredList;
  }
}
