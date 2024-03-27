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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.packageurl.MalformedPackageURLException;
import com.redhat.exhort.logging.LoggersFactory;
import com.redhat.exhort.tools.Operations;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.redhat.exhort.image.Platform.EMPTY_PLATFORM;
import static com.redhat.exhort.impl.ExhortApi.getStringValueEnvironment;

public class ImageUtils {

  static final String EXHORT_SYFT_CONFIG_PATH = "EXHORT_SYFT_CONFIG_PATH";
  static final String EXHORT_SYFT_IMAGE_SOURCE = "EXHORT_SYFT_IMAGE_SOURCE";
  static final String EXHORT_IMAGE_PLATFORM = "EXHORT_IMAGE_PLATFORM";
  static final String EXHORT_IMAGE_OS = "EXHORT_IMAGE_OS";
  static final String EXHORT_IMAGE_ARCH = "EXHORT_IMAGE_ARCH";
  static final String EXHORT_IMAGE_VARIANT = "EXHORT_IMAGE_VARIANT";
  static final String EXHORT_SKOPEO_CONFIG_PATH = "EXHORT_SKOPEO_CONFIG_PATH";
  static final String EXHORT_IMAGE_SERVICE_ENDPOINT = "EXHORT_IMAGE_SERVICE_ENDPOINT";
  private static final String MEDIA_TYPE_DOCKER2_MANIFEST = "application/vnd.docker.distribution.manifest.v2+json";
  private static final String MEDIA_TYPE_DOCKER2_MANIFEST_LIST = "application/vnd.docker.distribution.manifest.list.v2+json";
  private static final String MEDIA_TYPE_OCI1_MANIFEST = "application/vnd.oci.image.manifest.v1+json";
  private static final String MEDIA_TYPE_OCI1_MANIFEST_LIST = "application/vnd.oci.image.index.v1+json";

  private static final Logger logger = LoggersFactory.getLogger(ImageUtils.class.getName());

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final Map<String, String> archMapping = Map.ofEntries(
    new AbstractMap.SimpleEntry<>("amd64", "amd64"),
    new AbstractMap.SimpleEntry<>("x86_64", "amd64"),
    new AbstractMap.SimpleEntry<>("armv5tl", "arm"),
    new AbstractMap.SimpleEntry<>("armv5tel", "arm"),
    new AbstractMap.SimpleEntry<>("armv5tejl", "arm"),
    new AbstractMap.SimpleEntry<>("armv6l", "arm"),
    new AbstractMap.SimpleEntry<>("armv7l", "arm"),
    new AbstractMap.SimpleEntry<>("armv7ml", "arm"),
    new AbstractMap.SimpleEntry<>("arm64", "arm64"),
    new AbstractMap.SimpleEntry<>("aarch64", "arm64"),
    new AbstractMap.SimpleEntry<>("i386", "386"),
    new AbstractMap.SimpleEntry<>("i486", "386"),
    new AbstractMap.SimpleEntry<>("i586", "386"),
    new AbstractMap.SimpleEntry<>("i686", "386"),
    new AbstractMap.SimpleEntry<>("mips64le", "mips64le"),
    new AbstractMap.SimpleEntry<>("ppc64le", "ppc64le"),
    new AbstractMap.SimpleEntry<>("riscv64", "riscv64"),
    new AbstractMap.SimpleEntry<>("s390x", "s390x")
  );
  private static final Map<String, String> variantMapping = Map.ofEntries(
    new AbstractMap.SimpleEntry<>("armv5tl", "v5"),
    new AbstractMap.SimpleEntry<>("armv5tel", "v5"),
    new AbstractMap.SimpleEntry<>("armv5tejl", "v5"),
    new AbstractMap.SimpleEntry<>("armv6l", "v6"),
    new AbstractMap.SimpleEntry<>("armv7l", "v7"),
    new AbstractMap.SimpleEntry<>("armv7ml", "v7"),
    new AbstractMap.SimpleEntry<>("arm64", "v8"),
    new AbstractMap.SimpleEntry<>("aarch64", "v8")
  );

  static String updatePATHEnv(String execPath) {
    String path = System.getenv("PATH");
    if (path != null) {
      return String.format("PATH=%s%s%s", path, File.pathSeparator, execPath);
    } else {
      return String.format("PATH=%s", execPath);
    }
  }

  public static JsonNode generateImageSBOM(ImageRef imageRef) throws IOException, MalformedPackageURLException {
    var output = execSyft(imageRef);

    if (!output.getError().isEmpty() || output.getExitCode() != 0) {
      throw new RuntimeException(output.getError());
    }

    var node = OBJECT_MAPPER.readTree(output.getOutput());
    if (node.hasNonNull("metadata")) {
      var metadataNode = node.get("metadata");
      if (metadataNode.hasNonNull("component")) {
        var componentNode = metadataNode.get("component");
        if (componentNode.isObject()) {
          String imagePurl = imageRef.getPackageURL().canonicalize();
          ((ObjectNode) componentNode).set("purl", new TextNode(imagePurl));
          return node;
        }
      }
    }

    throw new RuntimeException(String.format("The generated SBOM of the image is invalid: %s", output.getOutput()));
  }

  static Operations.ProcessExecOutput execSyft(ImageRef imageRef) {
    var syft = Operations.getCustomPathOrElse("syft");
    var docker = Operations.getCustomPathOrElse("docker");
    var podman = Operations.getCustomPathOrElse("podman");

    var syftConfigPath = getStringValueEnvironment(EXHORT_SYFT_CONFIG_PATH, "");
    var imageSource = getStringValueEnvironment(EXHORT_SYFT_IMAGE_SOURCE, "");
    SyftImageSource.getImageSource(imageSource);

    var dockerPath = docker != null && docker.contains(File.separator) ?
      docker.substring(0, docker.lastIndexOf(File.separator) + 1) : "";
    var podmanPath = podman != null && podman.contains(File.separator) ?
      podman.substring(0, podman.lastIndexOf(File.separator) + 1) : "";
    var envs = getSyftEnvs(dockerPath, podmanPath);

    var scheme = imageRef.getImage().toString();

    String[] cmd;
    if (!imageSource.isEmpty()) {
      cmd = syftConfigPath.isEmpty() ?
        new String[]{syft, scheme, "--from", imageSource, "-s", "all-layers", "-o", "cyclonedx-json", "-q"} :
        new String[]{syft, scheme, "--from", imageSource, "-c", syftConfigPath, "-s", "all-layers", "-o", "cyclonedx-json", "-q"};
    } else {
      cmd = syftConfigPath.isEmpty() ?
        new String[]{syft, scheme, "-s", "all-layers", "-o", "cyclonedx-json", "-q"} :
        new String[]{syft, scheme, "-c", syftConfigPath, "-s", "all-layers", "-o", "cyclonedx-json", "-q"};
    }

    return Operations.runProcessGetFullOutput(null, cmd,
      envs.isEmpty() ? null : envs.toArray(new String[1]));
  }

  static List<String> getSyftEnvs(String dockerPath, String podmanPath) {
    String path = null;
    if (!dockerPath.isEmpty() && !podmanPath.isEmpty()) {
      path = String.format("%s%s%s", dockerPath, File.pathSeparator, podmanPath);
    } else if (!dockerPath.isEmpty()) {
      path = dockerPath;
    } else if (!podmanPath.isEmpty()) {
      path = podmanPath;
    }
    var envPath = path != null ? updatePATHEnv(path) : null;

    List<String> envs = new ArrayList<>(1);
    if (envPath != null) {
      envs.add(envPath);
    }
    return envs;
  }

  public static Platform getImagePlatform() {
    var platform = getStringValueEnvironment(EXHORT_IMAGE_PLATFORM, "");
    if (!platform.isEmpty()) {
      return new Platform(platform);
    }

    var imageSource = getStringValueEnvironment(EXHORT_SYFT_IMAGE_SOURCE, "");
    SyftImageSource source = SyftImageSource.getImageSource(imageSource);

    var os = getStringValueEnvironment(EXHORT_IMAGE_OS, "");
    if (os.isEmpty()) {
      os = source.getOs();
    }
    var arch = getStringValueEnvironment(EXHORT_IMAGE_ARCH, "");
    if (arch.isEmpty()) {
      arch = source.getArch();
    }
    if (!os.isEmpty() && !arch.isEmpty()) {
      if (!Platform.isVariantRequired(os, arch)) {
        return new Platform(os, arch, null);
      }

      var variant = getStringValueEnvironment(EXHORT_IMAGE_VARIANT, "");
      if (variant.isEmpty()) {
        variant = source.getVariant();
      }
      if (!variant.isEmpty()) {
        return new Platform(os, arch, variant);
      }
    }

    return null;
  }

  static String hostInfo(String engine, String info) {
    var exec = Operations.getCustomPathOrElse(engine);
    var cmd = new String[]{exec, "info"};

    var output = Operations.runProcessGetFullOutput(null, cmd, null);
    if (output.getOutput().isEmpty() && (!output.getError().isEmpty() || output.getExitCode() != 0)) {
      throw new RuntimeException(output.getError());
    }

    return output.getOutput()
      .lines()
      .filter(line -> line.stripLeading().startsWith(info + ":"))
      .map(line -> line.strip().substring(info.length() + 1).strip())
      .findAny()
      .orElse("");
  }

  static String dockerGetOs() {
    return hostInfo("docker", "OSType");
  }

  static String dockerGetArch() {
    var arch = hostInfo("docker", "Architecture");
    arch = archMapping.get(arch);
    return Objects.requireNonNullElse(arch, "");
  }

  static String dockerGetVariant() {
    var variant = hostInfo("docker", "Architecture");
    variant = variantMapping.get(variant);
    return Objects.requireNonNullElse(variant, "");
  }

  static String podmanGetOs() {
    return hostInfo("podman", "os");
  }

  static String podmanGetArch() {
    return hostInfo("podman", "arch");
  }

  static String podmanGetVariant() {
    return hostInfo("podman", "variant");
  }

  static String dockerPodmanInfo(Supplier<String> dockerSupplier, Supplier<String> podmanSupplier) {
    var info = dockerSupplier.get();
    if (info.isEmpty()) {
      info = podmanSupplier.get();
    }
    return info;
  }

  public static Map<Platform, String> getImageDigests(ImageRef imageRef) throws JsonProcessingException {
    var output = execSkopeoInspect(imageRef, true);

    if (!output.getError().isEmpty() || output.getExitCode() != 0) {
      throw new RuntimeException(output.getError());
    }

    var node = OBJECT_MAPPER.readTree(output.getOutput());
    if (node.hasNonNull("mediaType")) {
      var mediaTypeNode = node.get("mediaType");
      if (mediaTypeNode.isTextual()) {
        var mediaType = mediaTypeNode.asText();
        switch (mediaType) {
          case MEDIA_TYPE_OCI1_MANIFEST:
          case MEDIA_TYPE_DOCKER2_MANIFEST:
            return getSingleImageDigest(imageRef);

          case MEDIA_TYPE_OCI1_MANIFEST_LIST:
          case MEDIA_TYPE_DOCKER2_MANIFEST_LIST:
            return getMultiImageDigests(node);
        }
      }
    }

    throw new RuntimeException(String.format("The image info is invalid: %s", output.getOutput()));
  }

  static Map<Platform, String> getMultiImageDigests(JsonNode node) {
    if (node.hasNonNull("manifests")) {
      var manifestsNode = node.get("manifests");
      if (manifestsNode.isArray()) {
        return StreamSupport.stream(manifestsNode.spliterator(), false)
          .filter(ImageUtils::filterMediaType)
          .filter(ImageUtils::filterDigest)
          .filter(ImageUtils::filterPlatform)
          .collect(Collectors.toMap(
            manifestNode -> {
              var platformNode = manifestNode.get("platform");
              var arch = platformNode.get("architecture").asText();
              var os = platformNode.get("os").asText();
              if (platformNode.hasNonNull("variant")) {
                var variant = platformNode.get("variant").asText();
                return new Platform(String.format("%s/%s/%s", os, arch, variant));
              } else {
                return new Platform(String.format("%s/%s", os, arch));
              }
            },
            manifestNode -> manifestNode.get("digest").asText()
          ));
      }
    }
    return Collections.emptyMap();
  }

  static boolean filterMediaType(JsonNode manifestNode) {
    if (manifestNode.hasNonNull("mediaType")) {
      var mediaTypeNode = manifestNode.get("mediaType");
      if (mediaTypeNode.isTextual()) {
        var mediaType = mediaTypeNode.asText();
        return MEDIA_TYPE_OCI1_MANIFEST.equals(mediaType) || MEDIA_TYPE_DOCKER2_MANIFEST.equals(mediaType);
      }
    }
    return false;
  }

  static boolean filterDigest(JsonNode manifestNode) {
    if (manifestNode.hasNonNull("digest")) {
      var digestNode = manifestNode.get("digest");
      return digestNode.isTextual();
    }
    return false;
  }

  static boolean filterPlatform(JsonNode manifestNode) {
    if (manifestNode.hasNonNull("platform")) {
      var platformNode = manifestNode.get("platform");
      if (platformNode.isObject()) {
        if (platformNode.hasNonNull("architecture") && platformNode.hasNonNull("os")) {
          var architectureNode = platformNode.get("architecture");
          var osNode = platformNode.get("os");
          if (architectureNode.isTextual() && osNode.isTextual()) {
            if (platformNode.hasNonNull("variant")) {
              var variantNode = platformNode.get("variant");
              if (variantNode.isTextual()) {
                try {
                  new Platform(String.format("%s/%s/%s", osNode.asText(), architectureNode.asText(), variantNode.asText()));
                } catch (IllegalArgumentException e) {
                  return false;
                }
                return true;
              }
            }
            try {
              new Platform(String.format("%s/%s", osNode.asText(), architectureNode.asText()));
            } catch (IllegalArgumentException e) {
              return false;
            }
            return true;
          }
        }
      }
    }
    return false;
  }

  static Map<Platform, String> getSingleImageDigest(ImageRef imageRef) throws JsonProcessingException {
    var output = execSkopeoInspect(imageRef, false);

    if (!output.getError().isEmpty() || output.getExitCode() != 0) {
      throw new RuntimeException(output.getError());
    }

    var node = OBJECT_MAPPER.readTree(output.getOutput());

    if (node.hasNonNull("Digest")) {
      var digestNode = node.get("Digest");
      if (digestNode.isTextual()) {
        return Collections.singletonMap(EMPTY_PLATFORM, digestNode.asText());
      }
    }
    return Collections.emptyMap();
  }

  static Operations.ProcessExecOutput execSkopeoInspect(ImageRef imageRef, boolean raw) {
    var skopeo = Operations.getCustomPathOrElse("skopeo");

    var configPath = getStringValueEnvironment(EXHORT_SKOPEO_CONFIG_PATH, "");
    var daemonHost = getStringValueEnvironment(EXHORT_IMAGE_SERVICE_ENDPOINT, "");

    String[] cmd;
    if (daemonHost.isEmpty()) {
      cmd = configPath.isEmpty() ?
        new String[]{skopeo, "inspect", raw ? "--raw" : "",
          String.format("docker://%s", imageRef.getImage().getFullName())} :
        new String[]{skopeo, "inspect", "--authfile", configPath, raw ? "--raw" : "",
          String.format("docker://%s", imageRef.getImage().getFullName())};
    } else {
      cmd = configPath.isEmpty() ?
        new String[]{skopeo, "inspect", "--daemon-host", daemonHost, raw ? "--raw" : "",
          String.format("docker-daemon:%s", imageRef.getImage().getFullName())} :
        new String[]{skopeo, "inspect", "--authfile", configPath, "--daemon-host", daemonHost, raw ? "--raw" : "",
          String.format("docker-daemon:%s", imageRef.getImage().getFullName())};
    }

    return Operations.runProcessGetFullOutput(null, cmd, null);
  }

  private enum SyftImageSource {
    DEFAULT("",
      () -> dockerPodmanInfo(ImageUtils::dockerGetOs, ImageUtils::podmanGetOs),
      () -> dockerPodmanInfo(ImageUtils::dockerGetArch, ImageUtils::podmanGetArch),
      () -> dockerPodmanInfo(ImageUtils::dockerGetVariant, ImageUtils::podmanGetVariant)),
    REGISTRY("registry",
      () -> dockerPodmanInfo(ImageUtils::dockerGetOs, ImageUtils::podmanGetOs),
      () -> dockerPodmanInfo(ImageUtils::dockerGetArch, ImageUtils::podmanGetArch),
      () -> dockerPodmanInfo(ImageUtils::dockerGetVariant, ImageUtils::podmanGetVariant)),
    DOCKER("docker",
      ImageUtils::dockerGetOs,
      ImageUtils::dockerGetArch,
      ImageUtils::dockerGetVariant),
    PODMAN("podman",
      ImageUtils::podmanGetOs,
      ImageUtils::podmanGetArch,
      ImageUtils::podmanGetVariant);

    private final String name;
    private final Supplier<String> osSupplier;
    private final Supplier<String> archSupplier;
    private final Supplier<String> variantSupplier;

    SyftImageSource(String name,
                    Supplier<String> osSupplier,
                    Supplier<String> archSupplier,
                    Supplier<String> variantSupplier) {
      this.name = name;
      this.osSupplier = osSupplier;
      this.archSupplier = archSupplier;
      this.variantSupplier = variantSupplier;
    }

    static SyftImageSource getImageSource(String name) {
      return EnumSet.allOf(SyftImageSource.class).stream()
        .filter(s -> s.name.equals(name))
        .findAny()
        .orElseThrow(() -> new IllegalArgumentException(String.format("The image source for syft is not valid: %s", name)));
    }

    String getOs() {
      return osSupplier.get();
    }

    String getArch() {
      return archSupplier.get();
    }

    String getVariant() {
      return variantSupplier.get();
    }
  }
}

