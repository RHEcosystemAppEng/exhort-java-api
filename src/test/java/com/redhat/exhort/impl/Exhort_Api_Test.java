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

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.redhat.exhort.Api;
import com.redhat.exhort.ExhortTest;
import com.redhat.exhort.Provider;
import com.redhat.exhort.api.AnalysisReport;
import com.redhat.exhort.image.ImageRef;
import com.redhat.exhort.tools.Ecosystem;
import com.redhat.exhort.tools.Operations;

@ExtendWith(MockitoExtension.class)
@ClearEnvironmentVariable(key="EXHORT_SNYK_TOKEN")
@ClearEnvironmentVariable(key="EXHORT_DEV_MODE")
@ClearEnvironmentVariable(key="DEV_EXHORT_BACKEND_URL")
@ClearEnvironmentVariable(key="RHDA_TOKEN")
@ClearEnvironmentVariable(key="RHDA_SOURCE")
@SuppressWarnings("unchecked")
class Exhort_Api_Test extends ExhortTest {
  @Mock
  Provider mockProvider;
  @Mock
  HttpClient mockHttpClient;
  @InjectMocks
  ExhortApi exhortApiSut;

  @AfterEach
  void cleanup() {
    System.clearProperty("EXHORT_SNYK_TOKEN");
  }

  @Test
  @SetEnvironmentVariable(key="EXHORT_SNYK_TOKEN", value="snyk-token-from-env-var")
  @SetEnvironmentVariable(key="RHDA_TOKEN", value="rhda-token-from-env-var")
  @SetEnvironmentVariable(key="RHDA_SOURCE", value="rhda-source-from-env-var")
  void stackAnalysisHtml_with_pom_xml_should_return_html_report_from_the_backend()
      throws IOException, ExecutionException, InterruptedException {
    // create a temporary pom.xml file
    var tmpFile = Files.createTempFile("exhort_test_pom_", ".xml");
    try (var is = getResourceAsStreamDecision(this.getClass(), new String [] {"tst_manifests","maven","empty","pom.xml"})) {
      Files.write(tmpFile, is.readAllBytes());
    }

    // stub the mocked provider with a fake content object
    given(mockProvider.provideStack(tmpFile))
      .willReturn(new Provider.Content("fake-body-content".getBytes(), "fake-content-type"));

    // create an argument matcher to make sure we mock the response to for right request
    ArgumentMatcher<HttpRequest> matchesRequest = r ->
      r.headers().firstValue("Content-Type").get().equals("fake-content-type") &&
      r.headers().firstValue("Accept").get().equals("text/html") &&
      // snyk token is set using the environment variable (annotation)
      r.headers().firstValue("ex-snyk-token").get().equals("snyk-token-from-env-var") &&
      r.headers().firstValue("rhda-token").get().equals("rhda-token-from-env-var") &&
        r.headers().firstValue("rhda-source").get().equals("rhda-source-from-env-var") &&
        r.headers().firstValue("rhda-operation-type").get().equals("Stack Analysis") &&

      r.method().equals("POST");

    // load dummy html and set as the expected analysis
    byte[] expectedHtml;
    try (var is = getResourceAsStreamDecision(this.getClass(), new String [] {"dummy_responses","maven","analysis-report.html"})) {
      expectedHtml = is.readAllBytes();
    }

    // mock and http response object and stub it to return a fake body
    var mockHttpResponse = mock(HttpResponse.class);
    given(mockHttpResponse.body()).willReturn(expectedHtml);
    given(mockHttpResponse.statusCode()).willReturn(200);

    // mock static getProvider utility function
    try(var ecosystemTool = mockStatic(Ecosystem.class)) {
      // stub static getProvider utility function to return our mock provider
      ecosystemTool.when(() -> Ecosystem.getProvider(tmpFile)).thenReturn(mockProvider);

      // stub the http client to return our mocked response when request matches our arg matcher
      given(mockHttpClient.sendAsync(argThat(matchesRequest), any()))
        .willReturn(CompletableFuture.completedFuture(mockHttpResponse));

      // when invoking the api for a html stack analysis report
      var htmlTxt = exhortApiSut.stackAnalysisHtml(tmpFile.toString());
      // verify we got the correct html response
      then(htmlTxt.get()).isEqualTo(expectedHtml);
    }
    // cleanup
    Files.deleteIfExists(tmpFile);
  }

  @Test
//    System.setProperty("RHDA_TOKEN", "rhda-token-from-property");
//    System.setProperty("RHDA_SOURCE", "rhda-source-from-property");
  @SetEnvironmentVariable(key="EXHORT_SNYK_TOKEN", value="snyk-token-from-env-var")
  @SetEnvironmentVariable(key="RHDA_TOKEN", value="rhda-token-from-env-var")
  @SetEnvironmentVariable(key="RHDA_SOURCE", value="rhda-source-from-env-var")
  void stackAnalysis_with_pom_xml_should_return_json_object_from_the_backend()
    throws IOException, ExecutionException, InterruptedException {
    // create a temporary pom.xml file
    var tmpFile = Files.createTempFile("exhort_test_pom_", ".xml");
    try (var is = getResourceAsStreamDecision(this.getClass(), new String [] {"tst_manifests","maven","empty","pom.xml"})) {
      Files.write(tmpFile, is.readAllBytes());
    }

    // stub the mocked provider with a fake content object
    given(mockProvider.provideStack(tmpFile))
      .willReturn(new Provider.Content("fake-body-content".getBytes(), "fake-content-type"));

    // we expect this to be ignored because tokens from env vars takes precedence
    System.setProperty("EXHORT_SNYK_TOKEN", "snyk-token-from-property");

    // create an argument matcher to make sure we mock the response for the right request
    ArgumentMatcher<HttpRequest> matchesRequest = r ->
      r.headers().firstValue("Content-Type").get().equals("fake-content-type") &&
        r.headers().firstValue("Accept").get().equals("application/json") &&
        // snyk token is set using the environment variable (annotation) - ignored the one set in properties
        r.headers().firstValue("ex-snyk-token").get().equals("snyk-token-from-env-var") &&
        r.headers().firstValue("rhda-token").get().equals("rhda-token-from-env-var") &&
        r.headers().firstValue("rhda-source").get().equals("rhda-source-from-env-var") &&
        r.headers().firstValue("rhda-operation-type").get().equals("Stack Analysis") &&
        r.method().equals("POST");

    // load dummy json and set as the expected analysis
    var mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    AnalysisReport expectedAnalysis;
    try (var is = getResourceAsStreamDecision(this.getClass(), new String [] {"dummy_responses","maven","analysis-report.json"})) {
      expectedAnalysis = mapper.readValue(is, AnalysisReport.class);
    }

    // mock and http response object and stub it to return the expected analysis
    var mockHttpResponse = mock(HttpResponse.class);
    given(mockHttpResponse.body()).willReturn(mapper.writeValueAsString(expectedAnalysis));
    given(mockHttpResponse.statusCode()).willReturn(200);

    // mock static getProvider utility function
    try(var ecosystemTool = mockStatic(Ecosystem.class)) {
      // stub static getProvider utility function to return our mock provider
      ecosystemTool.when(() -> Ecosystem.getProvider(tmpFile)).thenReturn(mockProvider);

      // stub the http client to return our mocked response when request matches our arg matcher
      given(mockHttpClient.sendAsync(argThat(matchesRequest), any()))
        .willReturn(CompletableFuture.completedFuture(mockHttpResponse));

      // when invoking the api for a json stack analysis report
      var responseAnalysis = exhortApiSut.stackAnalysis(tmpFile.toString());
      // verify we got the correct analysis report
      then(responseAnalysis.get()).isEqualTo(expectedAnalysis);
    }
    // cleanup
    Files.deleteIfExists(tmpFile);
  }

  @Test
  void componentAnalysis_with_pom_xml_should_return_json_object_from_the_backend()
    throws IOException, ExecutionException, InterruptedException {
    // load pom.xml
    byte[] targetPom;
    try (var is =  getResourceAsStreamDecision(this.getClass(), new String [] {"tst_manifests","maven","empty","pom.xml"})) {
      targetPom = is.readAllBytes();
    }

    // stub the mocked provider with a fake content object
    given(mockProvider.provideComponent(targetPom))
      .willReturn(new Provider.Content("fake-body-content".getBytes(), "fake-content-type"));

    // we expect this to picked up because no env var to take precedence
    System.setProperty("EXHORT_SNYK_TOKEN", "snyk-token-from-property");
    System.setProperty("RHDA_TOKEN", "rhda-token-from-property");
    System.setProperty("RHDA_SOURCE", "rhda-source-from-property");

    // create an argument matcher to make sure we mock the response for the right request
    ArgumentMatcher<HttpRequest> matchesRequest = r ->
      r.headers().firstValue("Content-Type").get().equals("fake-content-type") &&
        r.headers().firstValue("Accept").get().equals("application/json") &&
        // snyk token is set using properties which is picked up because no env var specified
        r.headers().firstValue("ex-snyk-token").get().equals("snyk-token-from-property") &&
        r.headers().firstValue("rhda-token").get().equals("rhda-token-from-property") &&
        r.headers().firstValue("rhda-source").get().equals("rhda-source-from-property") &&
        r.headers().firstValue("rhda-operation-type").get().equals("Component Analysis") &&
        r.method().equals("POST");

    // load dummy json and set as the expected analysis
    var mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    AnalysisReport expectedReport;
    try (var is = getResourceAsStreamDecision(this.getClass(), new String [] {"dummy_responses","maven","analysis-report.json"})) {
      expectedReport = mapper.readValue(is, AnalysisReport.class);
    }

    // mock and http response object and stub it to return the expected analysis
    var mockHttpResponse = mock(HttpResponse.class);
    given(mockHttpResponse.body()).willReturn(mapper.writeValueAsString(expectedReport));
    given(mockHttpResponse.statusCode()).willReturn(200);

    // mock static getProvider utility function
    try (var ecosystemTool = mockStatic(Ecosystem.class)) {
      // stub static getProvider utility function to return our mock provider
      ecosystemTool.when(() -> Ecosystem.getProvider("pom.xml")).thenReturn(mockProvider);

      // stub the http client to return our mocked response when request matches our arg matcher
      given(mockHttpClient.sendAsync(argThat(matchesRequest), any()))
        .willReturn(CompletableFuture.completedFuture(mockHttpResponse));

      // when invoking the api for a json stack analysis report
      var responseAnalysis = exhortApiSut.componentAnalysis("pom.xml", targetPom);
      // verify we got the correct analysis report
      then(responseAnalysis.get()).isEqualTo(expectedReport);
    }
  }

  @Test
  void stackAnalysisMixed_with_pom_xml_should_return_both_html_text_and_json_object_from_the_backend()
    throws IOException, ExecutionException, InterruptedException {
    // load dummy json and set as the expected analysis
    var mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    AnalysisReport expectedJson;
    try (var is =  getResourceAsStreamDecision(this.getClass(), new String [] {"dummy_responses","maven","analysis-report.json"})) {
      expectedJson = mapper.readValue(is, AnalysisReport.class);
    }

    // load dummy html and set as the expected analysis
    byte[] expectedHtml;
    try (var is = getResourceAsStreamDecision(this.getClass(), new String [] {"dummy_responses","maven","analysis-report.html"})) {
      expectedHtml = is.readAllBytes();
    }

    // create a temporary pom.xml file
    var tmpFile = Files.createTempFile("exhort_test_pom_", ".xml");
    try (var is = getResourceAsStreamDecision(this.getClass(), new String [] {"tst_manifests","maven","empty","pom.xml"})) {
      Files.write(tmpFile, is.readAllBytes());
    }

    // stub the mocked provider with a fake content object
    given(mockProvider.provideStack(tmpFile))
      .willReturn(new Provider.Content("fake-body-content".getBytes(), "fake-content-type"));

    // create an argument matcher to make sure we mock the response for the right request
    ArgumentMatcher<HttpRequest> matchesRequest = r ->
      r.headers().firstValue("Content-Type").get().equals("fake-content-type") &&
        r.headers().firstValue("Accept").get().equals("multipart/mixed") &&
        r.method().equals("POST");

    // load dummy mixed and set as the expected analysis
    byte[] mixedResponse;
    try (var is = getResourceAsStreamDecision(this.getClass(), new String [] {"dummy_responses","maven","analysis-report.mixed"})) {
      mixedResponse = is.readAllBytes();
    }

    // mock and http response object and stub it to return the expected analysis
    var mockHttpResponse = mock(HttpResponse.class);
    given(mockHttpResponse.body()).willReturn(mixedResponse);
    given(mockHttpResponse.statusCode()).willReturn(200);

    // mock static getProvider utility function
    try(var ecosystemTool = mockStatic(Ecosystem.class)) {
      // stub static getProvider utility function to return our mock provider
      ecosystemTool.when(() -> Ecosystem.getProvider(tmpFile)).thenReturn(mockProvider);

      // stub the http client to return our mocked response when request matches our arg matcher
      given(mockHttpClient.sendAsync(argThat(matchesRequest), any()))
        .willReturn(CompletableFuture.completedFuture(mockHttpResponse));

      // when invoking the api for a json stack analysis mixed report
      var responseAnalysis = exhortApiSut.stackAnalysisMixed(tmpFile.toString()).get();
      // verify we got the correct mixed report
      then(new String(responseAnalysis.html).trim()).isEqualTo(new String(expectedHtml).trim());
      then(responseAnalysis.json).isEqualTo(expectedJson);
    }
    // cleanup
    Files.deleteIfExists(tmpFile);
  }

  @Test
  void componentAnalysis_with_pom_xml_as_path_should_return_json_object_from_the_backend()
    throws IOException, ExecutionException, InterruptedException {
    // load pom.xml
    var tmpFile = Files.createTempFile("exhort_test_pom_", ".xml");
    try (var is = getResourceAsStreamDecision(this.getClass(), new String [] {"tst_manifests","maven","empty","pom.xml"})) {
      Files.write(tmpFile, is.readAllBytes());
    }

    // stub the mocked provider with a fake content object
    given(mockProvider.provideComponent(tmpFile))
      .willReturn(new Provider.Content("fake-body-content".getBytes(), "fake-content-type"));

    // we expect this to picked up because no env var to take precedence
    System.setProperty("EXHORT_SNYK_TOKEN", "snyk-token-from-property");

    // create an argument matcher to make sure we mock the response for the right request
    ArgumentMatcher<HttpRequest> matchesRequest = r ->
      r.headers().firstValue("Content-Type").get().equals("fake-content-type") &&
        r.headers().firstValue("Accept").get().equals("application/json") &&
        // snyk token is set using properties which is picked up because no env var specified
        r.headers().firstValue("ex-snyk-token").get().equals("snyk-token-from-property") &&
        r.method().equals("POST");

    // load dummy json and set as the expected analysis
    var mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    AnalysisReport expectedReport;
    try (var is = getResourceAsStreamDecision(this.getClass(), new String [] {"dummy_responses","maven","analysis-report.json"})) {
      expectedReport = mapper.readValue(is, AnalysisReport.class);
    }

    // mock and http response object and stub it to return the expected analysis
    var mockHttpResponse = mock(HttpResponse.class);
    given(mockHttpResponse.body()).willReturn(mapper.writeValueAsString(expectedReport));
    given(mockHttpResponse.statusCode()).willReturn(200);

    // mock static getProvider utility function
    try (var ecosystemTool = mockStatic(Ecosystem.class)) {
      // stub static getProvider utility function to return our mock provider
      ecosystemTool.when(() -> Ecosystem.getProvider(tmpFile)).thenReturn(mockProvider);

      // stub the http client to return our mocked response when request matches our arg matcher
      given(mockHttpClient.sendAsync(argThat(matchesRequest), any()))
        .willReturn(CompletableFuture.completedFuture(mockHttpResponse));

      // when invoking the api for a json stack analysis report
      var responseAnalysis = exhortApiSut.componentAnalysis(tmpFile.toString());
      // verify we got the correct analysis report
      then(responseAnalysis.get()).isEqualTo(expectedReport);
      //cleanup
      Files.deleteIfExists(tmpFile);
    }
  }


  @AfterEach
  void afterEach() {
    System.clearProperty("EXHORT_DEV_MODE");
    System.clearProperty("DEV_EXHORT_BACKEND_URL");
    System.clearProperty("RHDA_TOKEN");
    System.clearProperty("RHDA_SOURCE");

  }

  @Test
  @SetEnvironmentVariable(key="EXHORT_DEV_MODE", value="true")
  @ClearEnvironmentVariable(key="DEV_EXHORT_BACKEND_URL")
  void check_Exhort_Url_When_DEV_Mode_true_Both() {
      System.setProperty("EXHORT_DEV_MODE","true");
      ExhortApi exhortApi = new ExhortApi();
      then(exhortApi.getEndpoint()).isEqualTo(ExhortApi.DEFAULT_ENDPOINT_DEV);
      then(exhortApi.getEndpoint()).isNotEqualTo(ExhortApi.DEFAULT_ENDPOINT);
  }
@Test
  @SetEnvironmentVariable(key="EXHORT_DEV_MODE", value="true")
  @ClearEnvironmentVariable(key="DEV_EXHORT_BACKEND_URL")
  void check_Exhort_Url_When_env_DEV_Mode_true_property_DEV_Mode_false() {
      System.setProperty("EXHORT_DEV_MODE","false");
      ExhortApi exhortApi = new ExhortApi();
      then(exhortApi.getEndpoint()).isEqualTo(ExhortApi.DEFAULT_ENDPOINT_DEV);
      then(exhortApi.getEndpoint()).isNotEqualTo(ExhortApi.DEFAULT_ENDPOINT);
  }

@Test
  @SetEnvironmentVariable(key="EXHORT_DEV_MODE", value="true")
  @ClearEnvironmentVariable(key="DEV_EXHORT_BACKEND_URL")
  void check_Exhort_Url_When_env_DEV_Mode_true_And_DEV_Exhort_Url_Set_Then_Default_DEV_Exhort_URL_Not_Selected() {
      String dummyUrl = "http://dummy-url";
      System.setProperty("DEV_EXHORT_BACKEND_URL", dummyUrl);
      ExhortApi exhortApi = new ExhortApi();
      then(exhortApi.getEndpoint()).isEqualTo(dummyUrl);
      then(exhortApi.getEndpoint()).isNotEqualTo(ExhortApi.DEFAULT_ENDPOINT_DEV);
  }

@Test
  @SetEnvironmentVariable(key="EXHORT_DEV_MODE", value="false")
  @ClearEnvironmentVariable(key="DEV_EXHORT_BACKEND_URL")
void check_Exhort_Url_When_env_DEV_Mode_false_And_DEV_Exhort_Url_Set_Then_Default_DEV_Exhort_URL_Not_Selected() {
    System.setProperty("EXHORT_DEV_MODE", "false");
    ExhortApi exhortApi = new ExhortApi();
    then(exhortApi.getEndpoint()).isEqualTo(ExhortApi.DEFAULT_ENDPOINT);
    then(exhortApi.getEndpoint()).isNotEqualTo(ExhortApi.DEFAULT_ENDPOINT_DEV);
  }


  @Test
  @SetEnvironmentVariable(key="EXHORT_DEV_MODE", value= "false")
  void check_Exhort_Url_When_env_DEV_Mode_false_And_Property_Dev_Mode_true_Default_Exhort_URL_Selected() {
    System.setProperty("EXHORT_DEV_MODE", "true");
    ExhortApi exhortApi = new ExhortApi();
    then(exhortApi.getEndpoint()).isEqualTo(ExhortApi.DEFAULT_ENDPOINT);
    then(exhortApi.getEndpoint()).isNotEqualTo(ExhortApi.DEFAULT_ENDPOINT_DEV);
  }

  @Test
  @SetEnvironmentVariable(key="EXHORT_DEV_MODE", value="false")
  @SetEnvironmentVariable(key="DEV_EXHORT_BACKEND_URL", value="http://dummy-route")
  void check_Exhort_Url_When_env_DEV_Mode_false_And_DEV_Exhort_Url_Set_Then_Default_Exhort_URL_Selected_Anyway() {
    System.setProperty("EXHORT_DEV_MODE", "true");
    System.setProperty("DEV_EXHORT_BACKEND_URL","http://dummy-route2");
    ExhortApi exhortApi = new ExhortApi();
    then(exhortApi.getEndpoint()).isEqualTo(ExhortApi.DEFAULT_ENDPOINT);
    then(exhortApi.getEndpoint()).isNotEqualTo(System.getenv("DEV_EXHORT_BACKEND_URL"));
    then(exhortApi.getEndpoint()).isNotEqualTo(System.getProperty("DEV_EXHORT_BACKEND_URL"));

  }
  @Test
  void check_Exhort_Url_When_env_DEV_Mode_not_set_And_Property_Exhort_Dev_Mode_false_Then_Default_Exhort_URL_Selected() {
    System.setProperty("EXHORT_DEV_MODE", "false");
    ExhortApi exhortApi = new ExhortApi();
    then(exhortApi.getEndpoint()).isEqualTo(ExhortApi.DEFAULT_ENDPOINT);
    then(exhortApi.getEndpoint()).isNotEqualTo(ExhortApi.DEFAULT_ENDPOINT_DEV);
  }
  @Test
  void check_Exhort_Url_When_env_DEV_Mode_not_set_And_Property_Exhort_Dev_Mode_true_Then_Default_DEV_Exhort_URL_Selected() {
    System.setProperty("EXHORT_DEV_MODE", "true");
    ExhortApi exhortApi = new ExhortApi();
    then(exhortApi.getEndpoint()).isNotEqualTo(ExhortApi.DEFAULT_ENDPOINT);
    then(exhortApi.getEndpoint()).isEqualTo(ExhortApi.DEFAULT_ENDPOINT_DEV);
  }
  @Test
  @SetEnvironmentVariable(key="DEV_EXHORT_BACKEND_URL", value="http://dummy-route")
  void check_Exhort_Url_When_env_DEV_Mode_not_set_And_Property_Exhort_Dev_Mode_true_and_Env_DEV_Exhort_Backend_Url_Set_Then_DEV_ENV_Exhort_URL_Selected() {
    System.setProperty("EXHORT_DEV_MODE", "true");
    System.setProperty("DEV_EXHORT_BACKEND_URL", "http://dummy-route2");
    ExhortApi exhortApi = new ExhortApi();
    then(exhortApi.getEndpoint()).isNotEqualTo(ExhortApi.DEFAULT_ENDPOINT);
    then(exhortApi.getEndpoint()).isNotEqualTo(ExhortApi.DEFAULT_ENDPOINT_DEV);
    then(exhortApi.getEndpoint()).isNotEqualTo("http://dummy-route2");
    then(exhortApi.getEndpoint()).isEqualTo("http://dummy-route");
  }

  @Test
  void check_Exhort_Url_When_Nothing_Set_Then_Default_Exhort_URL_Selected() {
    ExhortApi exhortApi = new ExhortApi();
    then(exhortApi.getEndpoint()).isEqualTo(ExhortApi.DEFAULT_ENDPOINT);

  }

  @Test
  @SetEnvironmentVariable(key = "EXHORT_SNYK_TOKEN", value = "snyk-token-from-env-var")
  @SetEnvironmentVariable(key = "RHDA_TOKEN", value = "rhda-token-from-env-var")
  @SetEnvironmentVariable(key = "RHDA_SOURCE", value = "rhda-source-from-env-var")
  void test_image_analysis() throws IOException, ExecutionException, InterruptedException, MalformedPackageURLException {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class);
         var sbomIS = getResourceAsStreamDecision(this.getClass(), new String[]{"msc", "image", "image_sbom.json"});
         var reportIS = getResourceAsStreamDecision(this.getClass(), new String[]{"msc", "image", "image_reports.json"})) {

      var imageRef = new ImageRef("test.io/test/test-app:test-version@sha256:1fafb0905264413501df60d90a92ca32df8a2011cbfb4876ddff5ceb20c8f165", "linux/amd64");

      var jsonSbom = new BufferedReader(new InputStreamReader(sbomIS, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
      var output = new Operations.ProcessExecOutput(jsonSbom, "", 0);

      mock.when(() -> Operations.getCustomPathOrElse(eq("syft")))
        .thenReturn("syft");

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{"syft", imageRef.getImage().getFullName(),
            "-s", "all-layers", "-o", "cyclonedx-json", "-q"}),
          isNull()))
        .thenReturn(output);

      var jsonReport = new BufferedReader(new InputStreamReader(reportIS, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));

      var httpResponse = mock(HttpResponse.class);
      when(httpResponse.statusCode()).thenReturn(200);
      when(httpResponse.body()).thenReturn(jsonReport);

      ArgumentMatcher<HttpRequest> matchesRequest = r ->
        r.uri().equals(URI.create(String.format("%s/api/v4/batch-analysis", exhortApiSut.getEndpoint()))) &&
          r.headers().firstValue("Content-Type").get().equals(Api.CYCLONEDX_MEDIA_TYPE) &&
          r.headers().firstValue("Accept").get().equals(Api.MediaType.APPLICATION_JSON.toString()) &&
          r.headers().firstValue("ex-snyk-token").get().equals("snyk-token-from-env-var") &&
          r.headers().firstValue("rhda-token").get().equals("rhda-token-from-env-var") &&
          r.headers().firstValue("rhda-source").get().equals("rhda-source-from-env-var") &&
          r.headers().firstValue("rhda-operation-type").get().equals("Image Analysis") &&
          r.method().equals("POST");

      when(mockHttpClient.sendAsync(argThat(matchesRequest), any()))
        .thenReturn(CompletableFuture.completedFuture(httpResponse));

      var result = exhortApiSut.imageAnalysis(Set.of(imageRef));
      var reports = result.get();
      assertEquals(2, reports.size());
      assertTrue(reports.containsKey(new ImageRef(new PackageURL("pkg:oci/ubi@sha256:f5983f7c7878cc9b26a3962be7756e3c810e9831b0b9f9613e6f6b445f884e74?repository_url=registry.access.redhat.com/ubi9/ubi&tag=9.3-1552&arch=amd64"))));
      assertTrue(reports.containsKey(new ImageRef(new PackageURL("pkg:oci/default-app@sha256:333224a233db31852ac1085c6cd702016ab8aaf54cecde5c4bed5451d636adcf?repository_url=quay.io/default-app&tag=0.0.1"))));
      assertNotNull(reports.get(new ImageRef(new PackageURL("pkg:oci/ubi@sha256:f5983f7c7878cc9b26a3962be7756e3c810e9831b0b9f9613e6f6b445f884e74?repository_url=registry.access.redhat.com/ubi9/ubi&tag=9.3-1552&arch=amd64r"))));
      assertNotNull(reports.get(new ImageRef(new PackageURL("pkg:oci/default-app@sha256:333224a233db31852ac1085c6cd702016ab8aaf54cecde5c4bed5451d636adcf?repository_url=quay.io/default-app&tag=0.0.1"))));
    }
  }

  @Test
  @SetEnvironmentVariable(key = "EXHORT_SNYK_TOKEN", value = "snyk-token-from-env-var")
  @SetEnvironmentVariable(key = "RHDA_TOKEN", value = "rhda-token-from-env-var")
  @SetEnvironmentVariable(key = "RHDA_SOURCE", value = "rhda-source-from-env-var")
  void imageAnalysisHtml() throws IOException, ExecutionException, InterruptedException, MalformedPackageURLException {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class);
         var sbomIS = getResourceAsStreamDecision(this.getClass(), new String[]{"msc", "image", "image_sbom.json"});
         var reportIS = getResourceAsStreamDecision(this.getClass(), new String[]{"msc", "image", "image_reports.json"})) {

      var imageRef = new ImageRef("test.io/test/test-app:test-version@sha256:1fafb0905264413501df60d90a92ca32df8a2011cbfb4876ddff5ceb20c8f165", "linux/amd64");

      var jsonSbom = new BufferedReader(new InputStreamReader(sbomIS, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
      var output = new Operations.ProcessExecOutput(jsonSbom, "", 0);

      mock.when(() -> Operations.getCustomPathOrElse(eq("syft")))
        .thenReturn("syft");

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{"syft", imageRef.getImage().getFullName(),
            "-s", "all-layers", "-o", "cyclonedx-json", "-q"}),
          isNull()))
        .thenReturn(output);

      var jsonReport = new BufferedReader(new InputStreamReader(reportIS, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));

      var httpResponse = mock(HttpResponse.class);
      when(httpResponse.statusCode()).thenReturn(200);
      when(httpResponse.body()).thenReturn(jsonReport);

      ArgumentMatcher<HttpRequest> matchesRequest = r ->
        r.uri().equals(URI.create(String.format("%s/api/v4/batch-analysis", exhortApiSut.getEndpoint()))) &&
          r.headers().firstValue("Content-Type").get().equals(Api.CYCLONEDX_MEDIA_TYPE) &&
          r.headers().firstValue("Accept").get().equals(Api.MediaType.TEXT_HTML.toString()) &&
          r.headers().firstValue("ex-snyk-token").get().equals("snyk-token-from-env-var") &&
          r.headers().firstValue("rhda-token").get().equals("rhda-token-from-env-var") &&
          r.headers().firstValue("rhda-source").get().equals("rhda-source-from-env-var") &&
          r.headers().firstValue("rhda-operation-type").get().equals("Image Analysis") &&
          r.method().equals("POST");

      when(mockHttpClient.sendAsync(argThat(matchesRequest), any()))
        .thenReturn(CompletableFuture.completedFuture(httpResponse));

      var result = exhortApiSut.imageAnalysisHtml(Set.of(imageRef));
      assertEquals(jsonReport, result.get());
    }
  }

  @Test
  @SetEnvironmentVariable(key = "EXHORT_SNYK_TOKEN", value = "snyk-token-from-env-var")
  @SetEnvironmentVariable(key = "RHDA_TOKEN", value = "rhda-token-from-env-var")
  @SetEnvironmentVariable(key = "RHDA_SOURCE", value = "rhda-source-from-env-var")
  void test_perform_batch_analysis() throws IOException, MalformedPackageURLException, ExecutionException, InterruptedException {
    try (var is = getResourceAsStreamDecision(this.getClass(), new String[]{"msc", "image", "image_sbom.json"})) {
      var sbomsGenerator = mock(Supplier.class);
      var responseBodyHandler = mock(HttpResponse.BodyHandler.class);
      var responseGenerator = mock(Function.class);
      var exceptionResponseGenerator = mock(Supplier.class);

      ArgumentMatcher<HttpRequest> matchesRequest = r ->
        r.uri().equals(URI.create(String.format("%s/api/v4/batch-analysis", exhortApiSut.getEndpoint()))) &&
          r.headers().firstValue("Content-Type").get().equals(Api.CYCLONEDX_MEDIA_TYPE) &&
          r.headers().firstValue("Accept").get().equals(Api.MediaType.APPLICATION_JSON.toString()) &&
          r.headers().firstValue("ex-snyk-token").get().equals("snyk-token-from-env-var") &&
          r.headers().firstValue("rhda-token").get().equals("rhda-token-from-env-var") &&
          r.headers().firstValue("rhda-source").get().equals("rhda-source-from-env-var") &&
          r.headers().firstValue("rhda-operation-type").get().equals("Image Analysis") &&
          r.method().equals("POST");

      var imageRef = new ImageRef("test.io/test/test-app:test-version@sha256:1fafb0905264413501df60d90a92ca32df8a2011cbfb4876ddff5ceb20c8f165", "linux/amd64");
      var sboms = new HashMap<String, JsonNode>();
      sboms.put(imageRef.getPackageURL().canonicalize(), new ObjectMapper().readTree(is));

      var httpResponse = mock(HttpResponse.class);
      when(httpResponse.statusCode()).thenReturn(200);

      when(mockHttpClient.sendAsync(argThat(matchesRequest), any()))
        .thenReturn(CompletableFuture.completedFuture(httpResponse));

      when(sbomsGenerator.get()).thenReturn(sboms);

      var expectedResult = "test-result";
      when(responseGenerator.apply(eq(httpResponse))).thenReturn(expectedResult);

      var result = exhortApiSut.performBatchAnalysis(
        sbomsGenerator,
        Api.MediaType.APPLICATION_JSON,
        responseBodyHandler,
        responseGenerator,
        exceptionResponseGenerator,
        "Image Analysis");

      assertEquals(expectedResult, result.get());
    }
  }

  @Test
  void test_get_batch_image_sboms() throws IOException, MalformedPackageURLException {
    try (MockedStatic<Operations> mock = Mockito.mockStatic(Operations.class);
         var is = getResourceAsStreamDecision(this.getClass(), new String[]{"msc", "image", "image_sbom.json"})) {
      var imageRef = new ImageRef("test.io/test/test-app:test-version@sha256:1fafb0905264413501df60d90a92ca32df8a2011cbfb4876ddff5ceb20c8f165", "linux/amd64");

      var json = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
      var output = new Operations.ProcessExecOutput(json, "", 0);

      mock.when(() -> Operations.getCustomPathOrElse(eq("syft")))
        .thenReturn("syft");

      mock.when(() -> Operations.runProcessGetFullOutput(isNull(),
          aryEq(new String[]{"syft", imageRef.getImage().getFullName(),
            "-s", "all-layers", "-o", "cyclonedx-json", "-q"}),
          isNull()))
        .thenReturn(output);

      var sboms = exhortApiSut.getBatchImageSboms(Set.of(imageRef));

      var mapper = new ObjectMapper();
      var node = mapper.readTree(json);
      ((ObjectNode) node.get("metadata").get("component")).set("purl", new TextNode(imageRef.getPackageURL().canonicalize()));

      var map = new HashMap<String, JsonNode>();
      map.put(imageRef.getPackageURL().canonicalize(), node);

      assertEquals(map, sboms);
    }
  }

  @Test
  void test_get_batch_image_analysis_reports() throws IOException, MalformedPackageURLException {
    try (var is = getResourceAsStreamDecision(this.getClass(), new String[]{"msc", "image", "image_reports.json"})) {
      var json = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));

      var httpResponse = mock(HttpResponse.class);
      when(httpResponse.statusCode()).thenReturn(200);
      when(httpResponse.body()).thenReturn(json);

      var reports = exhortApiSut.getBatchImageAnalysisReports(httpResponse);
      assertEquals(2, reports.size());
      assertTrue(reports.containsKey(new ImageRef(new PackageURL("pkg:oci/ubi@sha256:f5983f7c7878cc9b26a3962be7756e3c810e9831b0b9f9613e6f6b445f884e74?repository_url=registry.access.redhat.com/ubi9/ubi&tag=9.3-1552&arch=amd64"))));
      assertTrue(reports.containsKey(new ImageRef(new PackageURL("pkg:oci/default-app@sha256:333224a233db31852ac1085c6cd702016ab8aaf54cecde5c4bed5451d636adcf?repository_url=quay.io/default-app&tag=0.0.1"))));
      assertNotNull(reports.get(new ImageRef(new PackageURL("pkg:oci/ubi@sha256:f5983f7c7878cc9b26a3962be7756e3c810e9831b0b9f9613e6f6b445f884e74?repository_url=registry.access.redhat.com/ubi9/ubi&tag=9.3-1552&arch=amd64r"))));
      assertNotNull(reports.get(new ImageRef(new PackageURL("pkg:oci/default-app@sha256:333224a233db31852ac1085c6cd702016ab8aaf54cecde5c4bed5451d636adcf?repository_url=quay.io/default-app&tag=0.0.1"))));
    }
  }

  @Test
  void test_get_batch_image_analysis_reports_error_response() {
    var httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(400);

    var reports = exhortApiSut.getBatchImageAnalysisReports(httpResponse);
    assertTrue(reports.isEmpty());
  }

  @Test
  void test_get_batch_analysis_reports_from_response() {
    var httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(200);

    var responseGenerator = mock(Function.class);

    exhortApiSut.getBatchAnalysisReportsFromResponse(httpResponse, responseGenerator,
      "test-operation", "testReport", "testTraceId");

    verify(responseGenerator).apply(eq(httpResponse));
  }
}
