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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RequestManager {

  private static RequestManager requestManager;
  private Map<String, String> requests;

  public static RequestManager getInstance() {
    if (Objects.isNull(requestManager)) {
      requestManager = new RequestManager();
    }
    return requestManager;
  }

  private RequestManager() {
    requests = new HashMap<>();
  }

  public synchronized void addClientTraceIdToRequest(String requestId) {
    requests.put(concatenatedThreadId(), requestId);
  }

  public synchronized void removeClientTraceIdFromRequest() {
    requests.remove(concatenatedThreadId());
  }

  public String getTraceIdOfRequest() {
    return requests.get(concatenatedThreadId());
  }

  private static String concatenatedThreadId() {
    return String.format("%s-%s", Thread.currentThread().getName(), Thread.currentThread().getId());
  }
}
