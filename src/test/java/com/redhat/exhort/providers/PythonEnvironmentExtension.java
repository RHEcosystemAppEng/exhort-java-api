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

import com.redhat.exhort.tools.Operations;
import com.redhat.exhort.utils.PythonControllerBase;
import com.redhat.exhort.utils.PythonControllerVirtualEnv;
import com.redhat.exhort.utils.PythonControllerTestEnv;
import org.junit.jupiter.api.extension.*;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;


public class PythonEnvironmentExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver, BeforeTestExecutionCallback {

  private PythonControllerBase pythonController = new PythonControllerTestEnv("python3");

//  public PythonEnvironmentExtension(List<String> requirementsFiles) {
//    this.requirementsFiles = requirementsFiles;
//  }

  private List<String> requirementsFiles;
  @Override
  public void afterAll(ExtensionContext extensionContext) throws Exception {
    pythonController.cleanEnvironment(true);
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) throws Exception {
  }

  @Override
  public void beforeAll(ExtensionContext extensionContext) throws Exception {
    String python3 = Operations.getCustomPathOrElse("python3");
    this.pythonController = new PythonControllerTestEnv(python3);
    this.pythonController.prepareEnvironment(python3);
    var tmpPythonModuleDir = Files.createTempDirectory("exhort_test_");
    var tmpPythonFile = Files.createFile(tmpPythonModuleDir.resolve("requirements.txt"));
    Python_Provider_Test.testFolders().forEach( test -> {
      try (var is = getClass().getModule().getResourceAsStream(String.join("/","tst_manifests", "pip", test, "requirements.txt"))) {
        Files.write(tmpPythonFile, is.readAllBytes());
        pythonController.installPackage(tmpPythonFile.toAbsolutePath().toString());

      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    Files.deleteIfExists(tmpPythonFile);
    Files.deleteIfExists(tmpPythonModuleDir);


  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) throws Exception {

  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    return parameterContext.getParameter().getType()
      .equals(PythonControllerBase.class) || parameterContext.getParameter().getType()
      .equals(PythonControllerTestEnv.class);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    return this.pythonController;
  }

  @Override
  public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {
//    Method requiredTestMethod = extensionContext.getRequiredTestInstances();
  }
}
