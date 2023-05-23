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

import com.redhat.crda.Api;
import com.redhat.crda.backend.AnalysisReport;

public final class ApiWrapper {
  private final Api crdaApi;

  public ApiWrapper(final Api crdaApi) {
    this.crdaApi = crdaApi;
  }

  public String getAnalysisHtml(final String manifestPath) throws Exception {
    return crdaApi.stackAnalysisHtmlAsync(manifestPath).get();
  }

  public AnalysisReport getAnalysis(final String manifestPath) throws Exception {
    return crdaApi.stackAnalysisAsync(manifestPath).get();
  }
}
