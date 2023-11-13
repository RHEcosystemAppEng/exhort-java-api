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
package com.redhat.exhort.utils;

import com.redhat.exhort.tools.Operations;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PythonControllerVirtualEnvTest {

  private static PythonControllerVirtualEnv pythonControllerVirtualEnv;
  private static PythonControllerVirtualEnv spiedPythonControllerVirtualEnv;
  @BeforeAll
  static void setUp() {

    pythonControllerVirtualEnv = new PythonControllerVirtualEnv("python3");
    spiedPythonControllerVirtualEnv = Mockito.spy(pythonControllerVirtualEnv);

  }


  @Test
  void test_Virtual_Environment_Flow() throws IOException {
//    Mockito
    String requirementsTxt = "Jinja2==3.0.3";
    Path requirementsFilePath = Path.of(System.getProperty("user.dir").toString(), "requirements.txt");
    Files.write(requirementsFilePath, requirementsTxt.getBytes());
//    MockedStatic<Operations> operationsMockedStatic = mockStatic(Operations.class);
//    when(spiedPythonControllerVirtualEnv.)
    List<Map<String, Object>> dependencies = spiedPythonControllerVirtualEnv.getDependencies(requirementsFilePath.toString(), true);
    verify(spiedPythonControllerVirtualEnv).prepareEnvironment(anyString());
    verify(spiedPythonControllerVirtualEnv).installPackages(anyString());
    verify(spiedPythonControllerVirtualEnv).cleanEnvironment(anyBoolean());
    verify(spiedPythonControllerVirtualEnv).cleanEnvironment(anyBoolean());
    verify(spiedPythonControllerVirtualEnv).automaticallyInstallPackageOnEnvironment();
    verify(spiedPythonControllerVirtualEnv,never()).isRealEnv();
    verify(spiedPythonControllerVirtualEnv,times(2)).isVirtualEnv();





  }

  @Test
  void isRealEnv() {

    assertFalse(this.spiedPythonControllerVirtualEnv.isRealEnv());
  }

  @Test
  void isVirtualEnv() {
    assertTrue(this.spiedPythonControllerVirtualEnv.isVirtualEnv());
  }
}
