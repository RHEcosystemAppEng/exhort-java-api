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
package com.redhat.crda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Application runner test cases. */
@ExtendWith(MockitoExtension.class)
class App_Runner_Test {
  @Mock private AppInterface mockApp;
  @InjectMocks private AppRunner sut;

  @Test
  void checking_the_runner_if_the_app_is_running_should_invoke_the_underlying_app() {
    given(mockApp.running()).willReturn(true);
    assertThat(sut.isAppRunning()).isTrue();
    then(mockApp).should().running();
  }
}
