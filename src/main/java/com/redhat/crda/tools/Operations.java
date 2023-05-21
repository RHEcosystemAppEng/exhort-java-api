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
package com.redhat.crda.tools;

import java.io.IOException;

public final class Operations {
  private Operations(){}

  public static void runProcess(final String... cmdList) {
    var processBuilder = new ProcessBuilder();
    processBuilder.command(cmdList);

    Process process = null;
    try {
      process = processBuilder.start();
    } catch (final IOException e) {
      throw new RuntimeException(
        String.format(
          "failed to build process for '%s' got %s",
          String.join(" ", cmdList),
          e.getMessage()
        )
      );
    }

    int exitCode = 0;
    try {
      exitCode = process.waitFor();
    } catch (final InterruptedException e) {
      throw new RuntimeException(
        String.format(
          "built process for '%s' interrupted, got %s",
          String.join(" ", cmdList),
          e.getMessage()
        )
      );
    }

    if (exitCode != 0) {
      throw new RuntimeException(
        String.format(
          "failed to execute '%s', exit-code %d",
          String.join(" ", cmdList),
          exitCode
        )
      );
    }
  }
}
