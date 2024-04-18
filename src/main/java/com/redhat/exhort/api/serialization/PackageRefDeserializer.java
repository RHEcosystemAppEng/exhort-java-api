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
package com.redhat.exhort.api.serialization;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.redhat.exhort.api.PackageRef;
import java.io.IOException;

public class PackageRefDeserializer extends StdDeserializer<PackageRef> {

  public PackageRefDeserializer() {
    this(null);
  }

  public PackageRefDeserializer(Class<PackageRef> c) {
    super(c);
  }

  @Override
  public PackageRef deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException, JacksonException {
    JsonNode n = p.getCodec().readTree(p);
    String purl = n.asText();
    if (purl == null) {
      return null;
    }
    return new PackageRef(purl);
  }
}
