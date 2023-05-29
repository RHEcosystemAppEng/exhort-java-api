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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.crda.backend.AnalysisReport;
import com.redhat.crda.impl.CrdaApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

class Simple_Integration_Test {
  CrdaApi crdaApi;
  boolean mockRealAPI = true;
  HttpClient mockHttpClient;

  @BeforeEach
  void initialize() throws Exception {
    if (mockRealAPI) {
      // mock a http client instance
      mockHttpClient = mock(HttpClient.class);
      // use reflection to make the constructor that takes a http client accessible
      var sutConstructor = CrdaApi.class.getDeclaredConstructor(HttpClient.class);
      sutConstructor.setAccessible(true);
      crdaApi = sutConstructor.newInstance(mockHttpClient);
    } else {
      crdaApi = new CrdaApi();
    }
  }

  @Test
  void test_stack_analysis_html_report() throws Exception {
    // load the pre-configured expected html response
    var expectedHtmlAnalysis = Files.readAllBytes(Paths.get("src/test/resources/it_poms/response.html"));
    if (mockRealAPI) {
      // mock a http response object and stub it to return the expected html report as a body
      var mockHtmlResponse = mock(HttpResponse.class);
      when(mockHtmlResponse.body()).thenReturn(expectedHtmlAnalysis);
      // stub the mocked http client to return the mocked http response for requests accepting text/html
      when(mockHttpClient.sendAsync(
        argThat(r -> r.headers().firstValue("Accept").get().equals("text/html")), any())
      ).thenReturn(CompletableFuture.completedFuture(mockHtmlResponse));
    }

    // get the html report from the api
    var htmlAnalysis = new ApiWrapper(crdaApi).getAnalysisHtml("src/test/resources/it_poms/pom.xml");
    assertThat(htmlAnalysis).isEqualTo(expectedHtmlAnalysis);
  }

  @Test
  void test_stack_analysis_report() throws Exception {
    // load the pre-configured expected json response
    var expectedAnalysisJson = Files.readString(Paths.get("src/test/resources/it_poms/response.json"));
    // deserialize the expected response
    var expectedAnalysis = new ObjectMapper().readValue(expectedAnalysisJson, AnalysisReport.class);
    if (mockRealAPI) {
      // mock a http response object and stub it to return the expected json report as a body
      var mockJsonResponse = mock(HttpResponse.class);
      when(mockJsonResponse.body()).thenReturn(expectedAnalysisJson);
      // stub the mocked http client to return the mocked http response for requests accepting json application
      when(mockHttpClient.sendAsync(
        argThat(r -> r.headers().firstValue("Accept").get().equals("application/json")), any())
      ).thenReturn(CompletableFuture.completedFuture(mockJsonResponse));
    }

    // get the analysis report object from the api
    var analysisReport = new ApiWrapper(crdaApi).getAnalysis("src/test/resources/it_poms/pom.xml");
    assertThat(analysisReport).isEqualTo(expectedAnalysis);
  }
}
