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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.exhort.Api;
import com.redhat.exhort.ExhortTest;
import com.redhat.exhort.api.AnalysisReport;
import com.redhat.exhort.api.ProviderReport;
import com.redhat.exhort.providers.HelperExtension;
import com.redhat.exhort.tools.Ecosystem;
import com.redhat.exhort.tools.Operations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

@Tag("IntegrationTest")
@ExtendWith(HelperExtension.class)
@ExtendWith(MockitoExtension.class)
class ExhortApiIT extends ExhortTest {

  private static Api api;
  private static Map<String,String> ecoSystemsManifestNames;

  private MockedStatic<Operations> mockedOperations;
  @BeforeAll
  static void beforeAll() {
    api = new ExhortApi();
    System.setProperty("EXHORT_DEV_MODE","true");
    ecoSystemsManifestNames = Map.of("golang", "go.mod","maven","pom.xml","npm","package.json","pypi","requirements.txt");

  }

  @Tag("IntegrationTest")
  @AfterAll
  static void afterAll() {
    System.clearProperty("EXHORT_DEV_MODE");
    api = null;
  }
  @Tag("IntegrationTest")
  @ParameterizedTest
  @EnumSource(value = Ecosystem.Type.class, names = { "GOLANG", "MAVEN", "NPM", "PYTHON" })
  void Integration_Test_End_To_End_Stack_Analysis(Ecosystem.Type packageManager) throws IOException, ExecutionException, InterruptedException {
    String manifestFileName = ecoSystemsManifestNames.get(packageManager.getType());
    String pathToManifest = getFileFromResource(manifestFileName, "tst_manifests", "it", packageManager.getType(), manifestFileName);
    preparePythonEnvironment(packageManager);
    // Github action runner with all maven and java versions seems to enter infinite loop in integration tests of MAVEN when runnig dependency maven plugin to produce verbose text dependenct tree format.
    // locally it's not recreated with same versions
    mockMavenDependencyTree(packageManager);
    AnalysisReport analysisReportResult = api.stackAnalysis(pathToManifest).get();
    handleJsonResponse(analysisReportResult,true);
    releaseStaticMock(packageManager);
  }

  private void releaseStaticMock(Ecosystem.Type packageManager) {
    if(packageManager.equals(Ecosystem.Type.MAVEN)) {
      this.mockedOperations.close();
    }
  }


  @Tag("IntegrationTest")
  @ParameterizedTest
  @EnumSource(value = Ecosystem.Type.class, names = { "GOLANG", "MAVEN", "NPM", "PYTHON" })
  void Integration_Test_End_To_End_Stack_Analysis_Mixed(Ecosystem.Type packageManager) throws IOException, ExecutionException, InterruptedException {
    String manifestFileName = ecoSystemsManifestNames.get(packageManager.getType());
    String pathToManifest = getFileFromResource(manifestFileName, "tst_manifests", "it", packageManager.getType(), manifestFileName);
    preparePythonEnvironment(packageManager);
    // Github action runner with all maven and java versions seems to enter infinite loop in integration tests of MAVEN when runnig dependency maven plugin to produce verbose text dependenct tree format.
    // locally it's not recreated with same versions
    mockMavenDependencyTree(packageManager);
    AnalysisReport analysisReportJson = api.stackAnalysisMixed(pathToManifest).get().json;
    String analysisReportHtml = new String(api.stackAnalysisMixed(pathToManifest).get().html);
    handleJsonResponse(analysisReportJson,true);
    handleHtmlResponse(analysisReportHtml);
    releaseStaticMock(packageManager);
  }

  @Tag("IntegrationTest")
  @ParameterizedTest
  @EnumSource(value = Ecosystem.Type.class, names = { "GOLANG", "MAVEN", "NPM", "PYTHON" })
  void Integration_Test_End_To_End_Stack_Analysis_Html(Ecosystem.Type packageManager) throws IOException, ExecutionException, InterruptedException {
    String manifestFileName = ecoSystemsManifestNames.get(packageManager.getType());
    String pathToManifest = getFileFromResource(manifestFileName, "tst_manifests", "it", packageManager.getType(), manifestFileName);
    preparePythonEnvironment(packageManager);
    // Github action runner with all maven and java versions seems to enter infinite loop in integration tests of MAVEN when runnig dependency maven plugin to produce verbose text dependenct tree format.
    // locally it's not recreated with same versions
    mockMavenDependencyTree(packageManager);
    String analysisReportHtml = new String(api.stackAnalysisHtml(pathToManifest).get());
    releaseStaticMock(packageManager);
    handleHtmlResponse(analysisReportHtml);
  }


  @Tag("IntegrationTest")
  @ParameterizedTest
  @EnumSource(value = Ecosystem.Type.class, names = { "GOLANG", "MAVEN", "NPM", "PYTHON" })
  void Integration_Test_End_To_End_Component_Analysis(Ecosystem.Type packageManager) throws IOException, ExecutionException, InterruptedException {
    String manifestFileName = ecoSystemsManifestNames.get(packageManager.getType());
  byte[] manifestContent = getStringFromFile("tst_manifests", "it", packageManager.getType(), manifestFileName).getBytes();
    preparePythonEnvironment(packageManager);
    AnalysisReport analysisReportResult = api.componentAnalysis(manifestFileName,manifestContent).get();
    handleJsonResponse(analysisReportResult,false);
  }


  private static void preparePythonEnvironment(Ecosystem.Type packageManager) {
    if(packageManager.equals(Ecosystem.Type.PYTHON)) {
      System.setProperty("EXHORT_PYTHON_VIRTUAL_ENV","true");
      System.setProperty("EXHORT_PYTHON_INSTALL_BEST_EFFORTS","true");
      System.setProperty("MATCH_MANIFEST_VERSIONS","false");
    }
    else {
      System.clearProperty("EXHORT_PYTHON_VIRTUAL_ENV");
      System.clearProperty("EXHORT_PYTHON_INSTALL_BEST_EFFORTS");
      System.clearProperty("MATCH_MANIFEST_VERSIONS");
    }
  }

  private static void handleJsonResponse(AnalysisReport analysisReportResult, boolean positiveNumberOfTransitives) {
    analysisReportResult.getProviders().entrySet().stream().forEach(provider -> { assertTrue(provider.getValue().getStatus().getOk());
      assertTrue(provider.getValue().getStatus().getCode() == HttpURLConnection.HTTP_OK);
    });
    analysisReportResult.getProviders().entrySet().stream()
                                                  .map(Map.Entry::getValue)
                                                  .map(ProviderReport::getSources)
                                                  .map(Map::entrySet)
                                                  .flatMap(Collection::stream)
                                                  .map(Map.Entry::getValue)
                                                  .forEach( source -> assertTrue(source.getSummary().getTotal() > 0 ));

    if(positiveNumberOfTransitives) {
      assertTrue(analysisReportResult.getScanned().getTransitive() > 0);
    }
    else {
      assertEquals(0,analysisReportResult.getScanned().getTransitive());
    }
  }

  private void handleHtmlResponse(String analysisReportHtml) throws JsonProcessingException {
    ObjectMapper om = new ObjectMapper();
    assertTrue(analysisReportHtml.contains("svg") && analysisReportHtml.contains("html"));
    int jsonStart = analysisReportHtml.indexOf("\"report\":");
    int jsonEnd = analysisReportHtml.indexOf("}}}}}");
    String embeddedJson = analysisReportHtml.substring(jsonStart + 9 ,jsonEnd + 5);
    JsonNode jsonInHtml = om.readTree(embeddedJson);
    JsonNode scannedNode = jsonInHtml.get("scanned");
    assertTrue(scannedNode.get("total").asInt(0) > 0);
    assertTrue(scannedNode.get("transitive").asInt(0) > 0);
    JsonNode status = jsonInHtml.get("providers").get("snyk").get("status");
    assertTrue(status.get("code").asInt(0) == 200);
    assertTrue(status.get("ok").asBoolean(false));

  }
  private void mockMavenDependencyTree(Ecosystem.Type packageManager) throws IOException {
    if(packageManager.equals(Ecosystem.Type.MAVEN)) {
      mockedOperations = mockStatic(Operations.class);
      String depTree;
      try (var is =  getResourceAsStreamDecision(getClass(), new String [] { "tst_manifests", "it","maven", "depTree.txt"})) {
        depTree = new String(is.readAllBytes());
      }
      mockedOperations.when(() -> Operations.runProcess(any(),any())).thenAnswer(invocationOnMock ->  { return getOutputFileAndOverwriteItWithMock(depTree, invocationOnMock, "-DoutputFile");});
    }
  }

  public static String getOutputFileAndOverwriteItWithMock(String outputFileContent, InvocationOnMock invocationOnMock, String parameterPrefix) throws IOException {
    String[] rawArguments = (String[]) invocationOnMock.getRawArguments()[0];
    Optional<String> outputFileArg = Arrays.stream(rawArguments).filter(arg -> arg!= null && arg.startsWith(parameterPrefix)).findFirst();
    String outputFilePath=null;
    if(outputFileArg.isPresent())
    {
      String outputFile = outputFileArg.get();
      outputFilePath = outputFile.substring(outputFile.indexOf("=") + 1);
      Files.writeString(Path.of(outputFilePath), outputFileContent);
    }
    return outputFilePath;
  }

}



