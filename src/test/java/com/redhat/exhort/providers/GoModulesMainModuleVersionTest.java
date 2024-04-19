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

import static org.junit.jupiter.api.Assertions.*;

import com.redhat.exhort.tools.Operations;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

@Tag("gitTest")
class GoModulesMainModuleVersionTest {

  private Path noGitRepo;
  private Path testGitRepo;
  private GoModulesProvider goModulesProvider;

  @BeforeEach
  void setUp() {
    try {
      this.goModulesProvider = new GoModulesProvider();
      this.testGitRepo = Files.createTempDirectory("exhort_tmp");
      Operations.runProcessGetOutput(this.testGitRepo, "git", "init");
      Operations.runProcessGetOutput(
          this.testGitRepo, "git", "config", "user.email", "tester@exhort-java-api.com");
      Operations.runProcessGetOutput(
          this.testGitRepo, "git", "config", "user.name", "exhort-java-api-tester");
      this.noGitRepo = Files.createTempDirectory("exhort_tmp");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @AfterEach
  void tearDown() {
    try {
      FileUtils.deleteDirectory(this.testGitRepo.toFile());
      FileUtils.deleteDirectory(this.noGitRepo.toFile());

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void determine_Main_Module_Version_NoRepo() {
    goModulesProvider.determineMainModuleVersion(noGitRepo);
    assertEquals(goModulesProvider.defaultMainVersion, goModulesProvider.getMainModuleVersion());
  }

  @Test
  void determine_Main_Module_Version_GitRepo() {
    goModulesProvider.determineMainModuleVersion(testGitRepo);
    assertEquals(goModulesProvider.defaultMainVersion, goModulesProvider.getMainModuleVersion());
  }

  @Test
  void determine_Main_Module_Version_GitRepo_commit_is_tag() {

    Operations.runProcessGetOutput(
        this.testGitRepo, "git", "commit", "-m \"sample\"", "--allow-empty");
    Operations.runProcessGetOutput(this.testGitRepo, "git", "tag", "v1.0.0");

    goModulesProvider.determineMainModuleVersion(testGitRepo);
    assertEquals("v1.0.0", goModulesProvider.getMainModuleVersion());
  }

  @Test
  void determine_Main_Module_Version_GitRepo_commit_is_annotated_tag() {

    Operations.runProcessGetOutput(
        this.testGitRepo, "git", "commit", "-m \"sample\"", "--allow-empty");
    Operations.runProcessGetOutput(
        this.testGitRepo, "git", "tag", "-a", "-m", "annotatedTag", "v1.0.0a");

    goModulesProvider.determineMainModuleVersion(testGitRepo);
    assertEquals("v1.0.0a", goModulesProvider.getMainModuleVersion());
  }

  @Test
  void determine_Main_Module_Version_GitRepo_commit_is_after_tag() {

    Operations.runProcessGetOutput(
        this.testGitRepo, "git", "commit", "-m \"sample\"", "--allow-empty");
    Operations.runProcessGetOutput(this.testGitRepo, "git", "tag", "v1.0.0");
    Operations.runProcessGetOutput(
        this.testGitRepo, "git", "commit", "-m \"sample2\"", "--allow-empty");

    goModulesProvider.determineMainModuleVersion(testGitRepo);
    assertTrue(
        Pattern.matches(
            "v1.0.1-0.[0-9]{14}-[a-f0-9]{12}", goModulesProvider.getMainModuleVersion()));
  }
}
