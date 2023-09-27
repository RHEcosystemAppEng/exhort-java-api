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

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.redhat.exhort.Api;

@ExtendWith(HelperExtension.class)
class Java_Maven_Provider_Test {
  private static System.Logger log = System.getLogger("Java_Maven_Provider_Test");
  // test folder are located at src/test/resources/tst_manifests
  // each folder should contain:
  // - pom.xml: the target manifest for testing
  // - expected_sbom.json: the SBOM expected to be provided
  static Stream<String> testFolders() {
    return Stream.of(
      "deps_no_trivial_with_ignore",
      "deps_with_ignore_on_artifact",
      "deps_with_ignore_on_dependency",
      "deps_with_ignore_on_group",
      "deps_with_ignore_on_version",
      "deps_with_ignore_on_wrong",
      "deps_with_no_ignore"
    );
  }

  @ParameterizedTest
  @MethodSource("testFolders")
  void test_the_provideStack(String testFolder) throws IOException, InterruptedException {
    // create temp file hosting our sut pom.xml
    var tmpPomFile = Files.createTempFile("exhort_test_", ".xml");
    System.out.print("the test folder is : " + testFolder);
    try (var is = getClass().getModule().getResourceAsStream(String.join("/","tst_manifests", "maven", testFolder, "pom.xml"))) {
      Files.write(tmpPomFile, is.readAllBytes());
    }
    // load expected SBOM
    String expectedSbom;
    try (var is = getClass().getModule().getResourceAsStream(String.join("/","tst_manifests", "maven", testFolder, "expected_stack_sbom.json"))) {
      expectedSbom = new String(is.readAllBytes());
    }
    // when providing stack content for our pom
    var content = new JavaMavenProvider().provideStack(tmpPomFile);
    // cleanup
    Files.deleteIfExists(tmpPomFile);
    // verify expected SBOM is returned
    assertThat(content.type).isEqualTo(Api.CYCLONEDX_MEDIA_TYPE);
    assertThat(dropIgnored(new String(content.buffer)))
      .isEqualTo(dropIgnored(expectedSbom));
  }

  @ParameterizedTest
  @MethodSource("testFolders")
  void test_the_provideComponent(String testFolder) throws IOException, InterruptedException {
    // load the pom target pom file
    byte[] targetPom;
    try (var is = getClass().getModule().getResourceAsStream(String.join("/","tst_manifests", "maven", testFolder, "pom.xml"))) {
      targetPom = is.readAllBytes();
    }
    // load expected SBOM
    String expectedSbom = "";
    try (var is = getClass().getModule().getResourceAsStream(String.join("/","tst_manifests", "maven", testFolder, "expected_component_sbom.json"))) {
      expectedSbom = new String(is.readAllBytes());
    }
    // when providing component content for our pom
    var content = new JavaMavenProvider().provideComponent(targetPom);
    // verify expected SBOM is returned
    assertThat(content.type).isEqualTo(Api.CYCLONEDX_MEDIA_TYPE);
    assertThat(dropIgnored(new String(content.buffer)))
      .isEqualTo(dropIgnored(expectedSbom));
  }
  @ParameterizedTest
  @MethodSource("testFolders")
  void test_the_provideComponent_With_Path(String testFolder) throws IOException, InterruptedException {
    // load the pom target pom file
    // create temp file hosting our sut pom.xml
    var tmpPomFile = Files.createTempFile("exhort_test_", ".xml");
    try (var is = getClass().getModule().getResourceAsStream(String.join("/","tst_manifests", "maven", testFolder, "pom.xml"))) {
      Files.write(tmpPomFile, is.readAllBytes());
    }
    // load expected SBOM
    String expectedSbom = "";
    try (var is = getClass().getModule().getResourceAsStream(String.join("/","tst_manifests", "maven", testFolder, "expected_component_sbom.json"))) {
      expectedSbom = new String(is.readAllBytes());
    }
    // when providing component content for our pom
    var content = new JavaMavenProvider().provideComponent(tmpPomFile);
    // verify expected SBOM is returned
    assertThat(content.type).isEqualTo(Api.CYCLONEDX_MEDIA_TYPE);
    assertThat(dropIgnored(new String(content.buffer)))
      .isEqualTo(dropIgnored(expectedSbom));
  }

  private String dropIgnored(String s) {
    return s.replaceAll("\\s+","").replaceAll("\"timestamp\":\"[a-zA-Z0-9\\-\\:]+\"", "");
  }
}
