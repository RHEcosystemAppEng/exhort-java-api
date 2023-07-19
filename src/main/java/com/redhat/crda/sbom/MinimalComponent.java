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
package com.redhat.crda.sbom;

import org.cyclonedx.model.Component;

import java.util.Objects;

public class MinimalComponent {
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MinimalComponent that = (MinimalComponent) o;
    return Objects.equals(name, that.name) && Objects.equals(purl, that.purl) && type == that.type && Objects.equals(bomRef, that.bomRef);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, purl, type, bomRef);
  }

  private String name;
    private String purl;
    private Component.Type type;
    private String bomRef;


    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getPurl() {
      return purl;
    }

    public void setPurl(String purl) {
      this.purl = purl;
    }

    public Component.Type getType() {
      return type;
    }

    public void setType(Component.Type type) {
      this.type = type;
    }

    public String getBomRef() {
      return bomRef;
    }

    public void setBomRef(String bomRef) {
      this.bomRef = bomRef;
    }
  }

