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
package com.redhat.crda.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.crda.Api;
import com.redhat.crda.backend.AnalysisReport;
import com.redhat.crda.tools.Ecosystem;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Concrete implementation of the Crda {@link Api} Service. **/
public final class CrdaApi implements Api {
  private static final String ENDPOINT = "http://crda-backend-dev-crda.apps.sssc-cl01.appeng.rhecoeng.com";

  private final HttpClient client;
  private final ObjectMapper mapper;

  public CrdaApi() {
    this(HttpClient.newHttpClient());
  }

  CrdaApi(final HttpClient client) {
    this.client = client;
    this.mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  @Override
  public CompletableFuture<String> stackAnalysisHtmlAsync(final String manifestFile) throws IOException {
    return this.client
      .sendAsync(this.buildRequest(manifestFile, "text/html"), HttpResponse.BodyHandlers.ofString())
      .thenApply(HttpResponse::body);
  }

  @Override
  public CompletableFuture<AnalysisReport> stackAnalysisAsync(final String manifestFile) throws IOException {
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

  /**
   *  Build and HTTP request for sending to the Backend API.
   *
   * @param manifestFile the path for the manifest file
   * @param acceptType the type of requested content, typically text/html or application/json
   * @return a HttpRequest ready to be sent to the Backend API
   * @throws IOException  when failed to load the manifest file
   */
  private HttpRequest buildRequest(final String manifestFile, final String acceptType) throws IOException {
    var manifestPath = Paths.get(manifestFile);
    var manifest = Ecosystem.getManifest(manifestPath);

    var uri = URI.create(
      String.format("%s/api/v3/dependency-analysis/%s", CrdaApi.ENDPOINT, manifest.packageManager().toString()));

    var content = manifest.provider().ProvideFor(manifestPath);

    return HttpRequest.newBuilder(uri)
      .setHeader("Accept", acceptType)
      .setHeader("Content-Type", content.type())
      .POST(HttpRequest.BodyPublishers.ofByteArray(content.buffer()))
      .build();
  }
}
