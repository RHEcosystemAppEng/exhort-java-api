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
import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * The Provider abstraction is used for contracting providers providing a {@link Content}
 * per manifest type for constructing backend requests.
 **/
public abstract class Provider {
  /**
   * Content is used to aggregate a content buffer and a content type.
   * These will be used to construct the backend API request.
   **/
  public static class Content{
    public final byte[] buffer;
    public final String type;
    public Content(byte[] buffer, String type){
      this.buffer = buffer;
      this.type = type;
    }
  }

  /** POJO class used for serializing packages for a component analysis request. */
  static final public class PackageAggregator {
    public String name;
    public String version;

    public PackageAggregator(@Nonnull final String name, @Nonnull final String version) {
      Objects.requireNonNull(name);
      Objects.requireNonNull(version);
      this.name = name;
      this.version = version;
    }

    /**
     * Custom implementation will return also return true if version is *.
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PackageAggregator)) return false;
      var that = (PackageAggregator) o;
      return Objects.equals(this.name, that.name) &&
        List.of(this.version, "*").contains(that.version);
    }
  }

  /** The ecosystem of this provider, i.e. maven. */
  final public String ecosystem;
  protected ObjectMapper objectMapper;

  protected Provider(String ecosystem) {
    this.ecosystem = ecosystem;
    this.objectMapper= new ObjectMapper();
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
}
