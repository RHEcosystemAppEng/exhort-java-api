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
package com.redhat.exhort.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.exhort.Api;
import com.redhat.exhort.ExhortTest;
import com.redhat.exhort.api.AnalysisReport;
import com.redhat.exhort.api.ProviderReport;
import com.redhat.exhort.image.ImageRef;
import com.redhat.exhort.providers.HelperExtension;
import com.redhat.exhort.tools.Ecosystem;
import com.redhat.exhort.tools.Operations;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("IntegrationTest")
@ExtendWith(HelperExtension.class)
@ExtendWith(MockitoExtension.class)
class ExhortApiIT extends ExhortTest {

  private static Api api;
  private static Map<String, String> ecoSystemsManifestNames;

  private MockedStatic<Operations> mockedOperations;

  @BeforeAll
  static void beforeAll() {
    api = new ExhortApi();
    System.setProperty("RHDA_SOURCE", "exhort-java-api-it");
    System.setProperty("EXHORT_DEV_MODE", "false");
    ecoSystemsManifestNames =
        Map.of(
            "golang",
            "go.mod",
            "maven",
            "pom.xml",
            "npm",
            "package.json",
            "pypi",
            "requirements.txt",
            "gradle-groovy",
            "build.gradle",
            "gradle-kotlin",
            "build.gradle.kts");
  }

  @Tag("IntegrationTest")
  @AfterAll
  static void afterAll() {
    System.clearProperty("RHDA_SOURCE");
    System.clearProperty("EXHORT_DEV_MODE");
    api = null;
  }

  private static List<Arguments> scenarios() {
    return ecoSystemsManifestNames.entrySet().stream()
        .map(e -> Arguments.of(e.getKey(), e.getValue()))
        .collect(Collectors.toList());
  }

  @Tag("IntegrationTest")
  @ParameterizedTest(name = "StackAnalysis for: {0} with manifest: {1}")
  @MethodSource("scenarios")
  void Integration_Test_End_To_End_Stack_Analysis(String useCase, String manifestFileName)
      throws IOException, ExecutionException, InterruptedException {
    var provider = Ecosystem.getProvider(manifestFileName);
    var packageManager = provider.ecosystem;
    String pathToManifest =
        getFileFromResource(manifestFileName, "tst_manifests", "it", useCase, manifestFileName);
    preparePythonEnvironment(packageManager);
    // Github action runner with all maven and java versions seems to enter infinite loop in
    // integration tests of
    // MAVEN when runnig dependency maven plugin to produce verbose text dependenct tree format.
    // locally it's not recreated with same versions
    mockMavenDependencyTree(packageManager);
    AnalysisReport analysisReportResult = api.stackAnalysis(pathToManifest).get();
    handleJsonResponse(analysisReportResult, true);
    releaseStaticMock(packageManager);
  }

  private void releaseStaticMock(Ecosystem.Type packageManager) {
    if (packageManager.equals(Ecosystem.Type.MAVEN)) {
      this.mockedOperations.close();
    }
  }

  @Tag("IntegrationTest")
  @ParameterizedTest(name = "StackAnalysis Mixed for: {0} with manifest: {1}")
  @MethodSource("scenarios")
  void Integration_Test_End_To_End_Stack_Analysis_Mixed(String useCase, String manifestFileName)
      throws IOException, ExecutionException, InterruptedException {
    var provider = Ecosystem.getProvider(manifestFileName);
    var packageManager = provider.ecosystem;
    String pathToManifest =
        getFileFromResource(manifestFileName, "tst_manifests", "it", useCase, manifestFileName);
    preparePythonEnvironment(packageManager);
    // Github action runner with all maven and java versions seems to enter infinite loop in
    // integration tests of
    // MAVEN when runnig dependency maven plugin to produce verbose text dependenct tree format.
    // locally it's not recreated with same versions
    mockMavenDependencyTree(packageManager);
    AnalysisReport analysisReportJson = api.stackAnalysisMixed(pathToManifest).get().json;
    String analysisReportHtml = new String(api.stackAnalysisMixed(pathToManifest).get().html);
    handleJsonResponse(analysisReportJson, true);
    handleHtmlResponse(analysisReportHtml);
    releaseStaticMock(packageManager);
  }

  @Tag("IntegrationTest")
  @ParameterizedTest(name = "StackAnalysis HTML for: {0} with manifest: {1}")
  @MethodSource("scenarios")
  void Integration_Test_End_To_End_Stack_Analysis_Html(String useCase, String manifestFileName)
      throws IOException, ExecutionException, InterruptedException {
    var provider = Ecosystem.getProvider(manifestFileName);
    var packageManager = provider.ecosystem;
    String pathToManifest =
        getFileFromResource(manifestFileName, "tst_manifests", "it", useCase, manifestFileName);
    preparePythonEnvironment(packageManager);
    // Github action runner with all maven and java versions seems to enter infinite loop in
    // integration tests of
    // MAVEN when running dependency maven plugin to produce verbose text dependenct tree format.
    // locally it's not recreated with same versions
    mockMavenDependencyTree(packageManager);
    String analysisReportHtml = new String(api.stackAnalysisHtml(pathToManifest).get());
    releaseStaticMock(packageManager);
    handleHtmlResponse(analysisReportHtml);
  }

  @Tag("IntegrationTest")
  @ParameterizedTest
  @EnumSource(
      value = Ecosystem.Type.class,
      names = {"GOLANG", "MAVEN", "NPM", "PYTHON"})
  void Integration_Test_End_To_End_Component_Analysis(Ecosystem.Type packageManager)
      throws IOException, ExecutionException, InterruptedException {
    String manifestFileName = ecoSystemsManifestNames.get(packageManager.getType());
    byte[] manifestContent =
        getStringFromFile("tst_manifests", "it", packageManager.getType(), manifestFileName)
            .getBytes();
    preparePythonEnvironment(packageManager);
    AnalysisReport analysisReportResult =
        api.componentAnalysis(manifestFileName, manifestContent).get();
    handleJsonResponse(analysisReportResult, false);
  }

  @Tag("IntegrationTest")
  @Test
  void Integration_Test_End_To_End_Image_Analysis() throws IOException {
    var result =
        testImageAnalysis(
            i -> {
              try {
                return api.imageAnalysis(i).get();
              } catch (InterruptedException | ExecutionException | IOException e) {
                throw new RuntimeException(e);
              }
            });

    assertEquals(1, result.size());
    handleJsonResponse(new ArrayList<>(result.values()).get(0), false);
  }

  @Tag("IntegrationTest")
  @Test
  void Integration_Test_End_To_End_Image_Analysis_Html() throws IOException {
    var result =
        testImageAnalysis(
            i -> {
              try {
                return api.imageAnalysisHtml(i).get();
              } catch (InterruptedException | ExecutionException | IOException e) {
                throw new RuntimeException(e);
              }
            });

    handleHtmlResponseForImage(new String(result));
  }

  private static <T> T testImageAnalysis(Function<Set<ImageRef>, T> imageAnalysisFunction)
      throws IOException {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class);
        var sbomIS =
            getResourceAsStreamDecision(
                ExhortApiIT.class, new String[] {"msc", "image", "image_sbom.json"})) {

      var imageRef =
          new ImageRef(
              "test.io/test/test-app:test-version@sha256:1fafb0905264413501df60d90a92ca32df8a2011cbfb4876ddff5ceb20c8f165",
              "linux/amd64");

      var jsonSbom =
          new BufferedReader(new InputStreamReader(sbomIS, StandardCharsets.UTF_8))
              .lines()
              .collect(Collectors.joining("\n"));
      var output = new Operations.ProcessExecOutput(jsonSbom, "", 0);

      mock.when(() -> Operations.getCustomPathOrElse(eq("syft"))).thenReturn("syft");

      mock.when(
              () ->
                  Operations.runProcessGetFullOutput(
                      isNull(),
                      aryEq(
                          new String[] {
                            "syft",
                            imageRef.getImage().getFullName(),
                            "-s",
                            "all-layers",
                            "-o",
                            "cyclonedx-json",
                            "-q"
                          }),
                      isNull()))
          .thenReturn(output);

      return imageAnalysisFunction.apply(Set.of(imageRef));
    }
  }

  private static void preparePythonEnvironment(Ecosystem.Type packageManager) {
    if (packageManager.equals(Ecosystem.Type.PYTHON)) {
      System.setProperty("EXHORT_PYTHON_VIRTUAL_ENV", "true");
      System.setProperty("EXHORT_PYTHON_INSTALL_BEST_EFFORTS", "true");
      System.setProperty("MATCH_MANIFEST_VERSIONS", "false");
    } else {
      System.clearProperty("EXHORT_PYTHON_VIRTUAL_ENV");
      System.clearProperty("EXHORT_PYTHON_INSTALL_BEST_EFFORTS");
      System.clearProperty("MATCH_MANIFEST_VERSIONS");
    }
  }

  private static void handleJsonResponse(
      AnalysisReport analysisReportResult, boolean positiveNumberOfTransitives) {
    analysisReportResult.getProviders().entrySet().stream()
        .forEach(
            provider -> {
              assertTrue(provider.getValue().getStatus().getOk());
              assertTrue(provider.getValue().getStatus().getCode() == HttpURLConnection.HTTP_OK);
            });
    analysisReportResult.getProviders().entrySet().stream()
        .map(Map.Entry::getValue)
        .map(ProviderReport::getSources)
        .map(Map::entrySet)
        .flatMap(Collection::stream)
        .map(Map.Entry::getValue)
        .forEach(source -> assertTrue(source.getSummary().getTotal() > 0));

    if (positiveNumberOfTransitives) {
      assertTrue(analysisReportResult.getScanned().getTransitive() > 0);
    } else {
      assertEquals(0, analysisReportResult.getScanned().getTransitive());
    }
  }

  private void handleHtmlResponse(String analysisReportHtml) throws JsonProcessingException {
    ObjectMapper om = new ObjectMapper();
    assertTrue(analysisReportHtml.contains("svg") && analysisReportHtml.contains("html"));
  }

  private void handleHtmlResponseForImage(String analysisReportHtml)
      throws JsonProcessingException {
    ObjectMapper om = new ObjectMapper();
    assertTrue(analysisReportHtml.contains("svg") && analysisReportHtml.contains("html"));
  }

  private void mockMavenDependencyTree(Ecosystem.Type packageManager) throws IOException {
    if (packageManager.equals(Ecosystem.Type.MAVEN)) {
      mockedOperations = mockStatic(Operations.class);
      String depTree;
      try (var is =
          getResourceAsStreamDecision(
              getClass(), new String[] {"tst_manifests", "it", "maven", "depTree.txt"})) {
        depTree = new String(is.readAllBytes());
      }
      mockedOperations
          .when(() -> Operations.runProcess(any(), any()))
          .thenAnswer(
              invocationOnMock -> {
                return getOutputFileAndOverwriteItWithMock(
                    depTree, invocationOnMock, "-DoutputFile");
              });
    }
  }

  public static String getOutputFileAndOverwriteItWithMock(
      String outputFileContent, InvocationOnMock invocationOnMock, String parameterPrefix)
      throws IOException {
    String[] rawArguments = (String[]) invocationOnMock.getRawArguments()[0];
    Optional<String> outputFileArg =
        Arrays.stream(rawArguments)
            .filter(arg -> arg != null && arg.startsWith(parameterPrefix))
            .findFirst();
    String outputFilePath = null;
    if (outputFileArg.isPresent()) {
      String outputFile = outputFileArg.get();
      outputFilePath = outputFile.substring(outputFile.indexOf("=") + 1);
      Files.writeString(Path.of(outputFilePath), outputFileContent);
    }
    return outputFilePath;
  }
}
