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

import com.redhat.crda.impl.CrdaApi;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * The Provider interface is a functional interface used for contracting
 * providers providing a {@link Content} per manifest {@link Path}.
 **/
@FunctionalInterface
public interface Provider {
  /**
   * Content is used to aggregate a content buffer and a content type.
   * These will be used to construct the backend API request.
   **/
  class Content{
    public final byte[] buffer;
    public final String type;
    public Content(byte[] buffer, String type){
      this.buffer = buffer;
      this.type = type;
    }
  }

  /**
   * Use for creating a {@link Content} per manifest {@link Path}.
   *
   * @param manifestPath the Path for the manifest file
   * @return A Content record aggregating the body content and content type.
   * @throws IOException when failed to load the manifest file
   */
  Content ProvideFor(Path manifestPath) throws IOException;
}
