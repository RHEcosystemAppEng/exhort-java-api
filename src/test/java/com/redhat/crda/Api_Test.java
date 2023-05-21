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
package com.redhat.crda;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.BDDMockito.given;

import com.redhat.crda.tools.Ecosystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@ExtendWith(MockitoExtension.class)
class Api_Test {
  @Mock
  Provider mockProvider;
  @Mock
  HttpClient mockHttpClient;
  @InjectMocks
  Api apiSut;

  @Test
  void the_getStackAnalysisHtml_method_should_return_html_report_from_the_backend()
      throws IOException, ExecutionException, InterruptedException {
    // create a temporary pom.xml file
    var tmpFile = Files.createTempFile(null, null);
    try (var is = getClass().getModule().getResourceAsStream("tst_manifests/pom.xml")) {
      Files.write(tmpFile, is.readAllBytes());
    }

    // create a fake manifest object serving the mocked provider
    var fakeManifest = new Ecosystem.Manifest(tmpFile, Ecosystem.PackageManager.MAVEN, mockProvider);
    // stub the mocked provider with a fake content object
    given(mockProvider.ProvideFor(tmpFile))
      .willReturn(new Provider.Content("fake-body-content".getBytes(), "fake-content-type"));

    // create an argument matcher to make sure we mock the response to the right request
    ArgumentMatcher<HttpRequest> matchesRequest = r ->
      r.headers().firstValue("Content-Type").get().equals("fake-content-type") &&
      r.headers().firstValue("Accept").get().equals("text/html") &&
      r.method().equals("POST");

    // mock and http response object and stub it to return a fake body
    var mockHttpResponse = mock(HttpResponse.class);
    given(mockHttpResponse.body()).willReturn("<html>hello-crda</html>");

    // mock static getManifest utility function
    try(var ecosystemTool = mockStatic(Ecosystem.class)) {
      // stub static getManifest utility function to return our fake manifest
      ecosystemTool.when(() -> Ecosystem.getManifest(tmpFile)).thenReturn(fakeManifest);

      // stub the http client to return our mocked response when request matches our arg matcher
      given(mockHttpClient.sendAsync(argThat(matchesRequest), any()))
        .willReturn(CompletableFuture.completedFuture(mockHttpResponse));

      // when invoking the api for a html stack analysis report
      var htmlTxt = apiSut.getStackAnalysisHtml(tmpFile.toString());
      // verify we got the correct html response
      then(htmlTxt.get()).isEqualTo("<html>hello-crda</html>");
    }
    // cleanup
    Files.delete(tmpFile);
  }
}
