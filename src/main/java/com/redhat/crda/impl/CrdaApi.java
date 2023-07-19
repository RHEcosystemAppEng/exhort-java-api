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
import com.redhat.crda.Provider;
import com.redhat.crda.backend.AnalysisReport;
import com.redhat.crda.tools.Ecosystem;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

/** Concrete implementation of the Crda {@link Api} Service. **/
public final class CrdaApi implements Api {
  private static final String DEFAULT_ENDPOINT = "http://crda-backend-dev-crda.apps.sssc-cl01.appeng.rhecoeng.com";
  private final String endpoint;

  /**
   * Enum for identifying token environment variables and their
   * corresponding request headers.
   */
  private enum TokenProvider {
    SNYK;

    /**
     * Get the expected environment variable name.
     * @return i.e. CRDA_SNYK_TOKEN
     */
    String getVarName() {
      return String.format("CRDA_%s_TOKEN", this);
    }

    /**
     * Get the expected request header name.
     * @return i.e. crda-snyk-token
     */
    String getHeaderName() {
      return String.format("crda-%s-token", this.toString().toLowerCase());
    }
  }

  private final HttpClient client;
  private final ObjectMapper mapper;

  public CrdaApi() {
    this(HttpClient.newHttpClient());
  }

  CrdaApi(final HttpClient client) {
    this.client = client;
    this.mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    this.endpoint = Objects.requireNonNullElse(
      System.getenv("CRDA_BACKEND_URL"), CrdaApi.DEFAULT_ENDPOINT
    );
  }

  @Override
  public CompletableFuture<MixedReport> stackAnalysisMixed(
    final String manifestFile
  ) throws IOException {
    return this.client
      .sendAsync(
        this.buildStackRequest(manifestFile, MediaType.MULTIPART_MIXED),
        HttpResponse.BodyHandlers.ofByteArray())
      .thenApply(resp -> {
        byte[] htmlPart = null;
        AnalysisReport jsonPart = null;
        var ds = new ByteArrayDataSource(resp.body(), MediaType.MULTIPART_MIXED.toString());
        try {
          var mp = new MimeMultipart(ds);
          for (var i=0; i<= mp.getCount(); i++) {
            if (Objects.isNull(htmlPart) &&
                MediaType.TEXT_HTML.toString().equals(mp.getBodyPart(i).getContentType())) {
              htmlPart = mp.getBodyPart(i).getInputStream().readAllBytes();
            }
            if (Objects.isNull(jsonPart) &&
                MediaType.APPLICATION_JSON.toString().equals(mp.getBodyPart(i).getContentType())) {
              jsonPart = this.mapper.readValue(
                mp.getBodyPart(i).getInputStream().readAllBytes(), AnalysisReport.class);
            }
          }
        } catch (IOException | MessagingException e) {
          throw new RuntimeException(e);
        }
        return new MixedReport(Objects.requireNonNull(htmlPart), Objects.requireNonNull(jsonPart));
      });
  }

  @Override
  public CompletableFuture<byte[]> stackAnalysisHtml(final String manifestFile) throws IOException {
    return this.client
      .sendAsync(
        this.buildStackRequest(manifestFile, MediaType.TEXT_HTML),
        HttpResponse.BodyHandlers.ofByteArray())
      .thenApply(HttpResponse::body);
  }

  @Override
  public CompletableFuture<AnalysisReport> stackAnalysis(
    final String manifestFile
  ) throws IOException {
    return this.client
      .sendAsync(
        this.buildStackRequest(manifestFile, MediaType.APPLICATION_JSON),
        HttpResponse.BodyHandlers.ofString())
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

  @Override
  public CompletableFuture<AnalysisReport> componentAnalysis(
    final String manifestType, final byte[] manifestContent
  ) throws IOException {
    var provider = Ecosystem.getProvider(manifestType);
    var uri = URI.create(
      String.format("%s/api/v3/component-analysis/%s", this.endpoint, provider.ecosystem));
    var content = provider.provideComponent(manifestContent);

    return this.client
      .sendAsync(
        this.buildRequest(content, uri, MediaType.APPLICATION_JSON),
        HttpResponse.BodyHandlers.ofString())
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
   *  Build an HTTP request wrapper for sending to the Backend API for Stack Analysis only.
   *
   * @param manifestFile the path for the manifest file
   * @param acceptType the type of requested content
   * @return a HttpRequest ready to be sent to the Backend API
   * @throws IOException  when failed to load the manifest file
   */
  private HttpRequest buildStackRequest(
    final String manifestFile, final MediaType acceptType
  ) throws IOException {
    var manifestPath = Paths.get(manifestFile);
    var provider = Ecosystem.getProvider(manifestPath);
    var uri = URI.create(
      String.format("%s/api/v3/dependency-analysis/%s", this.endpoint, provider.ecosystem));
    var content = provider.provideStack(manifestPath);

    return buildRequest(content, uri, acceptType);
  }

  /**
   * Build an HTTP request for sending to the Backend API.
   *
   * @param content the {@link com.redhat.crda.Provider.Content} info for the request body
   * @param uri the {@link URI} for sending the request to
   * @param acceptType value the Accept header in the request, indicating the required response type
   * @return  a HttpRequest ready to be sent to the Backend API
   */
  private HttpRequest buildRequest(
    final Provider.Content content, final URI uri, final MediaType acceptType
  ) {
    var request = HttpRequest.newBuilder(uri)
      .setHeader("Accept", acceptType.toString())
      .setHeader("Content-Type", content.type)
      .POST(HttpRequest.BodyPublishers.ofByteArray(content.buffer));

    // include tokens from environment variables of java properties as request headers
    Stream.of(CrdaApi.TokenProvider.values()).forEach(p -> {
      var envToken = System.getenv(p.getVarName());
      if (Objects.nonNull(envToken)) {
        request.setHeader(p.getHeaderName(), envToken);
      } else {
        var propToken = System.getProperty(p.getVarName());
        if (Objects.nonNull(propToken)) {
          request.setHeader(p.getHeaderName(), propToken);
        }
      }
    });

    return request.build();
  }
}
