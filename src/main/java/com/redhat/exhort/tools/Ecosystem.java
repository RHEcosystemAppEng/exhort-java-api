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

import java.nio.file.Path;

import com.redhat.exhort.Provider;
import com.redhat.exhort.providers.JavaMavenProvider;

/** Utility class used for instantiating providers. **/
public final class Ecosystem {
  private Ecosystem(){
    // constructor not required for a utility class
  }

  /**
   * Utility function for instantiating {@link Provider} implementations.
   *
   * @param manifestPath the manifest Path
   * @return a Manifest record
   */
  public static Provider getProvider(final Path manifestPath) {
    return Ecosystem.getProvider(manifestPath.getFileName().toString());
  }

  /**
   * Utility function for instantiating {@link Provider} implementations.
   *
   * @param manifestType the type (filename + type) of the manifest
   * @return a Manifest record
   */
  public static Provider getProvider(final String manifestType) {
    switch (manifestType) {
      case "pom.xml":
        return new JavaMavenProvider("maven");
      default:
        throw new IllegalStateException(String.format("Unknown manifest file %s", manifestType)
        );
    }
  }
}
