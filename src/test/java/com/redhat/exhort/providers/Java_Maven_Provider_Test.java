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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class Java_Maven_Provider_Test {
  // test folder are located at src/test/resources/tst_manifests
  // each folder should contain:
  // - pom.xml: the target manifest for testing
  // - stack_expected_dot_graph: the dot graph expected to be provided by invoking provideStack
  // - component_expected_json: the json expected to be provided by invoking provideComponent
  static Stream<String> testFolders() {
    return Stream.of(
      "pom_deps_with_ignore_on_artifact",
      "pom_deps_with_ignore_on_dependency",
      "pom_deps_with_ignore_on_group",
      "pom_deps_with_ignore_on_version",
      "pom_deps_with_ignore_on_wrong",
      "pom_deps_with_no_ignore"
    );
  }

  @ParameterizedTest
  @MethodSource("testFolders")
  void test_the_provideStack_a_pom_with_no_deps_ignored(String testFolder) throws IOException, InterruptedException {
    // create temp file hosting our sut pom.xml
    var tmpPomFile = Files.createTempFile("exhort_test_", ".xml");
    try (var is = getClass().getModule().getResourceAsStream(String.join("/","tst_manifests", testFolder, "pom.xml"))) {
      Files.write(tmpPomFile, is.readAllBytes());
    }
    // load expected dot graph
    String expectedDotGraph;
    try (var is = getClass().getModule().getResourceAsStream(String.join("/","tst_manifests", testFolder, "stack_expected_dot_graph"))) {
      expectedDotGraph = new String(is.readAllBytes());
    }
    // when providing stack content for our pom
    var content = new JavaMavenProvider("maven").provideStack(tmpPomFile);
    // cleanup
    Files.deleteIfExists(tmpPomFile);
    // verify expected dot graph is returned
    assertThat(content.type).isEqualTo("text/vnd.graphviz");
    assertThat(new String(content.buffer).replaceAll("\\s+",""))
      .isEqualTo(expectedDotGraph.replaceAll("\\s+",""));
  }

  @ParameterizedTest
  @MethodSource("testFolders")
  void test_the_provideComponent_a_pom_with_no_deps_ignored(String testFolder) throws IOException, InterruptedException {
    // load the pom target pom file
    byte[] targetPom;
    try (var is = getClass().getModule().getResourceAsStream(String.join("/","tst_manifests", testFolder, "pom.xml"))) {
      targetPom = is.readAllBytes();
    }
    // load expected Json
    String expectedJson = "";
    try (var is = getClass().getModule().getResourceAsStream(String.join("/","tst_manifests", testFolder, "component_expected_json"))) {
      expectedJson = new String(is.readAllBytes());
    }
    // when providing component content for our pom
    var content = new JavaMavenProvider("maven").provideComponent(targetPom);
    // verify expected dot graph is returned
    assertThat(content.type).isEqualTo("application/json");
    assertThat(new String(content.buffer).replaceAll("\\s+",""))
      .isEqualTo(expectedJson.replaceAll("\\s+",""));
  }
}
