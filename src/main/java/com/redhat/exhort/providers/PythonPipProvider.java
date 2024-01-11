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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.redhat.exhort.Api;
import com.redhat.exhort.Provider;
import com.redhat.exhort.sbom.Sbom;
import com.redhat.exhort.sbom.SbomFactory;
import com.redhat.exhort.tools.Ecosystem;
import com.redhat.exhort.tools.Operations;
import com.redhat.exhort.utils.PythonControllerBase;
import com.redhat.exhort.utils.PythonControllerRealEnv;
import com.redhat.exhort.utils.PythonControllerVirtualEnv;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.redhat.exhort.impl.ExhortApi.debugLoggingIsNeeded;
import static com.redhat.exhort.impl.ExhortApi.getBooleanValueEnvironment;

public final class PythonPipProvider extends Provider {

  private System.Logger log = System.getLogger(this.getClass().getName());
  public void setPythonController(PythonControllerBase pythonController) {
    this.pythonController = pythonController;
  }

  private PythonControllerBase pythonController;
  public static void main(String[] args) {
    try {
      PythonPipProvider pythonPipProvider = new PythonPipProvider();
//      byte[] bytes = Files.readAllBytes(Path.of("/tmp/exhort_env/requirements.txt"));
//      Content content = pythonPipProvider.provideComponent(bytes);
      Content content = pythonPipProvider.provideStack(Path.of("/home/zgrinber/git/exhort-java-api/src/test/resources/tst_manifests/pip/pip_requirements_txt_ignore/requirements.txt"));
      String s = new String(content.buffer);
      System.out.print(s);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public PythonPipProvider() {
    super(Ecosystem.Type.PYTHON);
  }

  @Override
  public Content provideStack(Path manifestPath) throws IOException {
    PythonControllerBase pythonController = getPythonController();
    List<Map<String, Object>> dependencies = pythonController.getDependencies(manifestPath.toString(), true);
    printDependenciesTree(dependencies);
    Sbom sbom = SbomFactory.newInstance(Sbom.BelongingCondition.PURL,"sensitive");
    try {
      sbom.addRoot(new PackageURL(Ecosystem.Type.PYTHON.getType(), "root"));
    } catch (MalformedPackageURLException e) {
      throw new RuntimeException(e);
    }
    dependencies.stream().forEach((component) ->
    {
      addAllDependencies(sbom.getRoot(), component, sbom);

    });
    byte[] requirementsFile = Files.readAllBytes(manifestPath);
    handleIgnoredDependencies(new String(requirementsFile), sbom);
    // In python' pip requirements.txt, there is no real root element, then need to remove dummy root element that was created for creating the sbom.
    sbom.removeRootComponent();
    return new Content(sbom.getAsJsonString().getBytes(StandardCharsets.UTF_8), Api.CYCLONEDX_MEDIA_TYPE);
  }

  private void addAllDependencies(PackageURL source, Map<String, Object> component, Sbom sbom) {

    sbom.addDependency(source, toPurl((String) component.get("name"), (String) component.get("version")));
    List<Map> directDeps = (List<Map>) component.get("dependencies");
    if (directDeps != null)
//    {
      directDeps.stream().forEach(dep -> {
        String name = (String) dep.get("name");
        String version = (String) dep.get("version");

        addAllDependencies(toPurl((String) component.get("name"), (String) component.get("version")), dep, sbom);
      });
//
//    }

  }

  @Override
  public Content provideComponent(byte[] manifestContent) throws IOException {
    PythonControllerBase pythonController = getPythonController();
    Path tempRepository = Files.createTempDirectory("exhort-pip");
    Path path = Paths.get(tempRepository.toAbsolutePath().normalize().toString(), "requirements.txt");
    Files.deleteIfExists(path);
    Path manifestPath = Files.createFile(path);
    Files.write(manifestPath, manifestContent);
    List<Map<String, Object>> dependencies = pythonController.getDependencies(manifestPath.toString(), false);
    printDependenciesTree(dependencies);
    Sbom sbom = SbomFactory.newInstance();
    try {
      sbom.addRoot(new PackageURL(Ecosystem.Type.PYTHON.getType(), "root"));
    } catch (MalformedPackageURLException e) {
      throw new RuntimeException(e);
    }
    dependencies.stream().forEach((component) ->
    {
      sbom.addDependency(sbom.getRoot(), toPurl((String) component.get("name"), (String) component.get("version")));
    });
    Files.delete(manifestPath);
    Files.delete(tempRepository);
    handleIgnoredDependencies(new String(manifestContent), sbom);
    // In python' pip requirements.txt, there is no real root element, then need to remove dummy root element that was created for creating the sbom.
    sbom.removeRootComponent();
    return new Content(sbom.getAsJsonString().getBytes(StandardCharsets.UTF_8), Api.CYCLONEDX_MEDIA_TYPE);

  }

  private void printDependenciesTree(List<Map<String, Object>> dependencies) throws JsonProcessingException {
    if(debugLoggingIsNeeded()) {
      String pythonControllerTree = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dependencies);
      log.log(System.Logger.Level.INFO,String.format("Python Generated Dependency Tree in Json Format: %s %s %s",System.lineSeparator(),pythonControllerTree,System.lineSeparator()));

    }
  }

  private void handleIgnoredDependencies(String manifestContent, Sbom sbom) {
    Set<PackageURL> ignoredDeps = getIgnoredDependencies(manifestContent);
    Set ignoredDepsVersions = ignoredDeps
                              .stream()
                              .filter(dep -> !dep.getVersion().trim().equals("*"))
                              .map(PackageURL::getCoordinates)
                              .collect(Collectors.toSet());
    Set ignoredDepsNoVersions = ignoredDeps
                                .stream()
                                .filter(dep -> dep.getVersion().trim().equals("*"))
                                .map(PackageURL::getCoordinates)
                                .collect(Collectors.toSet());

// filter out by name only from sbom all exhortignore dependencies that their version will be resolved by pip.
    sbom.setBelongingCriteriaBinaryAlgorithm(Sbom.BelongingCondition.NAME);
    sbom.filterIgnoredDeps(ignoredDepsNoVersions);
    boolean matchManifestVersions = getBooleanValueEnvironment("MATCH_MANIFEST_VERSIONS", "true");
    // filter out by purl from sbom all exhortignore dependencies that their version hardcoded in requirements.txt - in case all versions in manifest matching installed versions of packages in environment.
    if(matchManifestVersions)
    {
      sbom.setBelongingCriteriaBinaryAlgorithm(Sbom.BelongingCondition.PURL);
      sbom.filterIgnoredDeps(ignoredDepsVersions);
    }
    else
    {
// in case version mismatch is possible (MATCH_MANIFEST_VERSIONS=false) , need to parse the name of package from the purl, and remove the package name from sbom according to name only
      Set deps = (Set) ignoredDepsVersions.stream().map(purlString -> {
        try {
          return new PackageURL((String) purlString).getName();
        } catch (MalformedPackageURLException e) {
          throw new RuntimeException(e);
        }
      }).collect(Collectors.toSet());
      sbom.setBelongingCriteriaBinaryAlgorithm(Sbom.BelongingCondition.NAME);
      sbom.filterIgnoredDeps(deps);
    }




  }

  private Set getIgnoredDependencies(String requirementsDeps) {

    String[] requirementsLines = requirementsDeps.split(System.lineSeparator());
    Set<PackageURL> collected = Arrays.stream(requirementsLines)
      .filter(line -> line.contains("#exhortignore") || line.contains("# exhortignore"))
      .map(PythonPipProvider::extractDepFull)
      .map(this::splitToNameVersion)
      .map(dep -> toPurl(dep[0], dep[1]))
//      .map(packageURL -> packageURL.getCoordinates())
      .collect(Collectors.toSet());

     return collected;
  }

  private String[] splitToNameVersion(String nameVersion) {
    String[] result;
    if (nameVersion.matches("[a-zA-Z0-9-_()]+={2}[0-9]{1,4}[.][0-9]{1,4}(([.][0-9]{1,4})|([.][a-zA-Z0-9]+)|([a-zA-Z0-9]+)|([.][a-zA-Z0-9]+[.][a-z-A-Z0-9]+))?")) {
      result = nameVersion.split("==");
    } else {
      String dependencyName = PythonControllerBase.getDependencyName(nameVersion);
      result = new String[]{dependencyName, "*"};
    }
    return result;
  }

  private static String extractDepFull(String requirementLine) {
    return requirementLine.substring(0, requirementLine.indexOf("#")).trim();
  }

  private PackageURL toPurl(String name, String version) {

    try {
      return new PackageURL(Ecosystem.Type.PYTHON.getType(), null, name, version, null, null);
    } catch (MalformedPackageURLException e) {
      throw new RuntimeException(e);
    }
  }

  private PythonControllerBase getPythonController() {
    String pythonPipBinaries = getPythonPipBinaries();
    String[] parts = pythonPipBinaries.split(";;");
    var python = parts[0];
    var pip = parts[1];
    String useVirtualPythonEnv = Objects.requireNonNullElseGet(
      System.getenv("EXHORT_PYTHON_VIRTUAL_ENV"),
      () -> Objects.requireNonNullElse(System.getProperty("EXHORT_PYTHON_VIRTUAL_ENV"), "false"));
    PythonControllerBase pythonController;
    if(this.pythonController == null) {
      if (Boolean.parseBoolean(useVirtualPythonEnv)) {
        pythonController = new PythonControllerVirtualEnv(python);
      } else {
        pythonController = new PythonControllerRealEnv(python, pip);
      }
    }
    else {
      pythonController = this.pythonController;
    }
    return pythonController;
  }

  private static String getPythonPipBinaries() {
    var python = Operations.getCustomPathOrElse("python3");
    var pip = Operations.getCustomPathOrElse("pip3");
    try {
      Operations.runProcess(python, "--version");
      Operations.runProcess(pip, "--version");
    } catch (Exception e) {
      python = Operations.getCustomPathOrElse("python");
      pip = Operations.getCustomPathOrElse("pip");
      Operations.runProcess(python, "--version");
      Operations.runProcess(pip, "--version");
    }
    return String.format("%s;;%s", python, pip);
  }

  @Override
  public Content provideComponent(Path manifestPath) throws IOException {
    throw new IllegalArgumentException("provideComponent with file system path for Python pip package manager is not supported");
  }
}
