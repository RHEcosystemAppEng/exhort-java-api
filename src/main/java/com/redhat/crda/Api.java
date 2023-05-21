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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.crda.backend.AnalysisReport;
import com.redhat.crda.tools.Ecosystem;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Crda API Service **/
public final class Api {
  private static final String ENDPOINT = "http://crda-backend-crda.apps.sssc-cl01.appeng.rhecoeng.com";

  private final HttpClient client;
  private final ObjectMapper mapper;

  public Api() {
    this(HttpClient.newHttpClient());
  }

  Api(final HttpClient client) {
    this.client = client;
    this.mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public CompletableFuture<String> getStackAnalysisHtml(final String manifestFile) {
    return this.client
      .sendAsync(this.buildRequest(manifestFile, "text/html"), HttpResponse.BodyHandlers.ofString())
      .thenApply(HttpResponse::body);
  }

  public CompletableFuture<AnalysisReport> getStackAnalysisJson(final String manifestFile) {
    return this.client
      .sendAsync(this.buildRequest(manifestFile, "application/json"), HttpResponse.BodyHandlers.ofString())
      .thenApply(HttpResponse::body)
      .thenApply(
        s -> {
          try {
            return this.mapper.readValue(s, AnalysisReport.class);
          } catch (JsonProcessingException e) {
            throw new CompletionException(e);
          }
        }
      );
  }

  private HttpRequest buildRequest(final String manifestFile, final String acceptType) {
    var manifestPath = Paths.get(manifestFile);
    var manifest = Ecosystem.getManifest(manifestPath);

    var uri = URI.create(
      String.format("%s/api/v3/dependency-analysis/%s", Api.ENDPOINT, manifest.packageManager().toString()));

    var content = manifest.provider().ProvideFor(manifestPath);

    return HttpRequest.newBuilder(uri)
      .setHeader("Accept", acceptType)
      .setHeader("Content-Type", content.type())
      .POST(HttpRequest.BodyPublishers.ofByteArray(content.buffer()))
      .build();
  }
}
