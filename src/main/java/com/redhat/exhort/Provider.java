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
package com.redhat.exhort;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.exhort.tools.Ecosystem;
import java.io.IOException;
import java.nio.file.Path;

/**
 * The Provider abstraction is used for contracting providers providing a {@link Content} per
 * manifest type for constructing backend requests.
 */
public abstract class Provider {
  /**
   * Content is used to aggregate a content buffer and a content type. These will be used to
   * construct the backend API request.
   */
  public static class Content {
    public final byte[] buffer;
    public final String type;

    public Content(byte[] buffer, String type) {
      this.buffer = buffer;
      this.type = type;
    }
  }

  /** The ecosystem of this provider, i.e. maven. */
  public final Ecosystem.Type ecosystem;

  protected final ObjectMapper objectMapper = new ObjectMapper();

  protected Provider(Ecosystem.Type ecosystem) {
    this.ecosystem = ecosystem;
  }

  /**
   * Use for providing content for a stack analysis request.
   *
   * @param manifestPath the Path for the manifest file
   * @return A Content record aggregating the body content and content type
   * @throws IOException when failed to load the manifest file
   */
  public abstract Content provideStack(Path manifestPath) throws IOException;

  /**
   * Use for providing content for a component analysis request.
   *
   * @param manifestContent the content of the manifest file
   * @return A Content record aggregating the body content and content type
   * @throws IOException when failed to load the manifest content
   */
  public abstract Content provideComponent(byte[] manifestContent) throws IOException;

  public abstract Content provideComponent(Path manifestPath) throws IOException;
}
