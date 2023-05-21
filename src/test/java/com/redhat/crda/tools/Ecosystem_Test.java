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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.redhat.crda.providers.JavaMavenProvider;
import org.junit.jupiter.api.Test;
import java.nio.file.Paths;

class Ecosystem_Test {

  @Test
  void get_a_manifest_for_an_unknown_package_file_should_throw_an_exception() {
    var manifestPath = Paths.get("/not/a/supported/mani.fest");
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> Ecosystem.getManifest(manifestPath));
  }

  @Test
  void get_a_manifest_for_a_pom_xml_file_should_return_java_maven_manifest() {
    var manifestPath = Paths.get("/supported/manifest/pom.xml");
    var manifest = Ecosystem.getManifest(manifestPath);

    assertThat(manifest.manifestPath()).isEqualTo(manifestPath);
    assertThat(manifest.packageManager()).isEqualTo(Ecosystem.PackageManager.MAVEN);
    assertThat(manifest.provider()).isInstanceOf(JavaMavenProvider.class);
  }

}
