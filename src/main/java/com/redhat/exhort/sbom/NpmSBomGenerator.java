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
package com.redhat.exhort.sbom;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cyclonedx.model.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NpmSBomGenerator extends CycloneDxSBOMGenerator {

  private static final String purlPrefix= "pkg:npm/";
  private ObjectMapper objectMapper = new ObjectMapper();
  @Override
  protected SBOMFieldsMapperMinimal providePackageMappings(Map<String, Object> packageManagerData) {
    SBOMFieldsMapperMinimal minimalSBOM = new SBOMFieldsMapperMinimal();
    MinimalComponent component = new MinimalComponent();
    String name = (String) packageManagerData.get("name");
    String version = (String) packageManagerData.get("version");
    component.setBomRef(String.format(this.purlStringFormatForArtifactVersion,name,version));
    component.setPurl(String.format(purlPrefix + this.purlStringFormatForArtifactVersion,name,version));
    component.setName(name);
    //TODO get some extra input from npm to determine this dynamically
    component.setType(Component.Type.APPLICATION);
    minimalSBOM.setComponent(component);

    // build components list
     List<MinimalComponent> components = new ArrayList<>();
    createComponents(packageManagerData, components);
    minimalSBOM.setComponents(components);

//     build Dependencies list
//    ((Map<String,Map>)packageManagerData.get("dependencies")).forEach((artifactName, properties) -> {
//         properties.;
//    });
    return minimalSBOM;
  }

  private void createComponents(Map<String, Object> packageManagerData, List<MinimalComponent> components) {
    ((Map<String,Map>) packageManagerData.get("dependencies")).forEach((artifactName, properties) -> {
      MinimalComponent current = new MinimalComponent();
      current.setName(artifactName);
      current.setType(Component.Type.LIBRARY);
      Map<String, String> stringProperties = properties;
      current.setBomRef(String.format(this.purlStringFormatForArtifactVersion,artifactName,stringProperties.get("version")));
      current.setPurl(String.format(purlPrefix + this.purlStringFormatForArtifactVersion,artifactName,stringProperties.get("version")));
      if(!components.contains(current))
      {
        components.add(current);
      }
      if(properties.get("dependencies") != null) {
        createComponents((Map<String, Object>)properties,components);

      }
    } );
  }


}
