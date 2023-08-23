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
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.redhat.exhort.Api;
import com.redhat.exhort.Provider;
import com.redhat.exhort.sbom.Sbom;
import com.redhat.exhort.sbom.SbomFactory;
import com.redhat.exhort.tools.Ecosystem.Type;
import com.redhat.exhort.tools.Operations;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Concrete implementation of the {@link Provider} used for converting
 * dependency trees
 * for npm projects (package.json) into a SBOM content for Stack analysis or
 * Component analysis.
 **/
public final class GoModulesProvider extends Provider {

  private final TreeMap goEnvironmentVariableForPurl;
  private final TreeMap goEnvironmentVariablesForRef;
  private String mainModuleVersion;

  public static void main(String[] args) {

    TreeMap qualifiers = GoModulesProvider.getQualifiers(true);
    Path path = Path.of("/tmp/tidy-test/go.mod");
    Provider provider = new GoModulesProvider();
    GoModulesProvider goProvider = (GoModulesProvider) provider;

      PackageURL purl = goProvider.toPurl("github.com/RHEcosystemAppEng/SaaSi/deployer", "@", goProvider.goEnvironmentVariableForPurl);
      System.out.println(purl.toString());
    try {
      provider.provideStack(path);
      byte[] bytes = Files.readAllBytes(path);
      provider.provideComponent(bytes);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public GoModulesProvider() {
    super(Type.GOLANG);
    this.goEnvironmentVariableForPurl=getQualifiers(true);
    this.goEnvironmentVariablesForRef =getQualifiers(false);
    this.mainModuleVersion="v0.0.0";
  }

  @Override
  public Content provideStack(final Path manifestPath) throws IOException {
    // check for custom npm executable
    Sbom sbom = getDependenciesSbom(manifestPath, true);
    return new Content(sbom.getAsJsonString().getBytes(StandardCharsets.UTF_8), Api.CYCLONEDX_MEDIA_TYPE);
  }

  @Override
  public Content provideComponent(byte[] manifestContent) throws IOException {
    // check for custom npm executable
    return new Content(getDependenciesSbomCa(manifestContent).getAsJsonString().getBytes(StandardCharsets.UTF_8),
      Api.CYCLONEDX_MEDIA_TYPE);
  }

  private Sbom getDependenciesSbomCa(byte[] manifestContent) {
    Sbom sbom;
    try {
      Path tempRepository = Files.createTempDirectory("exhort-go");
      Path path = Paths.get(tempRepository.toAbsolutePath().normalize().toString(), "go.mod");
      Files.deleteIfExists(path);
      Path manifestPath = Files.createFile(path);
      Files.write(manifestPath, manifestContent);
      sbom = getDependenciesSbom(manifestPath, false);

      Files.delete(manifestPath);
      Files.delete(tempRepository);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return sbom;
  }

  private PackageURL getRoot(String DependenciesGolang)  {
    return null;
  }

  private PackageURL toPurl(String dependency, String delimiter, TreeMap qualifiers) {
    try {
      int lastSlashIndex = dependency.lastIndexOf("/");
      //there is no '/' char in module/package, so there is not namespace, only name
      if (lastSlashIndex == -1)
      {
        String[] splitParts = dependency.split(delimiter);
        return new PackageURL(Type.GOLANG.getType(), null, splitParts[0], splitParts[1], qualifiers, null);

      }
      String namespace = dependency.substring(0, lastSlashIndex);
      String dependencyAndVersion = dependency.substring(lastSlashIndex + 1);
      String[] parts = dependencyAndVersion.split(delimiter);

        if (parts.length == 2) {
          return new PackageURL(Type.GOLANG.getType(), namespace, parts[0], parts[1], qualifiers, null);
        } else {
          return new PackageURL(Type.GOLANG.getType(), namespace, parts[0], this.mainModuleVersion, qualifiers, null);
        }
    } catch (MalformedPackageURLException e) {
      throw new IllegalArgumentException("Unable to parse golang module package : " + dependency , e);
    }

  }



  private Sbom getDependenciesSbom(Path manifestPath, boolean buildTree) throws IOException {
    var goModulesResult = buildGoModulesDependencies(manifestPath);
    Sbom sbom;
    if (!buildTree) {
      sbom = buildSbomFromList(goModulesResult);
    }
    else
    {
      sbom = buildSbomFromGraph(goModulesResult);
    }
//    sbom.filterIgnoredDeps(getIgnoredDeps(manifestPath));
    return sbom;
  }

  private Sbom buildSbomFromGraph(String goModulesResult) throws IOException{
//    Each entry contains a key of the module, and the list represents the module direct dependencies , so pairing of the key with each of the dependencies in a list is basically an edge in the graph.
    Map<String,List> edges = new HashMap<>();
    // iterate over go mod graph line by line and create map , with each entry to contain module as a key , and value of list of that module' dependencies.
    String[] lines = goModulesResult.split(System.lineSeparator());
    List<String> linesList = Arrays.asList(lines);
//    System.out.print("Start time: " + LocalDateTime.now() + System.lineSeparator());
    Integer startingIndex=0;
    Integer EndingIndex=lines.length - 1;
    String[] targetLines = Arrays.copyOfRange(lines,0,lines.length-1);
    for (String line : linesList) {

      if (!edges.containsKey(getParentVertex(line)))
      {
        //Collect all direct dependencies of the current module into a list.
        List<String> deps = collectAllDirectDependencies(targetLines, line);
        edges.put(getParentVertex(line),deps);
        startingIndex+=deps.size();
        // Because all the deps of the current module were collected, not need to search for next modules on these lines, so truncate these lines from search array to make the search more rapid and efficient.
        if(startingIndex < EndingIndex) {
          targetLines = Arrays.copyOfRange(lines, startingIndex, EndingIndex);
        }

      }
    }
//    Build Sbom
    String rootPackage = getParentVertex(lines[0]);

    PackageURL root = toPurl(rootPackage, "@", this.goEnvironmentVariableForPurl);
    Sbom sbom = SbomFactory.newInstance();
    sbom.addRoot(root);
    edges.forEach((key,value)-> {
       PackageURL source = toPurl(key,"@",this.goEnvironmentVariableForPurl);
       value.forEach(dep -> {
         PackageURL targetPurl = toPurl((String) dep, "@", this.goEnvironmentVariableForPurl);
         sbom.addDependency(source,targetPurl);
       });
    });
 return sbom;

  }

  private static List<String> collectAllDirectDependencies(String[] targetLines, String edge) {
    return Arrays.stream(targetLines)
                 .filter(line2 -> getParentVertex(line2)
                 .equals(getParentVertex(edge)))
                 .map(GoModulesProvider::getChildVertex)
                 .collect(Collectors.toList());
  }

  private static TreeMap getQualifiers(boolean includeOsAndArch) {

    if(includeOsAndArch)
    {
      var go = Operations.getCustomPathOrElse("go");
      String goEnvironmentVariables = Operations.runProcessGetOutput(null, new String[]{go, "env"});
      String hostArch = getEnvironmentVariable(goEnvironmentVariables,"GOHOSTARCH");
      int endOfLineIndex;
      int i;
      String hostOS = getEnvironmentVariable(goEnvironmentVariables,"GOHOSTOS");
      return new TreeMap(Map.of("type", "module","goos",hostOS,"goarch",hostArch));
    }

    return new TreeMap(Map.of("type", "module"));
  }

  private static String getEnvironmentVariable(String goEnvironmentVariables,String envName) {
    int i = goEnvironmentVariables.indexOf(String.format("%s=",envName));
    int beginIndex = i + String.format("%s=", envName).length();
    int endOfLineIndex = goEnvironmentVariables.substring(beginIndex).indexOf(System.lineSeparator());
    String envValue = goEnvironmentVariables.substring(beginIndex).substring(0, endOfLineIndex);
    return envValue.replaceAll("\"","");

  }

  private String buildGoModulesDependencies(Path manifestPath)
      throws JsonMappingException, JsonProcessingException {
    var go = Operations.getCustomPathOrElse("go");
    String[] goModulesDeps;
    goModulesDeps = new String[]{go, "mod",  "graph"};

    // execute the clean command
    String goModulesOutput = Operations.runProcessGetOutput(manifestPath.getParent(),goModulesDeps);
    return goModulesOutput;
  }

  private Sbom buildSbomFromList(String golangDeps) {
    String[] allModulesFlat = golangDeps.split(System.lineSeparator());
    String parentVertex = getParentVertex(allModulesFlat[0]);
    PackageURL root = toPurl(parentVertex,"@",this.goEnvironmentVariableForPurl);
    List<String> deps = collectAllDirectDependencies(allModulesFlat, parentVertex);

    Sbom sbom = SbomFactory.newInstance();
    sbom.addRoot(root);
    deps.forEach(dep -> {
      PackageURL targetPurl = toPurl(dep, "@", this.goEnvironmentVariableForPurl);
      sbom.addDependency(root,targetPurl);
    });
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

  private static String getParentVertex(String edge)
  {
    String[] edgeParts = edge.trim().split(" ");
    return edgeParts[0];
  }
  private static String getChildVertex(String edge)
  {

    String[] edgeParts = edge.trim().split(" ");
    return edgeParts[1];
  }

}
