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
import java.util.Objects;

/** Utility class used for executing process on the operating system. **/
public final class Operations {
  private Operations(){
    // constructor not required for a utility class
  }

  /**
   * Function for looking up custom executable path based on the default one provides as an
   * argument. I.e. if defaultExecutable=mvn, this function will look for a custom mvn path
   * set as an environment variable or a java property with the name CRDA_MVN_PATH. If not found,
   * the original mvn passed as defaultExecutable will be returned.
   * Note, environment variables takes precedence on java properties.
   *
   * @param defaultExecutable default executable (uppercase spaces and dashes will be replaced with underscores).
   * @return the custom path from the relevant environment variable or the original argument.
   */
  public static String getCustomPathOrElse(String defaultExecutable) {
    var target = defaultExecutable.toUpperCase()
      .replaceAll(" ", "_")
      .replaceAll("-", "_");
    var executableKey = String.format("CRDA_%s_PATH", target);
    return Objects.requireNonNullElseGet(
      System.getenv(executableKey),
      () -> Objects.requireNonNullElse(System.getProperty(executableKey) ,defaultExecutable));
  }

  /**
   * Function for building a command from the command parts list and execute it as a process on
   * the operating system. Will throw a RuntimeException if the command build or execution failed.
   *
   * @param cmdList list of command parts
   */
  public static void runProcess(final String... cmdList) {
    var processBuilder = new ProcessBuilder();
    processBuilder.command(cmdList);

    // create a process builder or throw a runtime exception
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

    // execute the command or throw runtime exception if failed
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

    // verify the command was executed successfully or throw a runtime exception
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
