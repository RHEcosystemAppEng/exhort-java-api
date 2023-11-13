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

import com.redhat.exhort.Api;
import com.redhat.exhort.ExhortTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
@ExtendWith(HelperExtension.class)
class Golang_Modules_Provider_Test extends ExhortTest {
  // test folder are located at src/test/resources/tst_manifests/npm
  // each folder should contain:
  // - package.json: the target manifest for testing
  // - expected_sbom.json: the SBOM expected to be provided
  static Stream<String> testFolders() {
    return Stream.of(
      "go_mod_light_no_ignore",
      "go_mod_no_ignore",
      "go_mod_with_ignore",
      "go_mod_with_all_ignore",
      "go_mod_with_one_ignored_prefix_go",
      "go_mod_no_path"
    );
  }

  @ParameterizedTest
  @MethodSource("testFolders")
  void test_the_provideStack(String testFolder) throws IOException, InterruptedException {
    // create temp file hosting our sut package.json
    var tmpGoModulesDir = Files.createTempDirectory("exhort_test_");
    var tmpGolangFile = Files.createFile(tmpGoModulesDir.resolve("go.mod"));
    try (var is = getClass().getModule().getResourceAsStream(String.join("/","tst_manifests", "golang", testFolder, "go.mod"))) {
      Files.write(tmpGolangFile, is.readAllBytes());
    }
    // load expected SBOM
    String expectedSbom;
    try (var is = getClass().getModule().getResourceAsStream(String.join("/","tst_manifests", "golang", testFolder, "expected_sbom_stack_analysis.json"))) {
      expectedSbom = new String(is.readAllBytes());
    }
    // when providing stack content for our pom
    var content = new GoModulesProvider().provideStack(tmpGolangFile);
    // cleanup
    Files.deleteIfExists(tmpGolangFile);
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
    try (var is = getClass().getModule().getResourceAsStream(String.join("/","tst_manifests", "golang", testFolder, "go.mod"))) {
      targetPom = is.readAllBytes();
    }
    // load expected SBOM
    String expectedSbom = "";
    try (var is = getClass().getModule().getResourceAsStream(String.join("/","tst_manifests", "golang", testFolder, "expected_sbom_component_analysis.json"))) {
      expectedSbom = new String(is.readAllBytes());
    }
    // when providing component content for our pom
    var content = new GoModulesProvider().provideComponent(targetPom);
    // verify expected SBOM is returned
    assertThat(content.type).isEqualTo(Api.CYCLONEDX_MEDIA_TYPE);
    assertThat(dropIgnored(new String(content.buffer)))
      .isEqualTo(dropIgnored(expectedSbom));


  }


  @Test
  void Test_The_ProvideComponent_Path_Should_Throw_Exception() {

    GoModulesProvider goModulesProvider = new GoModulesProvider();
    assertThatIllegalArgumentException().isThrownBy(() -> {
      goModulesProvider.provideComponent(Path.of("."));
    }).withMessage("provideComponent with file system path for GoModules package manager not implemented yet");


  }

  private String dropIgnored(String s) {
    return s.replaceAll("\\s+","").replaceAll("\"timestamp\":\"[a-zA-Z0-9\\-\\:]+\",", "");
  }
}
