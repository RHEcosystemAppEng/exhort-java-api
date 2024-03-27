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
package com.redhat.exhort.image;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.packageurl.MalformedPackageURLException;
import com.redhat.exhort.ExhortTest;
import com.redhat.exhort.tools.Operations;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.redhat.exhort.image.ImageUtils.EXHORT_IMAGE_ARCH;
import static com.redhat.exhort.image.ImageUtils.EXHORT_IMAGE_OS;
import static com.redhat.exhort.image.ImageUtils.EXHORT_IMAGE_PLATFORM;
import static com.redhat.exhort.image.ImageUtils.EXHORT_IMAGE_SERVICE_ENDPOINT;
import static com.redhat.exhort.image.ImageUtils.EXHORT_IMAGE_VARIANT;
import static com.redhat.exhort.image.ImageUtils.EXHORT_SKOPEO_CONFIG_PATH;
import static com.redhat.exhort.image.ImageUtils.EXHORT_SYFT_CONFIG_PATH;
import static com.redhat.exhort.image.ImageUtils.EXHORT_SYFT_IMAGE_SOURCE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

class ImageUtilsTest extends ExhortTest {

  static final String mockImageName = "test.io/test/test-app:test-version@sha256:1fafb0905264413501df60d90a92ca32df8a2011cbfb4876ddff5ceb20c8f165";
  static final String mockImagePlatform = "linux/amd64";
  static final ImageRef mockImageRef = new ImageRef(mockImageName, mockImagePlatform);
  static final String mockSyftPath = "test-path/syft";
  static final String mockSyftConfig = "test-path/syft-config";
  static final String mockSyftSource = "registry";
  static final String mockSkopeoPath = "test-path/skopeo";
  static final String mockSkopeoConfig = "test-path/skopeo-config";
  static final String mockSkopeoDaemon = "test-path/daemon-host";
  static final String mockDockerPath = "test-path/docker";
  static final String mockPodmanPath = "test-path/podman";
  static final String mockPath = "test-path";
  static final String mockOs = "linux";
  static final String mockArch = "arm";
  static final String mockVariant = "v7";

  static Stream<Arguments> dockerArchSources() {
    return Stream.of(
      Arguments.of(
        Named.of("amd64", "amd64"), "amd64"
      ),
      Arguments.of(
        Named.of("x86_64", "x86_64"), "amd64"
      ),
      Arguments.of(
        Named.of("armv5tl", "armv5tl"), "arm"
      ),
      Arguments.of(
        Named.of("armv5tel", "armv5tel"), "arm"
      ),
      Arguments.of(
        Named.of("armv5tejl", "armv5tejl"), "arm"
      ),
      Arguments.of(
        Named.of("armv6l", "armv6l"), "arm"
      ),
      Arguments.of(
        Named.of("armv7l", "armv7l"), "arm"
      ),
      Arguments.of(
        Named.of("armv7ml", "armv7ml"), "arm"
      ),
      Arguments.of(
        Named.of("arm64", "arm64"), "arm64"
      ),
      Arguments.of(
        Named.of("aarch64", "aarch64"), "arm64"
      ),
      Arguments.of(
        Named.of("i386", "i386"), "386"
      ),
      Arguments.of(
        Named.of("i486", "i486"), "386"
      ),
      Arguments.of(
        Named.of("i586", "i586"), "386"
      ),
      Arguments.of(
        Named.of("i686", "i686"), "386"
      ),
      Arguments.of(
        Named.of("mips64le", "mips64le"), "mips64le"
      ),
      Arguments.of(
        Named.of("ppc64le", "ppc64le"), "ppc64le"
      ),
      Arguments.of(
        Named.of("riscv64", "riscv64"), "riscv64"
      ),
      Arguments.of(
        Named.of("s390x", "s390x"), "s390x"
      ),
      Arguments.of(
        Named.of("empty", ""), ""
      )
    );
  }

  static Stream<Arguments> dockerVariantSources() {
    return Stream.of(
      Arguments.of(
        Named.of("armv5tl", "armv5tl"), "v5"
      ),
      Arguments.of(
        Named.of("armv5tel", "armv5tel"), "v5"
      ),
      Arguments.of(
        Named.of("armv5tejl", "armv5tejl"), "v5"
      ),
      Arguments.of(
        Named.of("armv6l", "armv6l"), "v6"
      ),
      Arguments.of(
        Named.of("armv7l", "armv7l"), "v7"
      ),
      Arguments.of(
        Named.of("armv7ml", "armv7ml"), "v7"
      ),
      Arguments.of(
        Named.of("arm64", "arm64"), "v8"
      ),
      Arguments.of(
        Named.of("aarch64", "aarch64"), "v8"
      ),
      Arguments.of(
        Named.of("empty", ""), ""
      )
    );
  }

  @Test
  @ClearEnvironmentVariable(key = "PATH")
  @ClearEnvironmentVariable(key = "EXHORT_SYFT_PATH")
  @ClearEnvironmentVariable(key = EXHORT_SYFT_CONFIG_PATH)
  @ClearEnvironmentVariable(key = "EXHORT_DOCKER_PATH")
  @ClearEnvironmentVariable(key = "EXHORT_PODMAN_PATH")
  @ClearEnvironmentVariable(key = EXHORT_SYFT_IMAGE_SOURCE)
  void test_generate_image_sbom() throws IOException, MalformedPackageURLException {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class);
         var is = getResourceAsStreamDecision(this.getClass(), new String[]{"msc", "image", "image_sbom.json"})) {
      var json = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
      var output = new Operations.ProcessExecOutput(json, "", 0);

      mock.when(() -> Operations.getCustomPathOrElse(eq("syft")))
        .thenReturn("syft");

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{"syft", mockImageRef.getImage().getFullName(),
            "-s", "all-layers", "-o", "cyclonedx-json", "-q"}),
          isNull()))
        .thenReturn(output);

      var sbom = ImageUtils.generateImageSBOM(mockImageRef);

      var mapper = new ObjectMapper();
      var node = mapper.readTree(json);
      ((ObjectNode) node.get("metadata").get("component")).set("purl", new TextNode(mockImageRef.getPackageURL().canonicalize()));

      assertEquals(node, sbom);
    }
  }

  @Test
  @ClearEnvironmentVariable(key = "EXHORT_SKOPEO_PATH")
  @ClearEnvironmentVariable(key = EXHORT_SKOPEO_CONFIG_PATH)
  @ClearEnvironmentVariable(key = EXHORT_IMAGE_SERVICE_ENDPOINT)
  void test_get_image_digests_single() throws IOException {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class);
         var isRaw = getResourceAsStreamDecision(this.getClass(), new String[]{"msc", "image", "skopeo_inspect_single_raw.json"});
         var is = getResourceAsStreamDecision(this.getClass(), new String[]{"msc", "image", "skopeo_inspect_single.json"})) {
      var jsonRaw = new BufferedReader(new InputStreamReader(isRaw, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
      var outputRaw = new Operations.ProcessExecOutput(jsonRaw, "", 0);

      mock.when(() -> Operations.getCustomPathOrElse(eq("skopeo")))
        .thenReturn("skopeo");

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{"skopeo", "inspect",
            "--raw", String.format("docker://%s", mockImageRef.getImage().getFullName())}),
          isNull()))
        .thenReturn(outputRaw);

      var json = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
      var output = new Operations.ProcessExecOutput(json, "", 0);

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{"skopeo", "inspect",
            "", String.format("docker://%s", mockImageRef.getImage().getFullName())}),
          isNull()))
        .thenReturn(output);

      var digests = ImageUtils.getImageDigests(mockImageRef);

      var expectedDigests = Collections.singletonMap(Platform.EMPTY_PLATFORM, "sha256:9aa20fd4e4842854ec1c081d2dae77c686601a8640018d68782f36c60eb1a19e");

      assertEquals(expectedDigests, digests);
    }
  }

  @Test
  @ClearEnvironmentVariable(key = "EXHORT_SKOPEO_PATH")
  @ClearEnvironmentVariable(key = EXHORT_SKOPEO_CONFIG_PATH)
  @ClearEnvironmentVariable(key = EXHORT_IMAGE_SERVICE_ENDPOINT)
  void test_get_image_digests_multiple() throws IOException {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class);
         var is = getResourceAsStreamDecision(this.getClass(), new String[]{"msc", "image", "skopeo_inspect_multi_raw.json"})) {
      var json = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
      var output = new Operations.ProcessExecOutput(json, "", 0);

      mock.when(() -> Operations.getCustomPathOrElse(eq("skopeo")))
        .thenReturn("skopeo");

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{"skopeo", "inspect",
            "--raw", String.format("docker://%s", mockImageRef.getImage().getFullName())}),
          isNull()))
        .thenReturn(output);

      var digests = ImageUtils.getImageDigests(mockImageRef);

      var expectedDigests = new HashMap<>();
      expectedDigests.put(new Platform("linux", "amd64", null), "sha256:06d06f15f7b641a78f2512c8817cbecaa1bf549488e273f5ac27ff1654ed33f0");
      expectedDigests.put(new Platform("linux", "arm64", null), "sha256:199d5daca3dba0a7deaf0086331917dee256089e94272bef5613517d0007f6f5");
      expectedDigests.put(new Platform("linux", "ppc64le", null), "sha256:1bba662cff053201db85aa55caf3273216a6b0e1766409ee133cf78df9b59314");
      expectedDigests.put(new Platform("linux", "s390x", null), "sha256:b39f9f6998e1693e29b7bd002bc32255fd4f69610e950523b647e61d2bb1dd66");

      assertEquals(expectedDigests, digests);
    }
  }

  @Test
  @SetEnvironmentVariable(key = EXHORT_IMAGE_PLATFORM, value = mockImagePlatform)
  @SetEnvironmentVariable(key = EXHORT_SYFT_IMAGE_SOURCE, value = mockSyftSource)
  @SetEnvironmentVariable(key = EXHORT_IMAGE_OS, value = mockOs)
  @SetEnvironmentVariable(key = EXHORT_IMAGE_ARCH, value = mockArch)
  @SetEnvironmentVariable(key = EXHORT_IMAGE_VARIANT, value = mockVariant)
  void test_get_image_platform() {
    var platform = ImageUtils.getImagePlatform();
    assertEquals(new Platform(mockImagePlatform), platform);
  }

  @Test
  @ClearEnvironmentVariable(key = EXHORT_IMAGE_PLATFORM)
  @SetEnvironmentVariable(key = EXHORT_SYFT_IMAGE_SOURCE, value = mockSyftSource)
  @SetEnvironmentVariable(key = EXHORT_IMAGE_OS, value = mockOs)
  @SetEnvironmentVariable(key = EXHORT_IMAGE_ARCH, value = mockArch)
  @SetEnvironmentVariable(key = EXHORT_IMAGE_VARIANT, value = mockVariant)
  void test_get_image_platform_no_default() {
    var platform = ImageUtils.getImagePlatform();
    assertEquals(new Platform(mockOs, mockArch, mockVariant), platform);
  }

  @Test
  @ClearEnvironmentVariable(key = EXHORT_IMAGE_PLATFORM)
  @SetEnvironmentVariable(key = EXHORT_SYFT_IMAGE_SOURCE, value = "podman")
  @SetEnvironmentVariable(key = EXHORT_IMAGE_OS, value = mockOs)
  @SetEnvironmentVariable(key = EXHORT_IMAGE_ARCH, value = mockArch)
  @ClearEnvironmentVariable(key = EXHORT_IMAGE_VARIANT)
  void test_get_image_platform_no_default_no_variant() {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class)) {
      mock.when(() -> Operations.getCustomPathOrElse(eq("podman")))
        .thenReturn("podman");

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{"podman", "info"}),
          isNull()))
        .thenReturn(new Operations.ProcessExecOutput("", "", 0));

      var platform = ImageUtils.getImagePlatform();
      assertNull(platform);
    }
  }

  @Test
  @ClearEnvironmentVariable(key = EXHORT_IMAGE_PLATFORM)
  @SetEnvironmentVariable(key = EXHORT_SYFT_IMAGE_SOURCE, value = "podman")
  @ClearEnvironmentVariable(key = EXHORT_IMAGE_OS)
  @ClearEnvironmentVariable(key = EXHORT_IMAGE_ARCH)
  @ClearEnvironmentVariable(key = EXHORT_IMAGE_VARIANT)
  @ClearEnvironmentVariable(key = "PATH")
  void test_get_image_platform_no_defaults() {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class)) {
      mock.when(() -> Operations.getCustomPathOrElse(eq("podman")))
        .thenReturn("podman");

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{"podman", "info"}),
          isNull()))
        .thenReturn(new Operations.ProcessExecOutput("os: linux\narch: arm64\nvariant=v8", "", 0));

      var platform = ImageUtils.getImagePlatform();
      assertEquals(new Platform("linux", "arm64", "v8"), platform);
    }
  }

  @Test
  @ClearEnvironmentVariable(key = "PATH")
  @SetEnvironmentVariable(key = "EXHORT_SYFT_PATH", value = mockSyftPath)
  @SetEnvironmentVariable(key = EXHORT_SYFT_CONFIG_PATH, value = mockSyftConfig)
  @SetEnvironmentVariable(key = "EXHORT_DOCKER_PATH", value = mockDockerPath)
  @SetEnvironmentVariable(key = "EXHORT_PODMAN_PATH", value = mockPodmanPath)
  @SetEnvironmentVariable(key = EXHORT_SYFT_IMAGE_SOURCE, value = mockSyftSource)
  void test_exec_syft() {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class)) {
      var output = new Operations.ProcessExecOutput("test-output", "test-error", 0);

      mock.when(() -> Operations.getCustomPathOrElse(eq("syft")))
        .thenReturn(mockSyftPath);

      mock.when(() -> Operations.getCustomPathOrElse(eq("docker")))
        .thenReturn(mockDockerPath);

      mock.when(() -> Operations.getCustomPathOrElse(eq("podman")))
        .thenReturn(mockPodmanPath);

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{mockSyftPath,  mockImageRef.getImage().getFullName(),
            "--from", mockSyftSource, "-c", mockSyftConfig, "-s", "all-layers",
            "-o", "cyclonedx-json", "-q"}),
          eq(new String[]{"PATH=" + "test-path/" + File.pathSeparator + "test-path/"})))
        .thenReturn(output);

      assertThat(ImageUtils.execSyft(mockImageRef)).isEqualTo(output);
    }
  }

  @Test
  @ClearEnvironmentVariable(key = "PATH")
  @ClearEnvironmentVariable(key = "EXHORT_SYFT_PATH")
  @ClearEnvironmentVariable(key = EXHORT_SYFT_CONFIG_PATH)
  @ClearEnvironmentVariable(key = "EXHORT_DOCKER_PATH")
  @ClearEnvironmentVariable(key = "EXHORT_PODMAN_PATH")
  @ClearEnvironmentVariable(key = EXHORT_SYFT_IMAGE_SOURCE)
  void test_exec_syft_no_config_no_source() {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class)) {
      var output = new Operations.ProcessExecOutput("test-output", "test-error", 0);

      mock.when(() -> Operations.getCustomPathOrElse(eq("docker")))
        .thenReturn("docker");

      mock.when(() -> Operations.getCustomPathOrElse(eq("podman")))
        .thenReturn("podman");

      mock.when(() -> Operations.getCustomPathOrElse(eq("syft")))
        .thenReturn("syft");

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{"syft", mockImageRef.getImage().getFullName(),
            "-s", "all-layers", "-o", "cyclonedx-json", "-q"}),
          isNull()))
        .thenReturn(output);

      assertThat(ImageUtils.execSyft(mockImageRef)).isEqualTo(output);
    }
  }

  @Test
  @ClearEnvironmentVariable(key = "PATH")
  void test_get_syft_envs() {
    var envs1 = ImageUtils.getSyftEnvs("", "");
    assertTrue(envs1.isEmpty());

    var envs2 = ImageUtils.getSyftEnvs("test-docker-path", "");
    var expected_envs2 = new ArrayList<>();
    expected_envs2.add("PATH=test-docker-path");
    assertEquals(expected_envs2, envs2);

    var envs3 = ImageUtils.getSyftEnvs("", "test-podman-path");
    var expected_envs3 = new ArrayList<>();
    expected_envs3.add("PATH=test-podman-path");
    assertEquals(expected_envs3, envs3);

    var envs4 = ImageUtils.getSyftEnvs("test-docker-path", "test-podman-path");
    var expected_envs4 = new ArrayList<>();
    expected_envs4.add("PATH=test-docker-path" + File.pathSeparator + "test-podman-path");
    assertEquals(expected_envs4, envs4);
  }

  @Test
  @SetEnvironmentVariable(key = "PATH", value = mockPath)
  void test_update_PATH_env() {
    var path = ImageUtils.updatePATHEnv("test-exec-path");
    assertEquals("PATH=test-path" + File.pathSeparator + "test-exec-path", path);
  }

  @Test
  @ClearEnvironmentVariable(key = "PATH")
  void test_update_PATH_env_no_PATH() {
    var path = ImageUtils.updatePATHEnv("test-exec-path");
    assertEquals("PATH=test-exec-path", path);
  }

  @Test
  @SetEnvironmentVariable(key = "EXHORT_DOCKER_PATH", value = mockDockerPath)
  @SetEnvironmentVariable(key = "PATH", value = mockPath)
  void test_host_info_docker() {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class)) {
      var output = new Operations.ProcessExecOutput("info0: test\n info: test-output", "", 0);

      mock.when(() -> Operations.getCustomPathOrElse(eq("docker")))
        .thenReturn(mockDockerPath);

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{mockDockerPath, "info"}),
          isNull()))
        .thenReturn(output);

      assertThat(ImageUtils.hostInfo("docker", "info")).isEqualTo("test-output");
    }
  }

  @Test
  @ClearEnvironmentVariable(key = "EXHORT_DOCKER_PATH")
  void test_host_info_no_docker_path() {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class)) {
      var output = new Operations.ProcessExecOutput("", "test-error", 0);

      mock.when(() -> Operations.getCustomPathOrElse(eq("docker")))
        .thenReturn("docker");

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{"docker", "info"}),
          isNull()))
        .thenReturn(output);

      var exception = assertThrows(RuntimeException.class, () -> {
        ImageUtils.hostInfo("docker", "info");
      });
      assertEquals("test-error", exception.getMessage());
    }
  }

  @Test
  @SetEnvironmentVariable(key = "EXHORT_DOCKER_PATH", value = mockDockerPath)
  @SetEnvironmentVariable(key = "PATH", value = mockPath)
  void test_docker_get_os() {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class)) {
      var output = new Operations.ProcessExecOutput("OSType: test-output", "", 0);

      mock.when(() -> Operations.getCustomPathOrElse(eq("docker")))
        .thenReturn(mockDockerPath);

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{mockDockerPath, "info"}),
          isNull()))
        .thenReturn(output);

      assertThat(ImageUtils.dockerGetOs()).isEqualTo("test-output");
    }
  }

  @ParameterizedTest(name = "{0}")
  @SetEnvironmentVariable(key = "EXHORT_DOCKER_PATH", value = mockDockerPath)
  @SetEnvironmentVariable(key = "PATH", value = mockPath)
  @MethodSource("dockerArchSources")
  void test_docker_get_arch(String sysArch, String arch) {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class)) {
      var output = new Operations.ProcessExecOutput("Architecture:" + sysArch, "", 0);

      mock.when(() -> Operations.getCustomPathOrElse(eq("docker")))
        .thenReturn(mockDockerPath);

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{mockDockerPath, "info"}),
          isNull()))
        .thenReturn(output);

      assertThat(ImageUtils.dockerGetArch()).isEqualTo(arch);
    }
  }

  @ParameterizedTest(name = "{0}")
  @SetEnvironmentVariable(key = "EXHORT_DOCKER_PATH", value = mockDockerPath)
  @SetEnvironmentVariable(key = "PATH", value = mockPath)
  @MethodSource("dockerVariantSources")
  void test_docker_get_variant(String sysArch, String variant) {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class)) {
      var output = new Operations.ProcessExecOutput("Architecture:" + sysArch, "", 0);

      mock.when(() -> Operations.getCustomPathOrElse(eq("docker")))
        .thenReturn(mockDockerPath);

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{mockDockerPath, "info"}),
          isNull()))
        .thenReturn(output);

      assertThat(ImageUtils.dockerGetVariant()).isEqualTo(variant);
    }
  }

  @Test
  @SetEnvironmentVariable(key = "EXHORT_PODMAN_PATH", value = mockPodmanPath)
  @SetEnvironmentVariable(key = "PATH", value = mockPath)
  void test_host_info_podman() {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class)) {
      var output = new Operations.ProcessExecOutput("info: test-output\nabcdesss", "", 0);

      mock.when(() -> Operations.getCustomPathOrElse(eq("podman")))
        .thenReturn(mockPodmanPath);

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{mockPodmanPath, "info"}),
          isNull()))
        .thenReturn(output);

      assertThat(ImageUtils.hostInfo("podman", "info")).isEqualTo("test-output");
    }
  }

  @Test
  @SetEnvironmentVariable(key = "EXHORT_PODMAN_PATH", value = mockPodmanPath)
  @SetEnvironmentVariable(key = "PATH", value = mockPath)
  void test_podman_get_os() {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class)) {
      var output = new Operations.ProcessExecOutput("os: test-output", "", 0);

      mock.when(() -> Operations.getCustomPathOrElse(eq("podman")))
        .thenReturn(mockPodmanPath);

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{mockPodmanPath, "info"}),
          isNull()))
        .thenReturn(output);

      assertThat(ImageUtils.podmanGetOs()).isEqualTo("test-output");
    }
  }

  @Test
  @SetEnvironmentVariable(key = "EXHORT_PODMAN_PATH", value = mockPodmanPath)
  @SetEnvironmentVariable(key = "PATH", value = mockPath)
  void test_podman_get_arch() {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class)) {
      var output = new Operations.ProcessExecOutput("arch: test-output", "", 0);

      mock.when(() -> Operations.getCustomPathOrElse(eq("podman")))
        .thenReturn(mockPodmanPath);

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{mockPodmanPath, "info"}),
          isNull()))
        .thenReturn(output);

      assertThat(ImageUtils.podmanGetArch()).isEqualTo("test-output");
    }
  }

  @Test
  @SetEnvironmentVariable(key = "EXHORT_PODMAN_PATH", value = mockPodmanPath)
  @SetEnvironmentVariable(key = "PATH", value = mockPath)
  void test_podman_get_variant() {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class)) {
      var output = new Operations.ProcessExecOutput("variant: test-output", "", 0);

      mock.when(() -> Operations.getCustomPathOrElse(eq("podman")))
        .thenReturn(mockPodmanPath);

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{mockPodmanPath, "info"}),
          isNull()))
        .thenReturn(output);

      assertThat(ImageUtils.podmanGetVariant()).isEqualTo("test-output");
    }
  }

  @Test
  void test_docker_podman_info() {
    var info = ImageUtils.dockerPodmanInfo(() -> "docker", () -> "podman");
    assertEquals("docker", info);

    info = ImageUtils.dockerPodmanInfo(() -> "", () -> "podman");
    assertEquals("podman", info);
  }

  @Test
  @SetEnvironmentVariable(key = "EXHORT_SKOPEO_PATH", value = mockSkopeoPath)
  @SetEnvironmentVariable(key = EXHORT_SKOPEO_CONFIG_PATH, value = mockSkopeoConfig)
  @SetEnvironmentVariable(key = EXHORT_IMAGE_SERVICE_ENDPOINT, value = mockSkopeoDaemon)
  void test_exec_skopeo_inspect_raw() {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class)) {
      var output = new Operations.ProcessExecOutput("test-output", "test-error", 0);

      mock.when(() -> Operations.getCustomPathOrElse(eq("skopeo")))
        .thenReturn(mockSkopeoPath);

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{mockSkopeoPath, "inspect", "--authfile", mockSkopeoConfig,
            "--daemon-host", mockSkopeoDaemon, "--raw",
            String.format("docker-daemon:%s", mockImageRef.getImage().getFullName())}),
          isNull()))
        .thenReturn(output);

      assertThat(ImageUtils.execSkopeoInspect(mockImageRef, true)).isEqualTo(output);
    }
  }

  @Test
  @SetEnvironmentVariable(key = "EXHORT_SKOPEO_PATH", value = mockSkopeoPath)
  @ClearEnvironmentVariable(key = EXHORT_SKOPEO_CONFIG_PATH)
  @SetEnvironmentVariable(key = EXHORT_IMAGE_SERVICE_ENDPOINT, value = mockSkopeoDaemon)
  void test_exec_skopeo_inspect_raw_no_config() {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class)) {
      var output = new Operations.ProcessExecOutput("test-output", "test-error", 0);

      mock.when(() -> Operations.getCustomPathOrElse(eq("skopeo")))
        .thenReturn(mockSkopeoPath);

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{mockSkopeoPath, "inspect",
            "--daemon-host", mockSkopeoDaemon, "--raw",
            String.format("docker-daemon:%s", mockImageRef.getImage().getFullName())}),
          isNull()))
        .thenReturn(output);

      assertThat(ImageUtils.execSkopeoInspect(mockImageRef, true)).isEqualTo(output);
    }
  }

  @Test
  @ClearEnvironmentVariable(key = "EXHORT_SKOPEO_PATH")
  @SetEnvironmentVariable(key = EXHORT_SKOPEO_CONFIG_PATH, value = mockSkopeoConfig)
  @ClearEnvironmentVariable(key = EXHORT_IMAGE_SERVICE_ENDPOINT)
  void test_exec_skopeo_inspect_raw_no_daemon() {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class)) {
      var output = new Operations.ProcessExecOutput("test-output", "test-error", 0);

      mock.when(() -> Operations.getCustomPathOrElse(eq("skopeo")))
        .thenReturn("skopeo");

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{"skopeo", "inspect", "--authfile", mockSkopeoConfig,
            "--raw", String.format("docker://%s", mockImageRef.getImage().getFullName())}),
          isNull()))
        .thenReturn(output);

      assertThat(ImageUtils.execSkopeoInspect(mockImageRef, true)).isEqualTo(output);
    }
  }

  @Test
  @ClearEnvironmentVariable(key = "EXHORT_SKOPEO_PATH")
  @ClearEnvironmentVariable(key = EXHORT_SKOPEO_CONFIG_PATH)
  @ClearEnvironmentVariable(key = EXHORT_IMAGE_SERVICE_ENDPOINT)
  void test_exec_skopeo_inspect_raw_no_config_no_daemon() {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class)) {
      var output = new Operations.ProcessExecOutput("test-output", "test-error", 0);

      mock.when(() -> Operations.getCustomPathOrElse(eq("skopeo")))
        .thenReturn("skopeo");

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{"skopeo", "inspect",
            "--raw", String.format("docker://%s", mockImageRef.getImage().getFullName())}),
          isNull()))
        .thenReturn(output);

      assertThat(ImageUtils.execSkopeoInspect(mockImageRef, true)).isEqualTo(output);
    }
  }

  @Test
  @SetEnvironmentVariable(key = "EXHORT_SKOPEO_PATH", value = mockSkopeoPath)
  @SetEnvironmentVariable(key = EXHORT_SKOPEO_CONFIG_PATH, value = mockSkopeoConfig)
  @SetEnvironmentVariable(key = EXHORT_IMAGE_SERVICE_ENDPOINT, value = mockSkopeoDaemon)
  void test_exec_skopeo_inspect() {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class)) {
      var output = new Operations.ProcessExecOutput("test-output", "test-error", 0);

      mock.when(() -> Operations.getCustomPathOrElse(eq("skopeo")))
        .thenReturn(mockSkopeoPath);

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{mockSkopeoPath, "inspect", "--authfile", mockSkopeoConfig,
            "--daemon-host", mockSkopeoDaemon, "",
            String.format("docker-daemon:%s", mockImageRef.getImage().getFullName())}),
          isNull()))
        .thenReturn(output);

      assertThat(ImageUtils.execSkopeoInspect(mockImageRef, false)).isEqualTo(output);
    }
  }

  @Test
  @SetEnvironmentVariable(key = "EXHORT_SKOPEO_PATH", value = mockSkopeoPath)
  @ClearEnvironmentVariable(key = EXHORT_SKOPEO_CONFIG_PATH)
  @SetEnvironmentVariable(key = EXHORT_IMAGE_SERVICE_ENDPOINT, value = mockSkopeoDaemon)
  void test_exec_skopeo_inspect_no_config() {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class)) {
      var output = new Operations.ProcessExecOutput("test-output", "test-error", 0);

      mock.when(() -> Operations.getCustomPathOrElse(eq("skopeo")))
        .thenReturn(mockSkopeoPath);

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{mockSkopeoPath, "inspect",
            "--daemon-host", mockSkopeoDaemon, "",
            String.format("docker-daemon:%s", mockImageRef.getImage().getFullName())}),
          isNull()))
        .thenReturn(output);

      assertThat(ImageUtils.execSkopeoInspect(mockImageRef, false)).isEqualTo(output);
    }
  }

  @Test
  @ClearEnvironmentVariable(key = "EXHORT_SKOPEO_PATH")
  @SetEnvironmentVariable(key = EXHORT_SKOPEO_CONFIG_PATH, value = mockSkopeoConfig)
  @ClearEnvironmentVariable(key = EXHORT_IMAGE_SERVICE_ENDPOINT)
  void test_exec_skopeo_inspect_no_daemon() {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class)) {
      var output = new Operations.ProcessExecOutput("test-output", "test-error", 0);

      mock.when(() -> Operations.getCustomPathOrElse(eq("skopeo")))
        .thenReturn("skopeo");

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{"skopeo", "inspect", "--authfile", mockSkopeoConfig,
            "", String.format("docker://%s", mockImageRef.getImage().getFullName())}),
          isNull()))
        .thenReturn(output);

      assertThat(ImageUtils.execSkopeoInspect(mockImageRef, false)).isEqualTo(output);
    }
  }

  @Test
  @ClearEnvironmentVariable(key = "EXHORT_SKOPEO_PATH")
  @ClearEnvironmentVariable(key = EXHORT_SKOPEO_CONFIG_PATH)
  @ClearEnvironmentVariable(key = EXHORT_IMAGE_SERVICE_ENDPOINT)
  void test_exec_skopeo_inspect_no_config_no_daemon() {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class)) {
      var output = new Operations.ProcessExecOutput("test-output", "test-error", 0);

      mock.when(() -> Operations.getCustomPathOrElse(eq("skopeo")))
        .thenReturn("skopeo");

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{"skopeo", "inspect",
            "", String.format("docker://%s", mockImageRef.getImage().getFullName())}),
          isNull()))
        .thenReturn(output);

      assertThat(ImageUtils.execSkopeoInspect(mockImageRef, false)).isEqualTo(output);
    }
  }

  @Test
  void test_get_multi_image_digests() throws IOException {
    try (var is = getResourceAsStreamDecision(this.getClass(), new String[]{"msc", "image", "skopeo_inspect_multi_raw.json"})) {
      var mapper = new ObjectMapper();
      var node = mapper.readTree(is);

      var digests = ImageUtils.getMultiImageDigests(node);
      Map<Platform, String> expectedDigests = new HashMap<>();
      expectedDigests.put(new Platform("linux", "amd64", null), "sha256:06d06f15f7b641a78f2512c8817cbecaa1bf549488e273f5ac27ff1654ed33f0");
      expectedDigests.put(new Platform("linux", "arm64", null), "sha256:199d5daca3dba0a7deaf0086331917dee256089e94272bef5613517d0007f6f5");
      expectedDigests.put(new Platform("linux", "ppc64le", null), "sha256:1bba662cff053201db85aa55caf3273216a6b0e1766409ee133cf78df9b59314");
      expectedDigests.put(new Platform("linux", "s390x", null), "sha256:b39f9f6998e1693e29b7bd002bc32255fd4f69610e950523b647e61d2bb1dd66");

      assertEquals(expectedDigests, digests);
    }
  }

  @Test
  void test_get_multi_image_digests_empty() {
    var node = new TextNode("root");

    var digests = ImageUtils.getMultiImageDigests(node);
    Map<Platform, String> expectedDigests = Collections.emptyMap();

    assertEquals(expectedDigests, digests);
  }

  @Test
  void test_filter_mediaType() throws IOException {
    try (var is = getResourceAsStreamDecision(this.getClass(), new String[]{"msc", "image", "skopeo_inspect_multi_raw.json"})) {
      var mapper = new ObjectMapper();
      var node = mapper.readTree(is);

      assertTrue(ImageUtils.filterMediaType(node.get("manifests").get(0)));
    }
  }

  @Test
  void test_filter_digest() throws IOException {
    try (var is = getResourceAsStreamDecision(this.getClass(), new String[]{"msc", "image", "skopeo_inspect_multi_raw.json"})) {
      var mapper = new ObjectMapper();
      var node = mapper.readTree(is);

      assertTrue(ImageUtils.filterDigest(node.get("manifests").get(0)));
    }
  }

  @Test
  void test_filter_platform() throws IOException {
    try (var is = getResourceAsStreamDecision(this.getClass(), new String[]{"msc", "image", "skopeo_inspect_multi_raw.json"})) {
      var mapper = new ObjectMapper();
      var node = mapper.readTree(is);

      assertTrue(ImageUtils.filterPlatform(node.get("manifests").get(0)));
    }
  }

  @Test
  void test_get_single_image_digest() throws IOException {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class);
         var is = getResourceAsStreamDecision(this.getClass(), new String[]{"msc", "image", "skopeo_inspect_single.json"})) {
      var json = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
      var output = new Operations.ProcessExecOutput(json, "", 0);

      mock.when(() -> Operations.getCustomPathOrElse(eq("skopeo")))
        .thenReturn("skopeo");

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{"skopeo", "inspect",
            "", String.format("docker://%s", mockImageRef.getImage().getFullName())}),
          isNull()))
        .thenReturn(output);

      var digests = ImageUtils.getSingleImageDigest(mockImageRef);
      Map<Platform, String> expectedDigests = Collections.singletonMap(Platform.EMPTY_PLATFORM, "sha256:9aa20fd4e4842854ec1c081d2dae77c686601a8640018d68782f36c60eb1a19e");

      assertEquals(expectedDigests, digests);
    }
  }

  @Test
  void test_get_single_image_digest_empty() throws JsonProcessingException {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class)) {
      var mapper = new ObjectMapper();
      var node = new TextNode("root");
      var output = new Operations.ProcessExecOutput(mapper.writeValueAsString(node), "", 0);

      mock.when(() -> Operations.getCustomPathOrElse(eq("skopeo")))
        .thenReturn("skopeo");

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{"skopeo", "inspect",
            "", String.format("docker://%s", mockImageRef.getImage().getFullName())}),
          isNull()))
        .thenReturn(output);

      var digests = ImageUtils.getSingleImageDigest(mockImageRef);
      Map<Platform, String> expectedDigests = Collections.emptyMap();

      assertEquals(expectedDigests, digests);
    }
  }
}
