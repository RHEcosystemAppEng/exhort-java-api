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
import com.redhat.exhort.utils.PythonControllerBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@ExtendWith(PythonEnvironmentExtension.class)
class Python_Provider_Test extends ExhortTest {

  static Stream<String> testFolders() {
    return Stream.of(
"pip_requirements_txt_no_ignore",
        "pip_requirements_txt_ignore"

    );
  }

//  @RegisterExtension
//  private PythonEnvironmentExtension pythonEnvironmentExtension = new PythonEnvironmentExtension();

  public Python_Provider_Test(PythonControllerBase pythonController) {
    this.pythonController = pythonController;
    this.pythonPipProvider = new PythonPipProvider();
    this.pythonPipProvider.setPythonController(pythonController);
  }

  private PythonControllerBase pythonController;
  private PythonPipProvider pythonPipProvider;
  @EnabledIfEnvironmentVariable(named = "RUN_PYTHON_BIN",matches = "true")
  @ParameterizedTest
  @MethodSource("testFolders")
  void test_the_provideStack(String testFolder) throws IOException, InterruptedException {
    // create temp file hosting our sut package.json
    var tmpPythonModuleDir = Files.createTempDirectory("exhort_test_");
    var tmpPythonFile = Files.createFile(tmpPythonModuleDir.resolve("requirements.txt"));
    try (var is = getClass().getModule().getResourceAsStream(String.join("/","tst_manifests", "pip", testFolder, "requirements.txt"))) {
      Files.write(tmpPythonFile, is.readAllBytes());
    }
    // load expected SBOM
    String expectedSbom;
    try (var is = getClass().getModule().getResourceAsStream(String.join("/","tst_manifests", "pip", testFolder, "expected_stack_sbom.json"))) {
      expectedSbom = new String(is.readAllBytes());
    }
    // when providing stack content for our pom
    var content = this.pythonPipProvider.provideStack(tmpPythonFile);
    // cleanup
    Files.deleteIfExists(tmpPythonFile);
    Files.deleteIfExists(tmpPythonModuleDir);
    // verify expected SBOM is returned
    assertThat(content.type).isEqualTo(Api.CYCLONEDX_MEDIA_TYPE);
    assertThat(dropIgnored(new String(content.buffer)))
      .isEqualTo(dropIgnored(expectedSbom));
  }

  @EnabledIfEnvironmentVariable(named = "RUN_PYTHON_BIN",matches = "true")
  @ParameterizedTest
  @MethodSource("testFolders")
  void test_the_provideComponent(String testFolder) throws IOException, InterruptedException {
    // load the pom target pom file
    byte[] targetRequirementsTxt;
    try (var is = getClass().getModule().getResourceAsStream(String.join("/","tst_manifests", "pip", testFolder, "requirements.txt"))) {
      targetRequirementsTxt = is.readAllBytes();
    }
    // load expected SBOM
    String expectedSbom = "";
    try (var is = getClass().getModule().getResourceAsStream(String.join("/","tst_manifests", "pip", testFolder, "expected_component_sbom.json"))) {
      expectedSbom = new String(is.readAllBytes());
    }
    // when providing component content for our pom
    var content = this.pythonPipProvider.provideComponent(targetRequirementsTxt);
    // verify expected SBOM is returned
    assertThat(content.type).isEqualTo(Api.CYCLONEDX_MEDIA_TYPE);
    assertThat(dropIgnored(new String(content.buffer)))
      .isEqualTo(dropIgnored(expectedSbom));


  }


  @ParameterizedTest
  @MethodSource("testFolders")
  void test_the_provideStack_with_properties(String testFolder) throws IOException, InterruptedException {
    // create temp file hosting our sut package.json
    var tmpPythonModuleDir = Files.createTempDirectory("exhort_test_");
    var tmpPythonFile = Files.createFile(tmpPythonModuleDir.resolve("requirements.txt"));
    try (var is = getClass().getModule().getResourceAsStream(String.join("/","tst_manifests", "pip", testFolder, "requirements.txt"))) {
      Files.write(tmpPythonFile, is.readAllBytes());
    }
    // load expected SBOM
    String expectedSbom;
    try (var is = getClass().getModule().getResourceAsStream(String.join("/","tst_manifests", "pip", testFolder, "expected_stack_sbom.json"))) {
      expectedSbom = new String(is.readAllBytes());
    }
    // when providing stack content for our pom
    var content = this.pythonPipProvider.provideStack(tmpPythonFile);
  String pipShowContent = this.getStringFromFile("tst_manifests", "pip", "pip-show.txt");
  String pipFreezeContent = this.getStringFromFile("tst_manifests", "pip", "pip-freeze-all.txt");
  String base64PipShow = new String(Base64.getEncoder().encode(pipShowContent.getBytes()));
  String base64PipFreeze = new String(Base64.getEncoder().encode(pipFreezeContent.getBytes()));
  System.setProperty("EXHORT_PIP_SHOW",base64PipShow);
  System.setProperty("EXHORT_PIP_FREEZE",base64PipFreeze);
    // cleanup
    Files.deleteIfExists(tmpPythonFile);
    Files.deleteIfExists(tmpPythonModuleDir);
   System.clearProperty("EXHORT_PIP_SHOW");
   System.clearProperty("EXHORT_PIP_FREEZE");
    // verify expected SBOM is returned
    assertThat(content.type).isEqualTo(Api.CYCLONEDX_MEDIA_TYPE);
    assertThat(dropIgnored(new String(content.buffer)))
      .isEqualTo(dropIgnored(expectedSbom));
  }

  @ParameterizedTest
  @MethodSource("testFolders")
  void test_the_provideComponent_with_properties(String testFolder) throws IOException, InterruptedException {
    // load the pom target pom file
    byte[] targetRequirementsTxt;
    try (var is = getClass().getModule().getResourceAsStream(String.join("/","tst_manifests", "pip", testFolder, "requirements.txt"))) {
      targetRequirementsTxt = is.readAllBytes();
    }
    // load expected SBOM
    String expectedSbom = "";
    try (var is = getClass().getModule().getResourceAsStream(String.join("/","tst_manifests", "pip", testFolder, "expected_component_sbom.json"))) {
      expectedSbom = new String(is.readAllBytes());
    }
    String pipShowContent = this.getStringFromFile("tst_manifests", "pip", "pip-show.txt");
    String pipFreezeContent = this.getStringFromFile("tst_manifests", "pip", "pip-freeze-all.txt");
    String base64PipShow = new String(Base64.getEncoder().encode(pipShowContent.getBytes()));
    String base64PipFreeze = new String(Base64.getEncoder().encode(pipFreezeContent.getBytes()));
    System.setProperty("EXHORT_PIP_SHOW",base64PipShow);
    System.setProperty("EXHORT_PIP_FREEZE",base64PipFreeze);
    // when providing component content for our pom
        var content = this.pythonPipProvider.provideComponent(targetRequirementsTxt);
    // verify expected SBOM is returned
    assertThat(content.type).isEqualTo(Api.CYCLONEDX_MEDIA_TYPE);
    assertThat(dropIgnored(new String(content.buffer)))
      .isEqualTo(dropIgnored(expectedSbom));
    System.clearProperty("EXHORT_PIP_SHOW");
    System.clearProperty("EXHORT_PIP_FREEZE");

  }


  @Test
  void Test_The_ProvideComponent_Path_Should_Throw_Exception() {
    assertThatIllegalArgumentException().isThrownBy(() -> {
      this.pythonPipProvider.provideComponent(Path.of("."));
    }).withMessage("provideComponent with file system path for Python pip package manager is not supported");


  }

  private String dropIgnored(String s) {
    return s.replaceAll("\\s+","").replaceAll("\"timestamp\":\"[a-zA-Z0-9\\-\\:]+\"", "");
  }
}
