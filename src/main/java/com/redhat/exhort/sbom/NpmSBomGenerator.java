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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;

import java.io.IOException;
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
    List<Dependency> dependencies = new ArrayList<>();
//    build Dependencies list
    createDependencies(packageManagerData,dependencies);
//    objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    minimalSBOM.setComponents(components);
    minimalSBOM.setDependencies(dependencies);




//    ((Map<String,Map>)packageManagerData.get("dependencies")).forEach((artifactName, properties) -> {
//         properties.;
//    });
    return minimalSBOM;
  }

  /**
   *
   * @param packageManagerData - A Map with All npm' package.json Dependencies and hierarchies
   * @param components - List of Components to be included in final sbom, will be built in this method
   * @implNote - components list will be populated recursively while traversing the Map.
   */
  private void createComponents(Map<String, Object> packageManagerData, List<MinimalComponent> components) {
    ((Map<String, Map>) packageManagerData.get("dependencies")).forEach((artifactName, properties) -> {
      MinimalComponent current = new MinimalComponent();
      current.setName(artifactName);
      current.setType(Component.Type.LIBRARY);
      Map<String, String> stringProperties = properties;
      current.setBomRef(String.format(purlPrefix + this.purlStringFormatForArtifactVersion, artifactName, stringProperties.get("version")).trim());
      current.setPurl(String.format(purlPrefix + this.purlStringFormatForArtifactVersion, artifactName, stringProperties.get("version")).trim());
      if (!components.contains(current)) {
        components.add(current);
      }
      if (properties.get("dependencies") != null) {
        createComponents((Map<String, Object>) properties, components);

      }
    });
  }

  /**
   *
   * @param packageManagerData - A Map with All npm' package.json Dependencies and hierarchies
   * @param dependencies - List of Components' Dependencies to be included in final sbom, each component and its direct dependencies. will be built in this method.
   *                       the list will be populated recursively while traversing the Map.
   */
    private void createDependencies(Map<String, Object> packageManagerData, List<Dependency> dependencies)  {
// Main/root Component (the application/library whose package.json belongs to her) structure different then all dependencies, then need to populate it differently before starting to traverse the Map.
         Dependency main = new Dependency(String.format(purlPrefix + this.purlStringFormatForArtifactVersion, packageManagerData.get("name"), packageManagerData.get("version")));
         // add main component to dependencies list.
         dependencies.add(main);
      Map<String, Map> dependenciesOfMain = (Map<String, Map>) packageManagerData.get("dependencies");
      //for each dependency of main component, add it to main component, deep clone it and go find all dependencies of it recursively
      dependenciesOfMain.forEach((name,properties)->{
        Dependency current = new Dependency(String.format(purlPrefix + this.purlStringFormatForArtifactVersion, name, properties.get("version")));
        Dependency clonedDep;
        try {
          // deep clone main dependency into a copy.
          clonedDep = objectMapper.readValue(objectMapper.writeValueAsString(current), Dependency.class);
        } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
        }
         // add it as a dependency to main component
        main.addDependency(current);
        // if current component has its own dependencies , send the cloned component to calculate its direct dependencies
        if(properties.get("dependencies") != null)
        {
          getAllDependencies((Map<String,Map>)properties.get("dependencies"),clonedDep,dependencies);
        }
        else
        {
          if(!dependencies.contains(clonedDep)) {
            dependencies.add(clonedDep);
          }
        }
      });


    }

  private void getAllDependencies(Map<String,Map> depStructure, Dependency main, List<Dependency> dependencies) {
      // add component to dependencies list
    if(!dependencies.contains(main)) {
      dependencies.add(main);
    }
    // add each one of its dependencies to component' dependencies, deep clone it, and go calculate its dependencies recursively if it has some.
      depStructure.forEach((name,properties)->{
      Dependency current = new Dependency(String.format(purlPrefix + this.purlStringFormatForArtifactVersion, name, properties.get("version")));
       main.addDependency(current);

        Dependency clonedDep;
        try {
          // Deep clone current component to be added as a separate component and only with direct dependencies
          clonedDep = objectMapper.readValue(objectMapper.writeValueAsString(current), Dependency.class);
        } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
        }
        // if current component has dependencies, traverse recursively to find and add them.
      if(properties.get("dependencies") != null)
      {
        getAllDependencies((Map<String,Map>)properties.get("dependencies"),clonedDep,dependencies);
      }
      /**  otherwise, only add it to {@link dependencies} list as a component with no direct dependencies.*/
      else
      {
        if(!dependencies.contains(clonedDep)) {
          dependencies.add(clonedDep);
        }
      }
    });
  }

}
