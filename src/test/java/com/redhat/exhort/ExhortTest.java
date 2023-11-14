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
package com.redhat.exhort;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ExhortTest {

  protected String getStringFromFile(String... list) {
    byte[] bytes = new byte[0];
    try {
      InputStream resourceAsStream = ExhortTest.class.getModule().getResourceAsStream(String.join("/", list));
      bytes = resourceAsStream.readAllBytes();
      resourceAsStream.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return new String(bytes);
  }

  protected String getFileFromResource(String fileName, String... pathList) {
    Path tmpFile;
    try {
      var tmpDir = Files.createTempDirectory("exhort_test_");
      tmpFile = Files.createFile(tmpDir.resolve(fileName));
      try (var is = getClass().getModule().getResourceAsStream(String.join("/", pathList))) {
        Files.write(tmpFile, is.readAllBytes());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return tmpFile.toString();
  }
protected String getFileFromString(String fileName, String content) {
    Path tmpFile;
    try {
      var tmpDir = Files.createTempDirectory("exhort_test_");
      tmpFile = Files.createFile(tmpDir.resolve(fileName));
        Files.write(tmpFile, content.getBytes());

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return tmpFile.toString();
  }

}
