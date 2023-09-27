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
import com.redhat.exhort.utils.PythonControllerTestEnv;
import org.junit.jupiter.api.extension.*;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;


public class HelperExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

  private System.Logger log = System.getLogger(this.getClass().getName());

//  public PythonEnvironmentExtension(List<String> requirementsFiles) {
//    this.requirementsFiles = requirementsFiles;
//  }

  private List<String> requirementsFiles;
  @Override
  public void afterAll(ExtensionContext extensionContext) throws Exception {
    log.log(System.Logger.Level.INFO,"Finished all tests!!");

  }

  @Override
  public void afterEach(ExtensionContext extensionContext) throws Exception {
    log.log(System.Logger.Level.INFO,String.format("Finished Test Method: %s, at test class: %s", extensionContext.getRequiredTestMethod(),extensionContext.getRequiredTestClass().getName()));
  }

  @Override
  public void beforeAll(ExtensionContext extensionContext) throws Exception {

    log.log(System.Logger.Level.INFO,"Before all tests");
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) throws Exception {
    log.log(System.Logger.Level.INFO,String.format("Before Invoking Test Method: %s, at test class: %s", extensionContext.getRequiredTestMethod(),extensionContext.getRequiredTestClass().getName()));
  }


}
