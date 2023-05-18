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
package com.redhat.crda.simple.it;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.crda.AppInterface;
import com.redhat.crda.AppRunner;

/** Simple Integration Test. */
class Simple_Integration_Test {
  private AppImplementation appImpl;
  private AppRunner runner;

  @BeforeEach
  void initialize() {
    appImpl = new AppImplementation();
    runner = new AppRunner(appImpl);
  }

  @Test
  void an_enacpsulated_running_application_should_be_registered_as_running() {
    appImpl.start();
    assertThat(runner.isAppRunning()).isTrue();
  }

  @Test
  void an_enacpsulated_stopped_application_should_not_be_registered_as_running() {
    appImpl.stop();
    assertThat(runner.isAppRunning()).isFalse();
  }
}
