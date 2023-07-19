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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import com.redhat.exhort.providers.JavaMavenProvider;

class Ecosystem_Test {

  @Test
  void get_a_provider_for_an_unknown_package_file_should_throw_an_exception() {
    var manifestPath = Paths.get("/not/a/supported/mani.fest");
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> Ecosystem.getProvider(manifestPath));
  }

  @Test
  void get_a_provider_for_a_pom_xml_file_should_return_java_maven_manifest() {
    var manifestPath = Paths.get("/supported/manifest/pom.xml");
    assertThat(Ecosystem.getProvider(manifestPath)).isInstanceOf(JavaMavenProvider.class);
  }

}
