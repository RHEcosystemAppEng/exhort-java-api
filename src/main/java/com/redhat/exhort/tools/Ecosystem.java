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
package com.redhat.exhort.tools;

import com.redhat.exhort.Provider;
import com.redhat.exhort.providers.GoModulesProvider;
import com.redhat.exhort.providers.GradleProvider;
import com.redhat.exhort.providers.JavaMavenProvider;
import com.redhat.exhort.providers.JavaScriptNpmProvider;
import com.redhat.exhort.providers.PythonPipProvider;
import java.nio.file.Files;
import java.nio.file.Path;

/** Utility class used for instantiating providers. * */
public final class Ecosystem {

  public enum Type {
    MAVEN("maven"),
    NPM("npm"),
    GOLANG("golang"),
    PYTHON("pypi"),
    GRADLE("gradle");

    String type;

    public String getType() {
      return type;
    }

    Type(String type) {
      this.type = type;
    }
  }

  private Ecosystem() {
    // constructor not required for a utility class
  }

  /**
   * Utility function for instantiating {@link Provider} implementations.
   *
   * @param manifestPath the manifest Path
   * @return a Manifest record
   */
  public static Provider getProvider(final Path manifestPath) {
    return Ecosystem.getProvider(manifestPath.getFileName().toString(), manifestPath);
  }

  /**
   * Utility function for instantiating {@link Provider} implementations.
   *
   * @param manifestType the type (filename + type) of the manifest
   * @return a Manifest record
   */
  public static Provider getProvider(final String manifestType, final Path manifestPath) {
    switch (manifestType) {
      case "pom.xml":
        return new JavaMavenProvider();
      case "package.json":
        Path lockFile = manifestPath.getParent().resolve("package-lock.json");
        if (Files.exists(lockFile)) {
          return new JavaScriptNpmProvider();
        } else {
          throw new IllegalStateException(
              String.format("NPM Lock file could not be found for %s", manifestType));
        }
      case "go.mod":
        return new GoModulesProvider();
      case "requirements.txt":
        return new PythonPipProvider();
      case "build.gradle":
      case "build.gradle.kts":
        return new GradleProvider();

      default:
        throw new IllegalStateException(String.format("Unknown manifest file %s", manifestType));
    }
  }
}
