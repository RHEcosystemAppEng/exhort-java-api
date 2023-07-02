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
package simple.modular;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.crda.backend.AnalysisReport;
import com.redhat.crda.impl.CrdaApi;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

class Simple_Integration_Test {
  static boolean useRealAPI = true;
  CrdaApi crdaApi;
  HttpClient mockHttpClient;

  ObjectMapper mapper;

  @BeforeAll
  static void prepare() {
    var useRealApi = System.getenv("CRDA_ITS_USE_REAL_API");
    Simple_Integration_Test.useRealAPI = Boolean.parseBoolean(useRealApi);
  }
  @BeforeEach
  void initialize() throws Exception {
    mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    if (Simple_Integration_Test.useRealAPI) {
      crdaApi = new CrdaApi();
    } else {
      // mock a http client instance
      mockHttpClient = mock(HttpClient.class);
      // use reflection to make the constructor that takes a http client accessible
      var sutConstructor = CrdaApi.class.getDeclaredConstructor(HttpClient.class);
      sutConstructor.setAccessible(true);
      crdaApi = sutConstructor.newInstance(mockHttpClient);
    }
  }

  @Test
  void test_stack_analysis_html_report() throws Exception {
    // load the pre-configured expected html response
    var expectedHtmlAnalysis = Files.readAllBytes(Paths.get("src/test/resources/it_poms/analysis-report.html"));
    if (!Simple_Integration_Test.useRealAPI) {
      // mock a http response object and stub it to return the expected html report as a body
      var mockHtmlResponse = mock(HttpResponse.class);
      when(mockHtmlResponse.body()).thenReturn(expectedHtmlAnalysis);
      // stub the mocked http client to return the mocked http response for requests accepting text/html
      when(mockHttpClient.sendAsync(
        argThat(r -> r.headers().firstValue("Accept").get().equals("text/html")), any())
      ).thenReturn(CompletableFuture.completedFuture(mockHtmlResponse));
    }

    // get the html report from the api
    var htmlAnalysis = crdaApi.stackAnalysisHtml("src/test/resources/it_poms/pom.xml").get();
    assertThat(htmlAnalysis).isEqualTo(expectedHtmlAnalysis);
  }

  @Test
  void test_stack_analysis_mixed_report() throws Exception {
    // load the pre-configured expected html and json responses
    var expectedHtmlAnalysis = Files.readAllBytes(Paths.get("src/test/resources/it_poms/analysis-report.html"));
    var expectedAnalysisJson = Files.readString(Paths.get("src/test/resources/it_poms/analysis-report.json"));
    // deserialize the json expected response
    var expectedAnalysis = mapper.readValue(expectedAnalysisJson, AnalysisReport.class);

    var expectedMixedAnalysis = Files.readAllBytes(Paths.get("src/test/resources/it_poms/analysis-report.mixed"));
    if (!Simple_Integration_Test.useRealAPI) {
      // mock a http response object and stub it to return the expected html report as a body
      var mockMixedResponse = mock(HttpResponse.class);
      when(mockMixedResponse.body()).thenReturn(expectedMixedAnalysis);
      // stub the mocked http client to return the mocked http response for requests accepting text/html
      when(mockHttpClient.sendAsync(
        argThat(r -> r.headers().firstValue("Accept").get().equals("multipart/mixed")), any())
      ).thenReturn(CompletableFuture.completedFuture(mockMixedResponse));
    }

    // get the html report from the api
    var mixedAnalysis = crdaApi.stackAnalysisMixed("src/test/resources/it_poms/pom.xml").get();
    assertThat(new String(mixedAnalysis.html).trim()).isEqualTo(new String(expectedHtmlAnalysis).trim());
    assertThat(mixedAnalysis.json).isEqualTo(expectedAnalysis);
  }

  @Test
  void test_stack_analysis_report() throws Exception {
    // load the pre-configured expected json response
    var expectedAnalysisJson = Files.readString(Paths.get("src/test/resources/it_poms/analysis-report.json"));
    // deserialize the expected response
    var expectedAnalysis = mapper.readValue(expectedAnalysisJson, AnalysisReport.class);
    if (!Simple_Integration_Test.useRealAPI) {
      // mock a http response object and stub it to return the expected json report as a body
      var mockJsonResponse = mock(HttpResponse.class);
      when(mockJsonResponse.body()).thenReturn(expectedAnalysisJson);
      // stub the mocked http client to return the mocked http response for requests accepting json application
      when(mockHttpClient.sendAsync(
        argThat(r -> r.headers().firstValue("Accept").get().equals("application/json")), any())
      ).thenReturn(CompletableFuture.completedFuture(mockJsonResponse));
    }

    // get the analysis report object from the api
    var analysisReport = crdaApi.stackAnalysis("src/test/resources/it_poms/pom.xml").get();
    assertThat(analysisReport).isEqualTo(expectedAnalysis);
  }

  @Test
  void test_component_analysis_report() throws Exception {
    // load the pre-configured expected json response
    var expectedAnalysisJson = Files.readString(Paths.get("src/test/resources/it_poms/analysis-report.json"));
    // deserialize the expected response
    var expectedAnalysis = mapper.readValue(expectedAnalysisJson, AnalysisReport.class);
    if (!Simple_Integration_Test.useRealAPI) {
      // mock a http response object and stub it to return the expected json report as a body
      var mockJsonResponse = mock(HttpResponse.class);
      when(mockJsonResponse.body()).thenReturn(expectedAnalysisJson);
      // stub the mocked http client to return the mocked http response for requests accepting json application
      when(mockHttpClient.sendAsync(
        argThat(r -> r.headers().firstValue("Accept").get().equals("application/json")), any())
      ).thenReturn(CompletableFuture.completedFuture(mockJsonResponse));
    }
    // get the analysis report object from the api
    var pomContent = Files.readAllBytes(Paths.get("src/test/resources/it_poms/pom.xml"));
    var analysisReport = crdaApi.componentAnalysis("pom.xml", pomContent).get();
    assertThat(analysisReport).isEqualTo(expectedAnalysis);
  }
}
