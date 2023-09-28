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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.exhort.exception.PackageNotInstalledException;
import com.redhat.exhort.tools.Operations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.join;

public abstract class PythonControllerBase {
  public static void main(String[] args) {

    PythonControllerBase pythonController;
//    pythonController = new PythonControllerVirtualEnv("/usr/bin/python3");
    LocalDateTime start = LocalDateTime.now();
    List<Map<String, Object>> dependencies;
//    dependencies = pythonController.getDependencies("/tmp/requirements.txt",true);
    LocalDateTime end = LocalDateTime.now();
    System.out.println("start time:" + start + "\n");
    System.out.println("end time:" + end  + "\n");
    System.out.println("elapsed time: " + start.until(end, ChronoUnit.SECONDS)  + "\n" );
    pythonController = new PythonControllerRealEnv("/usr/bin/python3","/usr/bin/pip3");
    start = LocalDateTime.now();
    try {
      dependencies = pythonController.getDependencies("/home/zgrinber/git/exhort-java-api/src/test/resources/tst_manifests/pip/pip_requirements_txt_ignore/requirements.txt",true);
    } catch (PackageNotInstalledException e) {
      System.out.println(e.getMessage());
      dependencies =null;
    }
    end = LocalDateTime.now();
//    LocalDateTime startNaive = LocalDateTime.now();
//    List<Map<String, Object>> dependenciesNaive = pythonController.getDependenciesNaive();
//    LocalDateTime endNaive = LocalDateTime.now();
    System.out.println("start time:" + start + "\n");
    System.out.println("end time:" + end  + "\n");
    System.out.println("elapsed time: " + start.until(end, ChronoUnit.SECONDS)  + "\n" );
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
  private System.Logger log = System.getLogger("name");
  protected Path pythonEnvironmentDir;
  protected Path pipBinaryDir;

  protected String pathToPythonBin;

  protected String pipBinaryLocation;

//  public int counter =0;

  public abstract void prepareEnvironment(String pathToPythonBin);

  public abstract boolean automaticallyInstallPackageOnEnvironment();

  public abstract boolean isRealEnv();

  public void installPackage(String pathToRequirements)
  {
    log.log(System.Logger.Level.INFO,"installPackage -> pip Binary location=" + pipBinaryLocation + ",pythonEnvironmentDir=" + pythonEnvironmentDir);
    String installedPackageSummary = Operations.runProcessGetOutput(pythonEnvironmentDir, pipBinaryLocation, "install", "-r", pathToRequirements);
    log.log(System.Logger.Level.INFO,installedPackageSummary);
    String freeze = Operations.runProcessGetOutput(pythonEnvironmentDir, pipBinaryLocation, "freeze");
    log.log(System.Logger.Level.INFO,"freeze output=" + System.lineSeparator() + freeze);

  }

  public abstract boolean isVirtualEnv();

  public abstract void cleanEnvironment(boolean deleteEnvironment);


//  public List<Map<String,Object>> getDependenciesNaive()
//  {
//    List<Map<String,Object>> dependencies = new ArrayList<>();
//    String freeze = Operations.runProcessGetOutput(pythonEnvironmentDir, pipBinaryLocation, "freeze");
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
//    String pipShowOutput = Operations.runProcessGetOutput(pythonEnvironmentDir, pipBinaryLocation, "show", depName);
//    LocalDateTime end = LocalDateTime.now();
//    System.out.println("pip show start time:" + start + "\n");
//    System.out.println("pip show end time:" + end  + "\n");
//    System.out.println("pip show elapsed time: " + start.until(end, ChronoUnit.SECONDS)  + "\n" );
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
//    String freeze = Operations.runProcessGetOutput(pythonEnvironmentDir, pipBinaryLocation, "freeze");
//    String[] deps = freeze.split(System.lineSeparator());
//    String depNames = Arrays.stream(deps).map(this::getDependencyName).collect(Collectors.joining(" "));
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
//    String pipShowOutput = Operations.runProcessGetOutput(pythonEnvironmentDir, pipBinaryLocation, "show", depName);
//    LocalDateTime end = LocalDateTime.now();
//    System.out.println("pip show start time:" + start + "\n");
//    System.out.println("pip show end time:" + end  + "\n");
//    System.out.println("pip show elapsed time: " + start.until(end, ChronoUnit.MILLIS)  + "\n" );
//    List<String> allLines = Arrays.stream(pipShowOutput.split("---")).collect(Collectors.toList());
//    allLines.stream().forEach(record -> {
//      String depVersion = getDependencyVersion(record);
//      List<String> directDeps = getDepsList(record);
//      getDependencyNameShow(record);
//      Map<String, Object> entry = new HashMap<String,Object>();
//      dependencies.add(entry);
//      entry.put("name", getDependencyNameShow(record));
//      entry.put("version", depVersion);
//      List<Map<String, Object>> targetDeps = new ArrayList<>();
//      String depsList = directDeps.stream().map(str -> str.replace(",", "")).collect(Collectors.joining(" "));
//      bringAllDependencies(targetDeps, depsList);
//      entry.put("dependencies",targetDeps);
//    });
//  }

  public final List<Map<String,Object>> getDependencies(String pathToRequirements, boolean includeTransitive) {
    if(isVirtualEnv() || isRealEnv() ) {
      prepareEnvironment(pathToPythonBin);
    }
    if(automaticallyInstallPackageOnEnvironment())
    {
      installPackage(pathToRequirements);
    }
    List<Map<String, Object>> dependencies = getDependenciesImpl(pathToRequirements, includeTransitive);
    if(isVirtualEnv())
    {
      cleanEnvironment(false);
    }

    return dependencies;
  }

  private List<Map<String, Object>> getDependenciesImpl(String pathToRequirements, boolean includeTransitive) {
    List<Map<String,Object>> dependencies = new ArrayList<>();
    log.log(System.Logger.Level.INFO,"getDependenciesImpl -> pip Binary location=" + pipBinaryLocation + ",pythonEnvironmentDir=" + pythonEnvironmentDir);
    String freeze = Operations.runProcessGetOutput(pythonEnvironmentDir, pipBinaryLocation, "freeze");
    log.log(System.Logger.Level.INFO,"freeze output=" + System.lineSeparator() + freeze);
    String[] deps = freeze.split(System.lineSeparator());
    log.log(System.Logger.Level.INFO,"Size of list of installed deps=" + deps.length);
    Arrays.stream(deps).forEach( s -> {
      log.log(System.Logger.Level.INFO,"Entry -->" + s + System.lineSeparator());
    });
    String depNames = Arrays.stream(deps).map(PythonControllerBase::getDependencyName).collect(Collectors.joining(" "));
    String pipShowOutput = Operations.runProcessGetOutput(pythonEnvironmentDir, pipBinaryLocation, "show", depNames);
    List<String> allPipShowLines = Arrays.stream(pipShowOutput.split("---")).collect(Collectors.toList());
    Map<StringInsensitive,String> CachedTree = new HashMap<>();
    List<String> linesOfRequirements;
    try {

      linesOfRequirements = Files.readAllLines(Path.of(pathToRequirements)).stream().filter( (line) -> !line.startsWith("#")).map(String::trim).collect(Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    allPipShowLines.stream().forEach(record -> {
      String dependencyNameShow = getDependencyNameShow(record);
      StringInsensitive stringInsensitive = new StringInsensitive(dependencyNameShow);
      CachedTree.put(stringInsensitive,record);
      CachedTree.putIfAbsent(new StringInsensitive(dependencyNameShow.replace("-","_")),record);
      CachedTree.putIfAbsent(new StringInsensitive(dependencyNameShow.replace("_","-")),record);
    });
    ObjectMapper om = new ObjectMapper();
    String tree;
    try {
      tree = om.writerWithDefaultPrettyPrinter().writeValueAsString(CachedTree);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    log.log(System.Logger.Level.INFO,"Installed Dependencies:" + System.lineSeparator() + tree);
    linesOfRequirements.stream().forEach( dep -> {
      bringAllDependencies(dependencies, getDependencyName(dep),CachedTree, includeTransitive);
    });

    return dependencies;
  }

  private void bringAllDependencies(List<Map<String, Object>> dependencies, String depName, Map<StringInsensitive, String> cachedTree, boolean includeTransitive) {

    if (dependencies == null || depName.trim().equals(""))
      return;

    String record = cachedTree.get(new StringInsensitive(depName));
    if(record == null)
    {
      throw new PackageNotInstalledException(String.format("Package name=>%s is not installed on your python environment, either install it ( better to install requirements.txt altogether) or turn on environment variable EXHORT_PYTHON_VIRTUAL_ENV=true to automatically installs it on virtual environment ( will slow down the analysis)",depName));
    }
    String depVersion = getDependencyVersion(record);
    List<String> directDeps = getDepsList(record);
    getDependencyNameShow(record);
    Map<String, Object> entry = new HashMap<String,Object>();
    dependencies.add(entry);
    entry.put("name", getDependencyNameShow(record));
    entry.put("version", depVersion);
    List<Map<String, Object>> targetDeps = new ArrayList<>();
    directDeps.stream().forEach( dep -> {
      if(includeTransitive) {
        bringAllDependencies(targetDeps, dep, cachedTree, includeTransitive);
      }
      Collections.sort(targetDeps, (o1, o2) -> {
        String string1 = (String) (o1.get("name"));
        String string2 = (String) (o2.get("name"));
        return Arrays.compare(string1.toCharArray(),string2.toCharArray());
      });
      entry.put("dependencies",targetDeps);
    });


  }

  private List getDepsList(String pipShowOutput) {
    int requiresKeyIndex = pipShowOutput.indexOf("Requires:");
    String requiresToken = pipShowOutput.substring(requiresKeyIndex + 9);
    int endOfLine = requiresToken.indexOf(System.lineSeparator());
    String listOfDeps = requiresToken.substring(0, endOfLine).trim();
    return Arrays.stream(listOfDeps.split(",")).map(String::trim).filter(dep -> !dep.equals("")).collect(Collectors.toList());
  }

  private String getDependencyVersion(String pipShowOutput) {
    int versionKeyIndex = pipShowOutput.indexOf("Version:");
    String versionToken = pipShowOutput.substring(versionKeyIndex + 8);
    int endOfLine = versionToken.indexOf(System.lineSeparator());
    return versionToken.substring(0,endOfLine).trim();
  }

  private String getDependencyNameShow(String pipShowOutput) {
    int versionKeyIndex = pipShowOutput.indexOf("Name:");
    String versionToken = pipShowOutput.substring(versionKeyIndex + 5);
    int endOfLine = versionToken.indexOf(System.lineSeparator());
    return versionToken.substring(0,endOfLine).trim();
  }

  public static String getDependencyName(String dep) {
    int rightTriangleBracket = dep.indexOf(">");
    int leftTriangleBracket = dep.indexOf("<");
    int equalsSign= dep.indexOf("=");
    int minimumIndex = getFirstSign(rightTriangleBracket,leftTriangleBracket,equalsSign);
    String depName;
    if(rightTriangleBracket == -1 && leftTriangleBracket == -1 &&  equalsSign == -1)
    {
      depName = dep;
    }
    else {

      depName = dep.substring(0, minimumIndex);
    }
    return depName.trim();
  }

  private  static int getFirstSign(int rightTriangleBracket, int leftTriangleBracket, int equalsSign) {
        rightTriangleBracket = rightTriangleBracket == -1 ? 999 : rightTriangleBracket;
        leftTriangleBracket = leftTriangleBracket == -1 ? 999 : leftTriangleBracket;
        equalsSign = equalsSign == -1 ? 999 : equalsSign;
        return equalsSign < leftTriangleBracket && equalsSign < rightTriangleBracket ? equalsSign : (leftTriangleBracket < equalsSign && leftTriangleBracket < rightTriangleBracket ? leftTriangleBracket : rightTriangleBracket);
  }
}
