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
package com.redhat.exhort.utils;

import static com.redhat.exhort.impl.ExhortApi.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.exhort.exception.PackageNotInstalledException;
import com.redhat.exhort.logging.LoggersFactory;
import com.redhat.exhort.tools.Operations;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class PythonControllerBase {
  public static void main(String[] args) {

    PythonControllerBase pythonController;
    //    pythonController = new PythonControllerVirtualEnv("/usr/bin/python3");
    LocalDateTime start = LocalDateTime.now();
    List<Map<String, Object>> dependencies;
    //    dependencies = pythonController.getDependencies("/tmp/requirements.txt",true);
    LocalDateTime end = LocalDateTime.now();
    System.out.println("start time:" + start + "\n");
    System.out.println("end time:" + end + "\n");
    System.out.println("elapsed time: " + start.until(end, ChronoUnit.SECONDS) + "\n");
    pythonController = new PythonControllerRealEnv("/usr/bin/python3", "/usr/bin/pip3");
    start = LocalDateTime.now();
    try {
      dependencies =
          pythonController.getDependencies(
              "/home/zgrinber/git/exhort-java-api/src/test/resources/tst_manifests/pip/pip_requirements_txt_ignore/requirements.txt",
              true);
    } catch (PackageNotInstalledException e) {
      System.out.println(e.getMessage());
      dependencies = null;
    }
    end = LocalDateTime.now();
    //    LocalDateTime startNaive = LocalDateTime.now();
    //    List<Map<String, Object>> dependenciesNaive = pythonController.getDependenciesNaive();
    //    LocalDateTime endNaive = LocalDateTime.now();
    System.out.println("start time:" + start + "\n");
    System.out.println("end time:" + end + "\n");
    System.out.println("elapsed time: " + start.until(end, ChronoUnit.SECONDS) + "\n");
    //    System.out.println("naive start time:" + startNaive + "\n" );
    //    System.out.println("naive end time:" + endNaive + "\n");
    //    System.out.println("elapsed time: " + startNaive.until(endNaive, ChronoUnit.SECONDS));

    ObjectMapper om = new ObjectMapper();
    try {
      String json = om.writerWithDefaultPrettyPrinter().writeValueAsString(dependencies);
      System.out.println(json);
      //      System.out.println(pythonController.counter);

    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private Logger log = LoggersFactory.getLogger(this.getClass().getName());
  protected Path pythonEnvironmentDir;
  protected Path pipBinaryDir;

  protected String pathToPythonBin;

  protected String pipBinaryLocation;

  //  public int counter =0;

  public abstract void prepareEnvironment(String pathToPythonBin);

  public abstract boolean automaticallyInstallPackageOnEnvironment();

  public abstract boolean isRealEnv();

  void installPackages(String pathToRequirements) {
    Operations.runProcess(pipBinaryLocation, "install", "-r", pathToRequirements);
    Operations.runProcess(pipBinaryLocation, "freeze");
  }

  public abstract boolean isVirtualEnv();

  public abstract void cleanEnvironment(boolean deleteEnvironment);

  //  public List<Map<String,Object>> getDependenciesNaive()
  //  {
  //    List<Map<String,Object>> dependencies = new ArrayList<>();
  //    String freeze = Operations.runProcessGetOutput(pythonEnvironmentDir, pipBinaryLocation,
  // "freeze");
  //    String[] deps = freeze.split(System.lineSeparator());
  //    Arrays.stream(deps).forEach(dep ->
  //    {
  //      Map<String,Object> component = new HashMap<>();
  //      dependencies.add(component);
  //      bringAllDependenciesNaive(component, getDependencyName(dep));
  //    });
  //
  //
  //
  //    return dependencies;
  //  }
  //
  //  private void bringAllDependenciesNaive(Map<String, Object> dependencies, String depName) {
  //
  //    if(dependencies == null || depName.trim().equals(""))
  //         return;
  //    counter++;
  //    LocalDateTime start = LocalDateTime.now();
  //    String pipShowOutput = Operations.runProcessGetOutput(pythonEnvironmentDir,
  // pipBinaryLocation, "show",
  // depName);
  //    LocalDateTime end = LocalDateTime.now();
  //    System.out.println("pip show start time:" + start + "\n");
  //    System.out.println("pip show end time:" + end  + "\n");
  //    System.out.println("pip show elapsed time: " + start.until(end, ChronoUnit.SECONDS)  + "\n"
  // );
  //    String depVersion = getDependencyVersion(pipShowOutput);
  //    List<String> directDeps = getDepsList(pipShowOutput);
  //    dependencies.put("name", depName);
  //    dependencies.put("version",depVersion);
  //    List<Map<String, Object>> targetDeps = new ArrayList<>();
  //    directDeps.stream().forEach(d -> {
  //      Map<String, Object> myMap = new HashMap<>();
  //      targetDeps.add(myMap);
  //      bringAllDependenciesNaive(myMap,d);
  //    });
  //    dependencies.put("dependencies",targetDeps);
  //
  //  }
  //  public List<Map<String,Object>> getDependencies()
  //  {
  //    List<Map<String,Object>> dependencies = new ArrayList<>();
  //    String freeze = Operations.runProcessGetOutput(pythonEnvironmentDir, pipBinaryLocation,
  // "freeze");
  //    String[] deps = freeze.split(System.lineSeparator());
  //    String depNames =
  // Arrays.stream(deps).map(this::getDependencyName).collect(Collectors.joining(" "));
  //    bringAllDependencies(dependencies, depNames);
  //
  //
  //
  //
  //    return dependencies;
  //  }
  //
  //  private void bringAllDependencies(List<Map<String, Object>> dependencies, String depName) {
  //
  //    if (dependencies == null || depName.trim().equals(""))
  //      return;
  //    counter++;
  //    LocalDateTime start = LocalDateTime.now();
  //    String pipShowOutput = Operations.runProcessGetOutput(pythonEnvironmentDir,
  // pipBinaryLocation, "show",
  // depName);
  //    LocalDateTime end = LocalDateTime.now();
  //    System.out.println("pip show start time:" + start + "\n");
  //    System.out.println("pip show end time:" + end  + "\n");
  //    System.out.println("pip show elapsed time: " + start.until(end, ChronoUnit.MILLIS)  + "\n"
  // );
  //    List<String> allLines =
  // Arrays.stream(pipShowOutput.split("---")).collect(Collectors.toList());
  //    allLines.stream().forEach(record -> {
  //      String depVersion = getDependencyVersion(record);
  //      List<String> directDeps = getDepsList(record);
  //      getDependencyNameShow(record);
  //      Map<String, Object> entry = new HashMap<String,Object>();
  //      dependencies.add(entry);
  //      entry.put("name", getDependencyNameShow(record));
  //      entry.put("version", depVersion);
  //      List<Map<String, Object>> targetDeps = new ArrayList<>();
  //      String depsList = directDeps.stream().map(str -> str.replace(",",
  // "")).collect(Collectors.joining(" "));
  //      bringAllDependencies(targetDeps, depsList);
  //      entry.put("dependencies",targetDeps);
  //    });
  //  }

  public final List<Map<String, Object>> getDependencies(
      String pathToRequirements, boolean includeTransitive) {
    if (isVirtualEnv() || isRealEnv()) {
      prepareEnvironment(pathToPythonBin);
    }
    if (automaticallyInstallPackageOnEnvironment()) {
      boolean installBestEfforts =
          getBooleanValueEnvironment("EXHORT_PYTHON_INSTALL_BEST_EFFORTS", "false");
      /*
       make best efforts to install the requirements.txt on the virtual environment created from
       the python3 passed in. that means that it will install the packages without referring to
       the versions, but will let pip choose the version tailored for version of the python
       environment( and of pip package manager) for each package.
      */
      if (installBestEfforts) {
        boolean matchManifestVersions =
            getBooleanValueEnvironment("MATCH_MANIFEST_VERSIONS", "true");
        if (matchManifestVersions) {
          throw new RuntimeException(
              "Conflicting settings, EXHORT_PYTHON_INSTALL_BEST_EFFORTS=true can only work with"
                  + " MATCH_MANIFEST_VERSIONS=false");
        } else {
          installingRequirementsOneByOne(pathToRequirements);
        }
      } else {
        installPackages(pathToRequirements);
      }
    }
    List<Map<String, Object>> dependencies =
        getDependenciesImpl(pathToRequirements, includeTransitive);
    if (isVirtualEnv()) {
      cleanEnvironment(false);
    }

    return dependencies;
  }

  private void installingRequirementsOneByOne(String pathToRequirements) {
    try {
      List<String> requirementsRows = Files.readAllLines(Path.of(pathToRequirements));
      requirementsRows.stream()
          .filter((line) -> !line.trim().startsWith("#"))
          .filter((line) -> !line.trim().equals(""))
          .forEach(
              (dependency) -> {
                String dependencyName = getDependencyName(dependency);
                try {
                  Operations.runProcess(this.pipBinaryLocation, "install", dependencyName);
                } catch (RuntimeException e) {
                  throw new RuntimeException(
                      String.format(
                          "Best efforts process - failed installing package - %s in created virtual"
                              + " python environment --> error message got from underlying process"
                              + " => %s ",
                          dependencyName, e.getMessage()));
                }
              });

    } catch (IOException e) {
      throw new RuntimeException(
          "Cannot continue with analysis - error opening requirements.txt file in order to install"
              + " packages one by one in a best efforts manner - related error message => "
              + e.getMessage());
    }
  }

  private List<Map<String, Object>> getDependenciesImpl(
      String pathToRequirements, boolean includeTransitive) {
    List<Map<String, Object>> dependencies = new ArrayList<>();
    Map<StringInsensitive, PythonDependency> cachedEnvironmentDeps = new HashMap<>();
    fillCacheWithEnvironmentDeps(cachedEnvironmentDeps);
    List<String> linesOfRequirements;
    try {
      linesOfRequirements =
          Files.readAllLines(Path.of(pathToRequirements)).stream()
              .filter((line) -> !line.startsWith("#"))
              .map(String::trim)
              .collect(Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    try {
      ObjectMapper om = new ObjectMapper();
      om.writerWithDefaultPrettyPrinter().writeValueAsString(cachedEnvironmentDeps);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    boolean matchManifestVersions = getBooleanValueEnvironment("MATCH_MANIFEST_VERSIONS", "true");

    for (String dep : linesOfRequirements) {
      if (matchManifestVersions) {
        String dependencyName;
        String manifestVersion;
        String installedVersion = "";
        int doubleEqualSignPosition;
        if (dep.contains("==")) {
          doubleEqualSignPosition = dep.indexOf("==");
          manifestVersion = dep.substring(doubleEqualSignPosition + 2).trim();
          if (manifestVersion.contains("#")) {
            var hashCharIndex = manifestVersion.indexOf("#");
            manifestVersion = manifestVersion.substring(0, hashCharIndex);
          }
          dependencyName = getDependencyName(dep);
          PythonDependency pythonDependency =
              cachedEnvironmentDeps.get(new StringInsensitive(dependencyName));
          if (pythonDependency != null) {
            installedVersion = pythonDependency.getVersion();
          }
          if (!installedVersion.trim().equals("")) {
            if (!manifestVersion.trim().equals(installedVersion.trim())) {
              throw new RuntimeException(
                  String.format(
                      "Can't continue with analysis - versions mismatch for dependency"
                          + " name=%s, manifest version=%s, installed Version=%s, if you"
                          + " want to allow version mismatch for analysis between installed"
                          + " and requested packages, set environment variable/setting -"
                          + " MATCH_MANIFEST_VERSIONS=false",
                      dependencyName, manifestVersion, installedVersion));
            }
          }
        }
      }
      List<String> path = new ArrayList<>();
      String selectedDepName = getDependencyName(dep.toLowerCase());
      path.add(selectedDepName);
      bringAllDependencies(
          dependencies, selectedDepName, cachedEnvironmentDeps, includeTransitive, path);
    }

    return dependencies;
  }

  private String getPipShowFromEnvironment(String depNames) {
    return executeCommandOrExtractFromEnv("EXHORT_PIP_SHOW", pipBinaryLocation, "show", depNames);
  }

  String getPipFreezeFromEnvironment() {
    return executeCommandOrExtractFromEnv(
        "EXHORT_PIP_FREEZE", pipBinaryLocation, "freeze", "--all");
  }

  List<PythonDependency> getDependencyTreeJsonFromPipDepTree() {
    executeCommandOrExtractFromEnv(
        "EXHORT_PIP_PIPDEPTREE", pipBinaryLocation, "install", "pipdeptree");

    String pipdeptreeJsonString;
    if (isVirtualEnv()) {
      pipdeptreeJsonString =
          executeCommandOrExtractFromEnv("EXHORT_PIP_PIPDEPTREE", "./bin/pipdeptree", "--json");
    } else if (isRealEnv()) {
      pipdeptreeJsonString =
          executeCommandOrExtractFromEnv(
              "EXHORT_PIP_PIPDEPTREE", pathToPythonBin, "-m", "pipdeptree", "--json");
    } else {
      pipdeptreeJsonString =
          executeCommandOrExtractFromEnv(
              "EXHORT_PIP_PIPDEPTREE", "./bin/pipdeptree", "--json", "--python", pathToPythonBin);
    }
    if (debugLoggingIsNeeded()) {
      String pipdeptreeMessage =
          String.format(
              "Package Manager pipdeptree --json command result output -> %s %s",
              System.lineSeparator(), pipdeptreeJsonString);
      log.info(pipdeptreeMessage);
    }
    return mapToPythonDependencies(pipdeptreeJsonString);
  }

  public static List<PythonDependency> mapToPythonDependencies(String jsonString) {
    // Parse JSON string using ObjectMapper
    ObjectMapper mapper = new ObjectMapper();
    JsonNode rootNode = null;
    try {
      rootNode = mapper.readTree(jsonString);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    List<PythonDependency> dependencies = new ArrayList<>();

    // Check if it's an array
    if (rootNode.isArray()) {
      for (JsonNode dependencyNode : rootNode) {
        if (dependencyNode.isObject()) {
          ObjectNode dependencyObject = (ObjectNode) dependencyNode;

          // Extract information from the nested "package" object
          JsonNode packageNode = dependencyObject.get("package");
          String name = packageNode.get("package_name").asText();
          String version = packageNode.get("installed_version").asText();

          // Extract dependencies (might be an array or an empty object)
          JsonNode dependenciesElement = dependencyObject.get("dependencies");
          List<String> depList = new ArrayList<>();
          if (dependenciesElement.isArray()) {
            // Loop through the dependencies array and add names
            for (JsonNode depNode : dependenciesElement) {
              String depName = depNode.get("package_name").asText();
              depList.add(depName);
            }
          }

          // Create a PythonDependency object and add it to the list
          PythonDependency dependency = new PythonDependency(name, version, depList);
          dependencies.add(dependency);
        }
      }
    }

    return dependencies;
  }

  private String executeCommandOrExtractFromEnv(String EnvVar, String... cmdList) {
    String envValue = getStringValueEnvironment(EnvVar, "");
    if (envValue.trim().equals(""))
      return Operations.runProcessGetOutput(pythonEnvironmentDir, cmdList);
    return new String(Base64.getDecoder().decode(envValue));
  }

  private void bringAllDependencies(
      List<Map<String, Object>> dependencyList,
      String depName,
      Map<StringInsensitive, PythonDependency> cachedTree,
      boolean includeTransitive,
      List<String> path) {

    if (dependencyList == null || depName.trim().equals("")) return;

    PythonDependency pythonDependency = cachedTree.get(new StringInsensitive(depName));
    if (pythonDependency == null) {
      throw new PackageNotInstalledException(
          String.format(
              "Package name=>%s is not installed on your python environment, either install it ("
                  + " better to install requirements.txt altogether) or turn on environment"
                  + " variable EXHORT_PYTHON_VIRTUAL_ENV=true to automatically installs it on"
                  + " virtual environment ( will slow down the analysis)",
              depName));
    }

    Map<String, Object> dataMap = new HashMap<>();
    dataMap.put("name", pythonDependency.getName());
    dataMap.put("version", pythonDependency.getVersion());
    dependencyList.add(dataMap);

    List<Map<String, Object>> transitiveDepList = new ArrayList<>();
    List<String> directDeps = pythonDependency.getDependencies();
    for (String directDep : directDeps) {
      if (!path.contains(directDep.toLowerCase())) {
        List<String> depList = new ArrayList<>();
        depList.add(directDep.toLowerCase());

        if (includeTransitive) {
          bringAllDependencies(
              transitiveDepList,
              directDep,
              cachedTree,
              true,
              Stream.concat(path.stream(), depList.stream()).collect(Collectors.toList()));
        }
      }
      transitiveDepList.sort(
          (map1, map2) -> {
            String string1 = (String) (map1.get("name"));
            String string2 = (String) (map2.get("name"));
            return Arrays.compare(string1.toCharArray(), string2.toCharArray());
          });
      dataMap.put("dependencies", transitiveDepList);
    }
  }

  protected List<String> getDepsList(String pipShowOutput) {
    int requiresKeyIndex = pipShowOutput.indexOf("Requires:");
    String requiresToken = pipShowOutput.substring(requiresKeyIndex + 9);
    int endOfLine = requiresToken.indexOf(System.lineSeparator());
    String listOfDeps;
    if (endOfLine > -1) {
      listOfDeps = requiresToken.substring(0, endOfLine).trim();
    } else {
      listOfDeps = requiresToken;
    }
    return Arrays.stream(listOfDeps.split(","))
        .map(String::trim)
        .filter(dep -> !dep.equals(""))
        .collect(Collectors.toList());
  }

  protected String getDependencyVersion(String pipShowOutput) {
    int versionKeyIndex = pipShowOutput.indexOf("Version:");
    String versionToken = pipShowOutput.substring(versionKeyIndex + 8);
    int endOfLine = versionToken.indexOf(System.lineSeparator());
    return versionToken.substring(0, endOfLine).trim();
  }

  protected String getDependencyNameShow(String pipShowOutput) {
    int versionKeyIndex = pipShowOutput.indexOf("Name:");
    String versionToken = pipShowOutput.substring(versionKeyIndex + 5);
    int endOfLine = versionToken.indexOf(System.lineSeparator());
    return versionToken.substring(0, endOfLine).trim();
  }

  public static String getDependencyName(String dep) {
    int rightTriangleBracket = dep.indexOf(">");
    int leftTriangleBracket = dep.indexOf("<");
    int equalsSign = dep.indexOf("=");
    int minimumIndex = getFirstSign(rightTriangleBracket, leftTriangleBracket, equalsSign);
    String depName;
    if (rightTriangleBracket == -1 && leftTriangleBracket == -1 && equalsSign == -1) {
      depName = dep;
    } else {
      depName = dep.substring(0, minimumIndex);
    }
    return depName.trim();
  }

  private static int getFirstSign(
      int rightTriangleBracket, int leftTriangleBracket, int equalsSign) {
    rightTriangleBracket = rightTriangleBracket == -1 ? 999 : rightTriangleBracket;
    leftTriangleBracket = leftTriangleBracket == -1 ? 999 : leftTriangleBracket;
    equalsSign = equalsSign == -1 ? 999 : equalsSign;
    return equalsSign < leftTriangleBracket && equalsSign < rightTriangleBracket
        ? equalsSign
        : (leftTriangleBracket < equalsSign && leftTriangleBracket < rightTriangleBracket
            ? leftTriangleBracket
            : rightTriangleBracket);
  }

  static List<String> splitPipShowLines(String pipShowOutput) {
    return Arrays.stream(
            pipShowOutput.split(System.lineSeparator() + "---" + System.lineSeparator()))
        .collect(Collectors.toList());
  }

  private PythonDependency getPythonDependencyByShowStringBlock(String pipShowStringBlock) {
    return new PythonDependency(
        getDependencyNameShow(pipShowStringBlock),
        getDependencyVersion(pipShowStringBlock),
        getDepsList(pipShowStringBlock));
  }

  private void fillCacheWithEnvironmentDeps(Map<StringInsensitive, PythonDependency> cache) {
    boolean usePipDepTree = getBooleanValueEnvironment("EXHORT_PIP_USE_DEP_TREE", "false");
    if (usePipDepTree) {
      getDependencyTreeJsonFromPipDepTree().forEach(d -> saveToCacheWithKeyVariations(cache, d));
    } else {
      String freezeOutput = getPipFreezeFromEnvironment();
      if (debugLoggingIsNeeded()) {
        String freezeMessage =
            String.format(
                "Package Manager PIP freeze --all command result output -> %s %s",
                System.lineSeparator(), freezeOutput);
        log.info(freezeMessage);
      }
      String[] deps = freezeOutput.split(System.lineSeparator());
      String depNames =
          Arrays.stream(deps)
              .map(PythonControllerBase::getDependencyName)
              .collect(Collectors.joining(" "));
      String pipShowOutput = getPipShowFromEnvironment(depNames);
      if (debugLoggingIsNeeded()) {
        String pipShowMessage =
            String.format(
                "Package Manager PIP show command result output -> %s %s",
                System.lineSeparator(), pipShowOutput);
        log.info(pipShowMessage);
      }
      splitPipShowLines(pipShowOutput).stream()
          .map(this::getPythonDependencyByShowStringBlock)
          .forEach(d -> saveToCacheWithKeyVariations(cache, d));
    }
  }

  private void saveToCacheWithKeyVariations(
      Map<StringInsensitive, PythonDependency> cache, PythonDependency pythonDependency) {
    StringInsensitive stringInsensitive = new StringInsensitive(pythonDependency.getName());
    cache.put(stringInsensitive, pythonDependency);
    cache.putIfAbsent(
        new StringInsensitive(pythonDependency.getName().replace("-", "_")), pythonDependency);
    cache.putIfAbsent(
        new StringInsensitive(pythonDependency.getName().replace("_", "-")), pythonDependency);
  }
}
