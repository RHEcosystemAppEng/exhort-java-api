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

import com.redhat.crda.Provider;
import com.redhat.crda.providers.JavaMavenProvider;

import java.nio.file.Path;

/** Utility class used for instantiating providers. **/
public final class Ecosystem {
  /** Enum used for relaying supported package managers. **/
  public enum PackageManager {
    // MAVEN is used to identify Java's Maven
    MAVEN;

    @Override
    public String toString() {
      return this.name().toLowerCase();
    }
  }

  /**
   * Manifest is used for aggregating a manifest {@link Path}, a {@link PackageManager},
   * and a {@link Provider}.
   **/
  public record Manifest(Path manifestPath, PackageManager packageManager, Provider provider){}
  private Ecosystem(){
    // constructor not required for a utility class
  }

  /**
   * Utility function for instantiating {@link Provider} implementations encapsulated in
   * a {@link Manifest} record based on file names and types.
   *
   * @param manifestPath the manifest Path
   * @return a Manifest record
   */
  public static Manifest getManifest(final Path manifestPath) {
    var filename = manifestPath.getFileName().toString();
    switch (filename) {
      case "pom.xml" -> {
        return new Manifest(manifestPath, PackageManager.MAVEN, new JavaMavenProvider());
      }
      default -> throw new IllegalStateException(
        String.format("Unknown manifest file %s", filename)
      );
    }
  }
}
