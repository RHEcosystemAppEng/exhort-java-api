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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class Java_Maven_Provider_Test {
  @ParameterizedTest
  @ValueSource(strings = {
    "pom_deps_with_no_ignore",
    "pom_deps_with_ignore_on_group",
    "pom_deps_with_ignore_on_dependency",
    "pom_deps_with_ignore_on_artifact",
    "pom_deps_with_ignore_on_version",
    "pom_deps_with_ignore_on_wrong"
  })
  void test_the_provideFor_a_pom_with_no_deps_ignored(String testFolder) throws IOException, InterruptedException {
    // create temp file hosting our sut pom.xml
    var tmpPomFile = Files.createTempFile(null, null);
    try (var is = getClass().getModule().getResourceAsStream(String.join("/","tst_manifests", testFolder, "pom.xml"))) {
      Files.write(tmpPomFile, is.readAllBytes());
    }
    // load expected dot graph
    String expectedDotGraph;
    try (var is = getClass().getModule().getResourceAsStream(String.join("/","tst_manifests", testFolder, "expected_dot_graph"))) {
      expectedDotGraph = new String(is.readAllBytes());
    }
    // when providing for our pom
    var content = new JavaMavenProvider().ProvideFor(tmpPomFile);
    // verify expected dot graph is returned
    assertThat(content.type).isEqualTo("text/vnd.graphviz");
    assertThat(new String(content.buffer).replaceAll("\\s+",""))
      .isEqualTo(expectedDotGraph.replaceAll("\\s+",""));
    // cleanup
    Files.deleteIfExists(tmpPomFile);
  }
}
