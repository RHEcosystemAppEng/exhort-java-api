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
package com.redhat.exhort.providers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.redhat.exhort.Api;
import com.redhat.exhort.Provider;
import com.redhat.exhort.sbom.Sbom;
import com.redhat.exhort.sbom.SbomFactory;
import com.redhat.exhort.tools.Ecosystem;
import com.redhat.exhort.tools.Ecosystem.Type;
import com.redhat.exhort.tools.Operations;

/**
 * Concrete implementation of the {@link Provider} used for converting
 * dependency trees
 * for npm projects (package.json) into a SBOM content for Stack analysis or
 * Component analysis.
 **/
public final class JavaScriptNpmProvider extends Provider {

  public JavaScriptNpmProvider() {
    super(Type.NPM);
  }

  @Override
  public Content provideStack(final Path manifestPath) throws IOException {
    // check for custom npm executable
    Sbom sbom = getDependencySbom(manifestPath, true);
    return new Content(sbom.getAsJsonString().getBytes(StandardCharsets.UTF_8), Api.CYCLONEDX_MEDIA_TYPE);
  }

  @Override
  public Content provideComponent(byte[] manifestContent) throws IOException {
    // check for custom npm executable
    return new Content(getDependencyTree(manifestContent).getAsJsonString().getBytes(StandardCharsets.UTF_8),
      Api.CYCLONEDX_MEDIA_TYPE);
  }

  private Sbom getDependencyTree(byte[] manifestContent) {
    Sbom sbom;
    try {
      Path path = Paths.get(Paths.get(".").toAbsolutePath().normalize().toString(), "package.json");
      Files.deleteIfExists(path);
      Path manifestPath = Files.createFile(path);
      Files.write(manifestPath, manifestContent);
      sbom = getDependencySbom(manifestPath, false);

      Files.delete(manifestPath);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return sbom;
  }

  private PackageURL getRoot(JsonNode jsonDependenciesNpm) throws MalformedPackageURLException {
    return toPurl(jsonDependenciesNpm.get("name").asText(), jsonDependenciesNpm.get("version").asText());
  }

  private PackageURL toPurl(String name, String version) throws MalformedPackageURLException {
    String[] parts = name.split("/");
    if (parts.length == 2) {
      return new PackageURL(Ecosystem.Type.NPM.getType(), parts[0], parts[1], version, null, null);
    }
    return new PackageURL(Ecosystem.Type.NPM.getType(), null, parts[0], version, null, null);
  }

  private void addDependenciesOf(Sbom sbom, PackageURL from, JsonNode dependencies)
      throws MalformedPackageURLException {
    Iterator<Entry<String, JsonNode>> fields = dependencies.fields();
    while (fields.hasNext()) {
      Entry<String, JsonNode> e = fields.next();
      String name = e.getKey();
      JsonNode versionNode = e.getValue().get("version");
      String version = "";
      if (versionNode != null) {
        version = versionNode.asText();
      }
      PackageURL purl = toPurl(name, version);
      sbom.addDependency(from, purl);
      JsonNode transitiveDeps = e.getValue().findValue("dependencies");
      if (transitiveDeps != null) {
        addDependenciesOf(sbom, purl, transitiveDeps);
      }
    }
  }

  private Sbom getDependencySbom(Path manifestPath, boolean includeTransitive) throws IOException {
    var npmListResult = buildNpmDependencyTree(manifestPath, includeTransitive);
    var sbom = buildSbom(npmListResult);
    sbom.filterIgnoredDeps(getIgnoredDeps(manifestPath));
    return sbom;
  }

  private JsonNode buildNpmDependencyTree(Path manifestPath, boolean includeTransitive)
      throws JsonMappingException, JsonProcessingException {
    var npm = Operations.getCustomPathOrElse("npm");
    // clean command used to clean build target
    Path packageLockJson = Path.of(manifestPath.getParent().toString(), "package-lock.json");
    if (!packageLockJson.toFile().exists()) {
      var createPackageLock = new String[] { npm, "i", "--package-lock-only", "--prefix",
          manifestPath.getParent().toString() };
      // execute the clean command
      Operations.runProcess(createPackageLock);
    }

    var npmAllDeps = new String[] { npm, "ls", includeTransitive ? "--all" : "", "--omit=dev", "--package-lock-only",
        "--json", "--prefix", manifestPath.getParent().toString() };
    // execute the clean command
    String npmOutput = Operations.runProcessGetOutput(npmAllDeps);
    return objectMapper.readTree(npmOutput);
  }

  private Sbom buildSbom(JsonNode npmListResult) {
    Sbom sbom = SbomFactory.newInstance();
    try {
      PackageURL root = getRoot(npmListResult);
      sbom.addRoot(root);
      JsonNode dependencies = npmListResult.get("dependencies");
      addDependenciesOf(sbom, root, dependencies);
    } catch (MalformedPackageURLException e) {
      throw new IllegalArgumentException("Unable to parse NPM Json", e);
    }
    return sbom;
  }

  private List<String> getIgnoredDeps(Path manifestPath) throws IOException {
    var ignored = new ArrayList<String>();
    var root = new ObjectMapper().readTree(Files.newInputStream(manifestPath));
    var ignoredNode = root.withArray("exhortignore");
    if (ignoredNode == null) {
      return ignored;
    }
    for (JsonNode n : ignoredNode) {
      ignored.add(n.asText());
    }
    return ignored;
  }
}
