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

import com.redhat.exhort.Api;
import com.redhat.exhort.Provider;
import com.redhat.exhort.logging.LoggersFactory;
import com.redhat.exhort.sbom.Sbom;
import com.redhat.exhort.sbom.SbomFactory;
import com.redhat.exhort.tools.Ecosystem.Type;
import com.redhat.exhort.tools.Operations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
      log.info(String.format("Package Manager Maven Stack Analysis Dependency Tree Output: %s %s", System.lineSeparator(), stackAnalysisDependencyTree));
    }
    Map<String, String> propertiesMap = extractProperties(manifestPath);

    var sbom = buildSbomFromTextFormat(tempFile, propertiesMap, "runtimeClasspath");
    return new Content(sbom.getAsJsonString().getBytes(), Api.CYCLONEDX_MEDIA_TYPE);
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
    return new Content(sbom.getAsJsonString().getBytes(), Api.CYCLONEDX_MEDIA_TYPE);
  }
}
