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
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/** The Api interface is used for contracting API implementations. **/
public interface Api {
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
