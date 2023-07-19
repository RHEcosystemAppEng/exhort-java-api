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

import com.redhat.exhort.Provider;
import org.cyclonedx.BomGeneratorFactory;
import org.cyclonedx.CycloneDxSchema;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.Metadata;

import java.util.List;
import java.util.Map;

public abstract class CycloneDxSBOMGenerator extends RootSBOMGenerator{
  private static final CycloneDxSchema.Version sbomVersion= CycloneDxSchema.Version.VERSION_14;
  public static final String purlStringFormatForArtifactVersion = "%s@%s";

  @Override
  // Must be overridden by the package manager subclass.
  protected abstract SBOMFieldsMapperMinimal providePackageMappings(Map<String, Object> packageManagerData);

  @Override
  protected void filterOutIgnoredDeps(Bom sbom, List<Provider.PackageAggregator> ignoredDependencies) {

    List<Component> components = sbom.getComponents();
    ignoredDependencies.forEach(pack ->{
      String artifactPlusVersion = String.format(purlStringFormatForArtifactVersion,pack.name,pack.version);
      components.removeIf( component -> component.getPurl().contains(artifactPlusVersion));
      }
    );
    List<Dependency> dependencies = sbom.getDependencies();
    ignoredDependencies.forEach(pack ->{
      String artifactPlusVersion = String.format(purlStringFormatForArtifactVersion,pack.name,pack.version);
      dependencies.removeIf(dependency -> dependency.getRef().equals(artifactPlusVersion));
    });

  }

  @Override
  protected void filterOutIgnoredDeps(Object sbom, List<Provider.PackageAggregator> ignoredDependencies) {
       //no-ops
  }

  @Override
  protected Bom copyFieldsToSBOM(SBOMFieldsMapperMinimal fields) {
    //need to be common to all package managers
    Bom bom = new Bom();

    bom.setVersion(SBOMFieldsMapperMinimal.version);

    //Create metadata and map primary component
    Metadata metadata = new Metadata();
    Component primaryComponent = new Component();
    primaryComponent.setName(fields.getComponent().getName());
    primaryComponent.setPurl(fields.getComponent().getPurl());
    primaryComponent.setType(fields.getComponent().getType());
    primaryComponent.setBomRef(fields.getComponent().getBomRef());
    metadata.setComponent(primaryComponent);
    bom.setMetadata(metadata);


    //Map List of Components
    fields.getComponents().forEach(minimalComponent -> {
      Component tempComponent = new Component();

      tempComponent.setName(minimalComponent.getName());
      tempComponent.setPurl(minimalComponent.getPurl());
      tempComponent.setType(minimalComponent.getType());
      tempComponent.setBomRef(minimalComponent.getBomRef());
      bom.addComponent(tempComponent);
    } );


    //Map List of Dependencies
     fields.getDependencies().forEach(dep -> {
       bom.addDependency(dep);

     });
    return bom;
  }

  @Override
  protected String createFinalSbom(Bom sbom) {
    //Provide default to all package managers that aren't using any automation tool/binary tool.
    return BomGeneratorFactory.createJson(sbomVersion,sbom).toJsonString();

  }

  @Override
  protected void defineMandatoryFields() {

  }

  public final String generateSBOM(Map<String, Object> packageManagerValues)
  {
    this.defineMandatoryFields();
    SBOMFieldsMapperMinimal sbom = this.providePackageMappings(packageManagerValues);
    Bom bom = this.copyFieldsToSBOM(sbom);
    return this.createFinalSbom(bom);


  }
}
