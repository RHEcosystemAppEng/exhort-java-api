package com.redhat.crda.tools;

import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import org.junit.jupiter.api.Test;

class Operations_Test {
  @Test
  void when_running_process_for_existing_command_should_not_throw_exception() {
    assertThatNoException().isThrownBy(() -> Operations.runProcess("ls", "."));
  }

  @Test
  void when_running_process_for_non_existing_command_should_throw_runtime_exception() {
    assertThatRuntimeException().isThrownBy(() -> Operations.runProcess("unknown", "--command"));
  }
}
