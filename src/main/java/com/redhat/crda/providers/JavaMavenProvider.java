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

public final class JavaMavenProvider implements Provider {
  @Override
  public Content ProvideFor(final Path manifestPath) throws IOException, InterruptedException {
    var mvnCleanCmd = new String[]{"mvn", "-q", "clean", "-f", manifestPath.toString()};
    Operations.runProcess(mvnCleanCmd);

    var tmpFile = Files.createTempFile(null, null);
    var mvnTreeCmd = new ArrayList<String>() {{
      add("mvn");
      add("-q");
      add("dependency:tree");
      add("-DoutputType=dot");
      add(String.format("-DoutputFile=%s", tmpFile.toString()));
    }};

    var ignored = this.getIgnored(manifestPath);
    if (!ignored.isEmpty()) {
      mvnTreeCmd.add(String.format("-Dexcludes=%s", String.join(",", ignored)));
    }
    Operations.runProcess(mvnTreeCmd.toArray(String[]::new));

    return new Content(Files.readAllBytes(tmpFile), "text/vnd.graphviz");
  }

  public List<String> getIgnored(final Path manifestPath) throws IOException {
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
