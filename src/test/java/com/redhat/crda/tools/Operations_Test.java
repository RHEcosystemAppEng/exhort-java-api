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

import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

class Operations_Test {
  @Test
  void when_running_process_for_existing_command_should_not_throw_exception() {
    assertThatNoException().isThrownBy(() -> Operations.runProcess("ls", "."));
  }

  @Test
  void when_running_process_for_non_existing_command_should_throw_runtime_exception() {
    assertThatRuntimeException().isThrownBy(() -> Operations.runProcess("unknown", "--command"));
  }

  @Test
  void when_executable_custom_path_env_var_not_found_should_return_the_default_executable() {
    assertThat(Operations.getCustomPathOrElse("madeupcmd")).isEqualTo("madeupcmd");
  }

  @Test
  @SetEnvironmentVariable(key="CRDA_MADE_UP_CMD_PATH", value="/path/to/custom/made_up_cmd")
  void when_executable_custom_path_was_specified_with_env_var_return_the_custom_path() {
    assertThat(Operations.getCustomPathOrElse("made-up cmd")).isEqualTo("/path/to/custom/made_up_cmd");
  }
}
