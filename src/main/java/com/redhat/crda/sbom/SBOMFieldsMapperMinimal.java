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
import org.cyclonedx.model.Dependency;

import java.util.List;

public class SBOMFieldsMapperMinimal {

   public static final String bomFormat= "CycloneDX";
   public static final String specVersion= "1.4";
   public static final Integer version= 1;
   private MinimalComponent component;

   private List<MinimalComponent> components;
   private List<Dependency> dependencies;

  public MinimalComponent getComponent() {
    return component;
  }

  public void setComponent(MinimalComponent component) {
    this.component = component;
  }

  public List<MinimalComponent> getComponents() {
    return components;
  }

  public void setComponents(List<MinimalComponent> components) {
    this.components = components;
  }

  public List<Dependency> getDependencies() {
    return dependencies;
  }

  public void setDependencies(List<Dependency> dependencies) {
    this.dependencies = dependencies;
  }



}
