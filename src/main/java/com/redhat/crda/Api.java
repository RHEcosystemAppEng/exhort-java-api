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

import com.redhat.crda.backend.AnalysisReport;
import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** The Api interface is used for contracting API implementations. **/
public interface Api {
  enum MediaType {
    APPLICATION_JSON,
    TEXT_HTML,
    MULTIPART_MIXED;

    @Override
    public String toString() {
      return this.name().toLowerCase().replace("_", "/");
    }
  }

  /** POJO class used for aggregating multipart/mixed analysis requests. */
  class MixedReport {
    final public byte[] html;
    final public AnalysisReport json;

    public MixedReport(final byte[] html, final AnalysisReport json) {
      this.html = html;
      this.json = json;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || this.getClass() != o.getClass()) return false;
      var that = (MixedReport) o;
      return Arrays.equals(this.html, that.html) && Objects.equals(this.json, that.json);
    }

    @Override
    public int hashCode() {
      return 31 * Objects.hash(json) + Arrays.hashCode(html);
    }
  }

  /**
   * Use for creating a stack analysis HTML report for a given manifest file.
   *
   * @param manifestFile the path for the manifest file
   * @return a mixed reports for both HTML and JSON wrapped in a CompletableFuture
   * @throws IOException when failed to load the manifest file
   */
  CompletableFuture<MixedReport> stackAnalysisMixed(String manifestFile) throws IOException;

  /**
   * Use for creating a stack analysis HTML report for a given manifest file.
   *
   * @param manifestFile the path for the manifest file
   * @return the HTML report as a String wrapped in a CompletableFuture
   * @throws IOException when failed to load the manifest file
   */
  CompletableFuture<byte[]> stackAnalysisHtml(String manifestFile) throws IOException;

  /**
   * Use for creating a stack analysis deserialized Json report for a given manifest file.
   *
   * @param manifestFile the path for the manifest file
   * @return the deserialized Json report as an AnalysisReport wrapped in a CompletableFuture
   * @throws IOException when failed to load the manifest file
   */
  CompletableFuture<AnalysisReport> stackAnalysis(String manifestFile) throws IOException;

  /**
   * Use for creating a component analysis deserialized Json report for a given type and content.
   *
   * @param manifestType the type of the manifest, i.e. {@code pom.xml}
   * @param manifestContent a byte array of the manifest's content
   * @return the deserialized Json report as an AnalysisReport wrapped in a CompletableFuture
   * @throws IOException when failed to load the manifest content
   */
  CompletableFuture<AnalysisReport> componentAnalysis(String manifestType, byte[] manifestContent) throws IOException;
}
