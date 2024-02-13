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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.logging.*;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.exhort.Api;
import com.redhat.exhort.Provider;
import com.redhat.exhort.api.AnalysisReport;
import com.redhat.exhort.logging.LoggersFactory;
import com.redhat.exhort.tools.Ecosystem;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

/**
 * Concrete implementation of the Exhort {@link Api} Service.
 **/
public final class ExhortApi implements Api {

//  private static final System.Logger LOG = System.getLogger(ExhortApi.class.getName());

  private static final Logger LOG = LoggersFactory.getLogger(ExhortApi.class.getName());



  public static final String DEFAULT_ENDPOINT = "https://rhda.rhcloud.com";
  public static final String DEFAULT_ENDPOINT_DEV = "https://exhort.stage.devshift.net";
  public static final String RHDA_TOKEN_HEADER = "rhda-token";
  public static final String RHDA_SOURCE_HEADER = "rhda-source";
  public static final String RHDA_OPERATION_TYPE_HEADER = "rhda-operation-type";
  public static final String EXHORT_REQUEST_ID_HEADER_NAME = "ex-request-id";

  private final String endpoint;

  public String getEndpoint() {
    return endpoint;
  }

  public static final void main(String[] args) throws IOException, InterruptedException, ExecutionException {
    System.setProperty("EXHORT_DEV_MODE", "true");
    AnalysisReport analysisReport = new ExhortApi()
      .stackAnalysisMixed("/home/zgrinber/.jenkins/workspace/run RHDA Analysis/package.json").get().json;
//    ObjectMapper om = new ObjectMapper().configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false);
//    System.out.println(om.writerWithDefaultPrettyPrinter().writeValueAsString(analysisReport));
//    AnalysisReport analysisReport = new ExhortApi()
//    byte[] analysisReport = new ExhortApi().
//    stackAnalysisHtml("/home/zgrinber/git/exhort-java-api/src/test/resources/tst_manifests/golang/go_mod_with_one_ignored_prefix_go/go.mod").get();
//    Path html = Files.createFile(Path.of("/","tmp", "golang0210.html"));
//    Files.write(html,analysisReport);

  }

  /**
   * Enum for identifying token environment variables and their
   * corresponding request headers.
   */
  private enum TokenProvider {
    SNYK, OSS_INDEX;


    /**
     * Get the expected environment variable name.
     *
     * @return i.e. EXHORT_SNYK_TOKEN
     */
    String getVarName() {
      return String.format("EXHORT_%s_TOKEN", this);
    }

    String getUserVarName() {
      return String.format("EXHORT_%s_USER", this);
    }

    /**
     * Get the expected request header name.
     *
     * @return i.e. ex-snyk-token
     */
    String getHeaderName() {
      return String.format("ex-%s-token", this.toString().replace("_", "-").toLowerCase());
    }

    String getUserHeaderName() {
      return String.format("ex-%s-user", this.toString().replace("_", "-").toLowerCase());
    }
  }

  private final HttpClient client;
  private final ObjectMapper mapper;

  private LocalDateTime startTime;
  private LocalDateTime providerEndTime;
  private LocalDateTime endTime;

  public ExhortApi() {
    this(HttpClient.newHttpClient());
  }

  /**
   * Get the HTTP protocol Version set by client in environment variable, if not set, the default is HTTP Protocol Version 1.1
   *
   * @return i.e. HttpClient.Version.HTTP_1.1
   */
  static HttpClient.Version getHttpVersion() {
    return (System.getenv("HTTP_VERSION_EXHORT_CLIENT") != null && System.getenv("HTTP_VERSION_EXHORT_CLIENT").contains("2")) ? HttpClient.Version.HTTP_2 : HttpClient.Version.HTTP_1_1;
  }

  ExhortApi(final HttpClient client) {
//    // temp system property - as long as prod exhort url not implemented the multi-source v4 endpoint, this property needs to be true
//    System.setProperty("EXHORT_DEV_MODE","true");
    commonHookBeginning(true);
    this.client = client;
    this.mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    // Take default from config.properties in case client didn't override DEV MODE
    if (System.getProperty("EXHORT_DEV_MODE") == null) {
      try {
        InputStream exhortConfig = this.getClass().getClassLoader().getResourceAsStream("config.properties");
        if (exhortConfig == null) {
          LOG.info("config.properties not found on the class path, fallback to default DEV MODE = false");
          System.setProperty("EXHORT_DEV_MODE", "false");
        } else {
          Properties properties = new Properties();
          properties.load(exhortConfig);
          System.setProperty("EXHORT_DEV_MODE", (String) properties.get("EXHORT_DEV_MODE"));
        }
      } catch (IOException e) {
        LOG.info(String.format("Error loading config.properties , fallback to set default property DEV MODE = false, Error message = %s", e.getMessage()));
        System.setProperty("EXHORT_DEV_MODE", "false");
      }
    }

    this.endpoint = getExhortUrl();
  }

  private String commonHookBeginning(boolean startOfApi) {
    if(startOfApi) {
      generateClientRequestId();
      LOG.info("Start of exhort-java-api client");
    }
    else {
      if(Objects.isNull(getClientRequestId())) {
        generateClientRequestId();
      }
      if (debugLoggingIsNeeded()) {

        this.startTime = LocalDateTime.now();

        LOG.info(String.format("Starting time: %s", this.startTime));
      }
    }
    return getClientRequestId();
  }

  private static void generateClientRequestId() {
    RequestManager.getInstance().addClientTraceIdToRequest(UUID.randomUUID().toString());
  }

  private static String getClientRequestId() {
    return RequestManager.getInstance().getTraceIdOfRequest();
  }

  public String getExhortUrl() {
    String endpoint;
    if (getBooleanValueEnvironment("EXHORT_DEV_MODE", "false")) {
      endpoint = getStringValueEnvironment("DEV_EXHORT_BACKEND_URL", DEFAULT_ENDPOINT_DEV);

    } else {
      endpoint = DEFAULT_ENDPOINT;
    }
    if (debugLoggingIsNeeded()) {
      LOG.info(String.format("EXHORT_DEV_MODE=%s,DEV_EXHORT_BACKEND_URL=%s, Chosen Backend URL=%s , DEFAULT_ENDPOINT_DEV=%s , DEFAULT_ENDPOINT=%s", getBooleanValueEnvironment("EXHORT_DEV_MODE", "false"), getStringValueEnvironment("DEV_EXHORT_BACKEND_URL", DEFAULT_ENDPOINT_DEV), endpoint, DEFAULT_ENDPOINT_DEV, DEFAULT_ENDPOINT));
    }
    return endpoint;
  }

  public static boolean getBooleanValueEnvironment(String key, String defaultValue) {
    String result = Objects.requireNonNullElse(System.getenv(key), Objects.requireNonNullElse(System.getProperty(key), defaultValue));
    return Boolean.parseBoolean(result.trim().toLowerCase());
  }

  public static String getStringValueEnvironment(String key, String defaultValue) {
    String result = Objects.requireNonNullElse(System.getenv(key), Objects.requireNonNullElse(System.getProperty(key), defaultValue));
    return result;
  }

  @Override
  public CompletableFuture<MixedReport> stackAnalysisMixed(final String manifestFile) throws IOException {
    String exClientTraceId = commonHookBeginning(false);
    return this.client.sendAsync(this.buildStackRequest(manifestFile, MediaType.MULTIPART_MIXED), HttpResponse.BodyHandlers.ofByteArray()).thenApply(resp -> {
      RequestManager.getInstance().addClientTraceIdToRequest(exClientTraceId);
      if(debugLoggingIsNeeded()) {
        logExhortRequestId(resp);
      }
      if (resp.statusCode() == 200) {
        byte[] htmlPart = null;
        AnalysisReport jsonPart = null;
        var ds = new ByteArrayDataSource(resp.body(), MediaType.MULTIPART_MIXED.toString());
        try {
          var mp = new MimeMultipart(ds);
          for (var i = 0; i < mp.getCount(); i++) {
            if (Objects.isNull(htmlPart) && MediaType.TEXT_HTML.toString().equals(mp.getBodyPart(i).getContentType())) {
              htmlPart = mp.getBodyPart(i).getInputStream().readAllBytes();
            }
            if (Objects.isNull(jsonPart) && MediaType.APPLICATION_JSON.toString().equals(mp.getBodyPart(i).getContentType())) {
              jsonPart = this.mapper.readValue(mp.getBodyPart(i).getInputStream().readAllBytes(), AnalysisReport.class);
            }
          }
        } catch (IOException | MessagingException e) {
          throw new RuntimeException(e);
        }
        commonHookAfterExhortResponse();
        return new MixedReport(Objects.requireNonNull(htmlPart), Objects.requireNonNull(jsonPart));
      } else {
        LOG.severe(String.format("failed to invoke stackAnalysisMixed for getting the html and json reports, Http Response Status=%s , received message from server= %s ", resp.statusCode(), new String(resp.body())));
        return new MixedReport();
      }
    });
  }

  @Override
  public CompletableFuture<byte[]> stackAnalysisHtml(final String manifestFile) throws IOException {
    String exClientTraceId = commonHookBeginning(false);
    return this.client.sendAsync(this.buildStackRequest(manifestFile, MediaType.TEXT_HTML), HttpResponse.BodyHandlers.ofByteArray()).thenApply(httpResponse -> {
      RequestManager.getInstance().addClientTraceIdToRequest(exClientTraceId);
      if(debugLoggingIsNeeded()) {
        logExhortRequestId(httpResponse);
      }
      if (httpResponse.statusCode() != 200) {
        LOG.severe(String.format("failed to invoke stackAnalysis for getting the html report, Http Response Status=%s , received message from server= %s ", httpResponse.statusCode(), new String(httpResponse.body())));
      }
      commonHookAfterExhortResponse();
      return httpResponse.body();
    }).exceptionally(exception -> {
      LOG.severe(String.format("failed to invoke stackAnalysis for getting the html report, received message= %s ", exception.getMessage()));
//      LOG.log(System.Logger.Level.ERROR, "Exception Entity", exception);
      commonHookAfterExhortResponse();
      return new byte[0];
    });
  }

  @Override
  public CompletableFuture<AnalysisReport> stackAnalysis(final String manifestFile) throws IOException {
    String exClientTraceId = commonHookBeginning(false);
    return this.client.sendAsync(this.buildStackRequest(manifestFile, MediaType.APPLICATION_JSON), HttpResponse.BodyHandlers.ofString())
//      .thenApply(HttpResponse::body)
      .thenApply(response -> getAnalysisReportFromResponse(response, "StackAnalysis", "json",exClientTraceId)).exceptionally(exception -> {
        LOG.severe(String.format("failed to invoke stackAnalysis for getting the json report, received message= %s ", exception.getMessage()));
//        LOG.log(System.Logger.Level.ERROR, "Exception Entity", exception);
        return new AnalysisReport();
      });
  }

  private AnalysisReport getAnalysisReportFromResponse(HttpResponse<String> response, String operation, String reportName,String exClientTraceId) {
    RequestManager.getInstance().addClientTraceIdToRequest(exClientTraceId);
    if (debugLoggingIsNeeded()) {
      logExhortRequestId(response);
    }
    if (response.statusCode() == 200) {
      if (debugLoggingIsNeeded()) {
        LOG.info(String.format("Response body received from exhort server : %s %s", System.lineSeparator(), response.body()));

      }
      commonHookAfterExhortResponse();
      try {

        return this.mapper.readValue(response.body(), AnalysisReport.class);
      } catch (JsonProcessingException e) {
        throw new CompletionException(e);
      }

    } else {
      LOG.severe(String.format("failed to invoke %s for getting the %s report, Http Response Status=%s , received message from server= %s ", operation, reportName, response.statusCode(), response.body()));
      return new AnalysisReport();
    }

  }

  private static void logExhortRequestId(HttpResponse response) {
    Optional<String> headerExRequestId = response.headers().allValues(EXHORT_REQUEST_ID_HEADER_NAME).stream().findFirst();
    headerExRequestId.ifPresent(value -> LOG.info(String.format("Unique Identifier associated with this request ( Received from Exhort Backend ) - ex-request-id= : %s", value)));
  }

  public static boolean debugLoggingIsNeeded() {
    return Boolean.parseBoolean(getStringValueEnvironment("EXHORT_DEBUG","false"));
  }

  @Override
  public CompletableFuture<AnalysisReport> componentAnalysis(final String manifestType, final byte[] manifestContent) throws IOException {
    String exClientTraceId = commonHookBeginning(false);
    var provider = Ecosystem.getProvider(manifestType);
    var uri = URI.create(String.format("%s/api/v4/analysis", this.endpoint));
    var content = provider.provideComponent(manifestContent);
    commonHookAfterProviderCreatedSbomAndBeforeExhort();
    return getAnalysisReportForComponent(uri, content,exClientTraceId);
  }

  private void commonHookAfterProviderCreatedSbomAndBeforeExhort() {
    if(debugLoggingIsNeeded()) {
      LOG.info("After Provider created sbom hook");
      this.providerEndTime = LocalDateTime.now();
      LOG.info(String.format("After Creating Sbom time: %s", this.startTime));
      LOG.info(String.format("Time took to create sbom file to be sent to exhort backend, in ms : %s, in seconds: %s",this.startTime.until(this.providerEndTime, ChronoUnit.MILLIS),(float)(this.startTime.until(this.providerEndTime, ChronoUnit.MILLIS) / 1000F)));
    }



  }

  private void commonHookAfterExhortResponse() {
    if(debugLoggingIsNeeded()) {
      this.endTime = LocalDateTime.now();
      LOG.info(String.format("After got response from exhort time: %s", this.endTime));
      LOG.info(String.format("Time took to get response from exhort backend, in ms: %s, in seconds: %s",this.providerEndTime.until(this.endTime, ChronoUnit.MILLIS),this.providerEndTime.until(this.endTime, ChronoUnit.MILLIS) / 1000F));
      LOG.info(String.format("Total time took for complete analysis, in ms: %s, in seconds: %s",this.startTime.until(this.endTime, ChronoUnit.MILLIS),this.startTime.until(this.endTime, ChronoUnit.MILLIS) / 1000F));

    }
    RequestManager.getInstance().removeClientTraceIdFromRequest();
  }
  @Override
  public CompletableFuture<AnalysisReport> componentAnalysis(String manifestFile) throws IOException {
    String exClientTraceId = commonHookBeginning(false);
    var manifestPath = Paths.get(manifestFile);
    var provider = Ecosystem.getProvider(manifestPath);
    var uri = URI.create(String.format("%s/api/v4/analysis", this.endpoint));
    var content = provider.provideComponent(manifestPath);
    commonHookAfterProviderCreatedSbomAndBeforeExhort();
    return getAnalysisReportForComponent(uri, content, exClientTraceId);
  }

  private CompletableFuture<AnalysisReport> getAnalysisReportForComponent(URI uri, Provider.Content content, String exClientTraceId) {
    return this.client.sendAsync(this.buildRequest(content, uri, MediaType.APPLICATION_JSON, "Component Analysis"), HttpResponse.BodyHandlers.ofString())
//      .thenApply(HttpResponse::body)
      .thenApply(response -> getAnalysisReportFromResponse(response, "Component Analysis", "json",exClientTraceId)).exceptionally(exception -> {
        LOG.severe( String.format("failed to invoke Component Analysis for getting the json report, received message= %s ", exception.getMessage()));
//        LOG.log(System.Logger.Level.ERROR, "Exception Entity", exception);
        return new AnalysisReport();
      });
  }

  /**
   * Build an HTTP request wrapper for sending to the Backend API for Stack Analysis only.
   *
   * @param manifestFile the path for the manifest file
   * @param acceptType   the type of requested content
   * @return a HttpRequest ready to be sent to the Backend API
   * @throws IOException when failed to load the manifest file
   */
  private HttpRequest buildStackRequest(final String manifestFile, final MediaType acceptType) throws IOException {
    var manifestPath = Paths.get(manifestFile);
    var provider = Ecosystem.getProvider(manifestPath);
    var uri = URI.create(String.format("%s/api/v4/analysis", this.endpoint));
    var content = provider.provideStack(manifestPath);
    commonHookAfterProviderCreatedSbomAndBeforeExhort();

    return buildRequest(content, uri, acceptType, "Stack Analysis");
  }

  /**
   * Build an HTTP request for sending to the Backend API.
   *
   * @param content    the {@link com.redhat.exhort.Provider.Content} info for the request body
   * @param uri        the {@link URI} for sending the request to
   * @param acceptType value the Accept header in the request, indicating the required response type
   * @return a HttpRequest ready to be sent to the Backend API
   */
  private HttpRequest buildRequest(final Provider.Content content, final URI uri, final MediaType acceptType, final String analysisType) {
    var request = HttpRequest.newBuilder(uri).version(Version.HTTP_1_1).setHeader("Accept", acceptType.toString()).setHeader("Content-Type", content.type).POST(HttpRequest.BodyPublishers.ofString(new String(content.buffer)));

    // include tokens from environment variables of java properties as request headers
    Stream.of(ExhortApi.TokenProvider.values()).forEach(p -> {
      var envToken = System.getenv(p.getVarName());
      if (Objects.nonNull(envToken)) {
        request.setHeader(p.getHeaderName(), envToken);
      } else {
        var propToken = System.getProperty(p.getVarName());
        if (Objects.nonNull(propToken)) {
          request.setHeader(p.getHeaderName(), propToken);
        }
      }
      var envUser = System.getenv(p.getUserHeaderName());
      if (Objects.nonNull(envUser)) {
        request.setHeader(p.getUserHeaderName(), envUser);
      } else {
        var propUser = System.getProperty(p.getUserVarName());
        if (Objects.nonNull(propUser)) {
          request.setHeader(p.getUserHeaderName(), propUser);
        }
      }

    });
    //set rhda-token
    // Environment variable/property name = RHDA_TOKEN
    String rhdaToken = calculateHeaderValue(RHDA_TOKEN_HEADER);
    if (rhdaToken != null && Optional.of(rhdaToken).isPresent()) {
      request.setHeader(RHDA_TOKEN_HEADER, rhdaToken);
    }
    //set rhda-source ( extension/plugin id/name)
    // Environment variable/property name = RHDA_SOURCE
    String rhdaSource = calculateHeaderValue(RHDA_SOURCE_HEADER);
    if (rhdaSource != null && Optional.of(rhdaSource).isPresent()) {
      request.setHeader(RHDA_SOURCE_HEADER, rhdaSource);
    }
    request.setHeader(RHDA_OPERATION_TYPE_HEADER, analysisType);

    return request.build();
  }

  private String calculateHeaderValue(String headerName) {
    String result;
    result = calculateHeaderValueActual(headerName);
    if (result == null) {
      result = calculateHeaderValueActual(headerName.toUpperCase().replace("-", "_"));
    }
    return result;
  }

  private String calculateHeaderValueActual(String headerName) {
    String result = null;
    result = System.getenv(headerName);
    if (result == null) {
      result = System.getProperty(headerName);
    }
    return result;
  }
}
