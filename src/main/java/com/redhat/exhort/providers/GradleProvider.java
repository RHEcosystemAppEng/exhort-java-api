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

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.redhat.exhort.Api;
import com.redhat.exhort.Provider;
import com.redhat.exhort.logging.LoggersFactory;
import com.redhat.exhort.sbom.Sbom;
import com.redhat.exhort.sbom.SbomFactory;
import com.redhat.exhort.tools.Ecosystem.Type;
import com.redhat.exhort.tools.Operations;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.redhat.exhort.impl.ExhortApi.debugLoggingIsNeeded;

/**
 * Concrete implementation of the {@link Provider} used for converting dependency trees
 * for Gradle projects (gradle.build) into a content Dot Graphs for Stack analysis or Json for
 * Component analysis.
 **/
public final class GradleProvider extends BaseJavaProvider {

  private Logger log = LoggersFactory.getLogger(this.getClass().getName());

  public GradleProvider() {
    super(Type.GRADLE);
  }

  @Override
  public Content provideStack(final Path manifestPath) throws IOException {
    Path tempFile = getDependencies(manifestPath);
    if (debugLoggingIsNeeded()) {
      String stackAnalysisDependencyTree = Files.readString(tempFile);
      log.info(String.format("Package Manager Gradle Stack Analysis Dependency Tree Output: %s %s", System.lineSeparator(), stackAnalysisDependencyTree));
    }
    Map<String, String> propertiesMap = extractProperties(manifestPath);

    var sbom = buildSbomFromTextFormat(tempFile, propertiesMap, "runtimeClasspath");
    var ignored = getIgnoredDeps(manifestPath);

    return new Content(sbom.filterIgnoredDeps(ignored).getAsJsonString().getBytes(), Api.CYCLONEDX_MEDIA_TYPE);
  }

  private List<String> getIgnoredDeps(Path manifestPath) throws IOException {
    List<String> buildGradleLines = Files.readAllLines(manifestPath);
    List<String> ignored = new ArrayList<>();

    var ignoredLines = buildGradleLines.stream()
      .filter(this::isIgnoredLine)
      .map(this::extractPackageName)
      .collect(Collectors.toList());

    // Process each ignored dependency
    for (String dependency : ignoredLines) {
      String ignoredDepInfo;
      if (isNotation(dependency)) {
        ignoredDepInfo = getDepFromNotation(dependency, manifestPath);
      } else {
        ignoredDepInfo = getDepInfo(dependency);
      }

      if (ignoredDepInfo != null) {
        ignored.add(ignoredDepInfo);
      }
    }

    return ignored;
  }

  private String getDepInfo(String dependencyLine) {
    // Check if the line contains "group:", "name:", and "version:"
    if (dependencyLine.contains("group:") && dependencyLine.contains("name:") && dependencyLine.contains("version:")) {
      Pattern pattern = Pattern.compile("(group|name|version):\\s*['\"](.*?)['\"]");
      Matcher matcher = pattern.matcher(dependencyLine);
      String groupId = null, artifactId = null, version = null;

      while (matcher.find()) {
        String key = matcher.group(1);
        String value = matcher.group(2);

        switch (key) {
          case "group":
            groupId = value;
            break;
          case "name":
            artifactId = value;
            break;
          case "version":
            version = value;
            break;
        }
      }
        if (groupId != null && artifactId != null && version != null) {
          PackageURL ignoredPackageUrl = toPurl(groupId, artifactId, version);
          return ignoredPackageUrl.getCoordinates();
        }
    } else {
      // Regular expression pattern to capture content inside single or double quotes
      Pattern pattern = Pattern.compile("['\"](.*?)['\"]");
      Matcher matcher = pattern.matcher(dependencyLine);
      // Check if the matcher finds a match
      if (matcher.find()) {
        // Get the matched string inside single or double quotes
        String dependency = matcher.group(1);
        String[] dependencyParts = dependency.split(":");
        if (dependencyParts.length == 3) {
          // Extract groupId, artifactId, and version
          String groupId = dependencyParts[0];
          String artifactId = dependencyParts[1];
          String version = dependencyParts[2];

          PackageURL ignoredPackageUrl = toPurl(groupId, artifactId, version);
          return ignoredPackageUrl.getCoordinates();
        }
      }
    }
    return null;
  }

  private String getDepFromNotation(String dependency, Path manifestPath) throws IOException {
    // Extract everything after "libs."
    String alias = dependency.substring(dependency.indexOf("libs.") + "libs.".length());
    alias = alias.replace(".", "-");
    // Read and parse the TOML file
    TomlParseResult toml = Toml.parse(getLibsVersionsTomlPath(manifestPath));
    TomlTable librariesTable = toml.getTable("libraries");
    TomlTable dependencyTable = librariesTable.getTable(alias);
    if (dependencyTable != null) {
      String groupId = dependencyTable.getString("module").split(":")[0];
      String artifactId = dependencyTable.getString("module").split(":")[1];
      String version = toml.getTable("versions").getString(dependencyTable.getString("version.ref"));
      PackageURL ignoredPackageUrl = toPurl(groupId, artifactId, version);
      return ignoredPackageUrl.getCoordinates();
    }

    return null;

  }

  private Path getLibsVersionsTomlPath(Path manifestPath) {
    return manifestPath.getParent().resolve("gradle/libs.versions.toml");
  }

  public PackageURL toPurl(String groupId, String artifactId, String version) {
    try {
      return new PackageURL(Type.MAVEN.getType(), groupId, artifactId, version, null, null);
    } catch (MalformedPackageURLException e) {
      throw new IllegalArgumentException("Unable to parse PackageURL", e);
    }
  }

  public static boolean isNotation(String line) {
    int colonCount = 0;
    for (char c : line.toCharArray()) {
      if (c == ':') {
        colonCount++;
        if (colonCount > 1) {
          return false; // Likely full dependency with group and artifact
        }
      }
    }
    return true; // Potentially a notation
  }

  private boolean isIgnoredLine(String line) {
    return line.contains("exhortignore");
  }

  private String extractPackageName(String line) {
    String packageName = line.trim();
    // Extract the package name before the comment
    int commentIndex = packageName.indexOf("//");
    if (commentIndex != -1) {
      packageName = packageName.substring(0, commentIndex).trim();
    }
    // Remove any other trailing comments or spaces
    commentIndex = packageName.indexOf("/*");
    if (commentIndex != -1) {
      packageName = packageName.substring(0, commentIndex).trim();
    }
    return packageName;
  }


  private static Path getDependencies(Path manifestPath) throws IOException {
    // check for custom gradle executable
    var gradle = Operations.getCustomPathOrElse("gradle");
    // create a temp file for storing the dependency tree in
    var tempFile = Files.createTempFile("exhort_graph_", null);
    // the command will create the dependency tree in the temp file
    String gradleCommand = gradle + " dependencies";

    String[] cmdList = gradleCommand.split("\\s+");
    String gradleOutput = Operations.runProcessGetOutput(Path.of(manifestPath.getParent().toString()), cmdList);
    Files.writeString(tempFile, gradleOutput);

    return tempFile;
  }

  private Path getProperties(Path manifestPath) throws IOException {
    Path propsTempFile = Files.createTempFile("propsfile", ".txt");
    var gradle = Operations.getCustomPathOrElse("gradle");
    String propCmd = gradle + " properties";
    String[] propCmdList = propCmd.split("\\s+");
    String properties = Operations.runProcessGetOutput(Path.of(manifestPath.getParent().toString()), propCmdList);
    // Create a temporary file
    Files.writeString(propsTempFile, properties);

    return propsTempFile;
  }

  private Sbom buildSbomFromTextFormat(Path textFormatFile, Map<String, String> propertiesMap, String configName) throws IOException {
    var sbom = SbomFactory.newInstance(Sbom.BelongingCondition.PURL, "sensitive");
    String root = getRoot(textFormatFile, propertiesMap);

    var rootPurl = parseDep(root);
    sbom.addRoot(rootPurl);
    List<String> lines = extractLines(textFormatFile, configName);
    String[] array = new String[lines.size()];
    for (int index = 0; index < array.length; index++) {
      String line = lines.get(index);
      line = line.replaceAll("---", "-").replaceAll("    ", "  ");
      line = line.replaceAll(":(.*):(.*) -> (.*)$", ":$1:$3");
      line = line.replaceAll("(.*):(.*):(.*)$", "$1:$2:jar:$3");
      line = line.replaceAll(" \\(n\\)$", "");
      line = line.replaceAll("$", ":compile");
      array[index] = line;
    }
    parseDependencyTree(root, 0, array, sbom);
    return sbom;
  }

  private String getRoot(Path textFormatFile, Map<String, String> propertiesMap) throws IOException {
    String group = propertiesMap.get("group");
    String version = propertiesMap.get("version");
    String rootName = extractRootProjectValue(textFormatFile);
    String root = group + ':' + rootName + ':' + "jar" + ':' + version ;
    return root;
  }

  private String extractRootProjectValue(Path inputFilePath) throws IOException {
    List<String> lines = Files.readAllLines(inputFilePath);
    for (String line : lines) {
      if (line.contains("Root project")) {
        Pattern pattern = Pattern.compile("Root project '(.+)'");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
          return matcher.group(1);
        }
      }
    }
    return null;
  }

  private Map<String, String> extractProperties(Path manifestPath) throws IOException {
    Path propsTempFile = getProperties(manifestPath);
    String content = Files.readString(propsTempFile);
    // Define the regular expression pattern for key-value pairs
    Pattern pattern = Pattern.compile("([^:]+):\\s+(.+)");
    Matcher matcher = pattern.matcher(content);
    // Create a Map to store key-value pairs
    Map<String, String> keyValueMap = new HashMap<>();

    // Iterate through matches and add them to the map
    while (matcher.find()) {
      String key = matcher.group(1).trim();
      String value = matcher.group(2).trim();
      keyValueMap.put(key, value);
    }
    // Check if any key-value pairs were found
    if (!keyValueMap.isEmpty()) {
      return keyValueMap;
    } else {
      return null;
    }
  }

  private List<String> extractLines(Path inputFilePath, String startMarker) throws IOException {
    List<String> lines = Files.readAllLines(inputFilePath);
    List<String> extractedLines = new ArrayList<>();
    boolean startFound = false;

    for (String line : lines) {
      // If the start marker is found, set startFound to true
      if (line.startsWith(startMarker)) {
        startFound = true;
        continue; // Skip the line containing the startMarker
      }
      // If startFound is true and the line is not empty, add it to the extractedLines list
      if (startFound && !line.trim().isEmpty()) {
        extractedLines.add(line);
      }
      // If an empty line is encountered, break out of the loop
      if (startFound && line.trim().isEmpty()) {
        break;
      }
    }
    return extractedLines;
  }

  @Override
  public Content provideComponent(byte[] manifestContent) throws IOException {
    throw new IllegalArgumentException("Gradle Package Manager requires the full package directory, not just the manifest content, to generate the dependency tree. Please provide the complete package directory path.");
  }

  @Override
  public Content provideComponent(Path manifestPath) throws IOException {

    Path tempFile = getDependencies(manifestPath);
    Map<String, String> propertiesMap = extractProperties(manifestPath);

    String[] configurationNames = {"api", "implementation", "compile"};

    String configName = null;
    for (String configurationName : configurationNames) {
      List<String> directDependencies = extractLines(tempFile, configurationName);

      // Check if dependencies are found for the current configuration
      if (!directDependencies.isEmpty()) {
        configName = configurationName;
        break;
      }
    }

    var sbom = buildSbomFromTextFormat(tempFile, propertiesMap, configName);
    var ignored = getIgnoredDeps(manifestPath);

    return new Content(sbom.filterIgnoredDeps(ignored).getAsJsonString().getBytes(), Api.CYCLONEDX_MEDIA_TYPE);
  }
}
