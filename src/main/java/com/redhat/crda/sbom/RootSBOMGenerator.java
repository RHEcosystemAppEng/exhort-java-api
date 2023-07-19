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


import com.redhat.crda.Provider;
import org.cyclonedx.model.Bom;

import java.util.List;
import java.util.Map;

public abstract class RootSBOMGenerator {

  protected abstract SBOMFieldsMapperMinimal providePackageMappings(Map<String,Object> packageManagerData);

  protected abstract Bom copyFieldsToSBOM(SBOMFieldsMapperMinimal fields);
  protected abstract String createFinalSbom(Bom sbom);
  protected abstract void defineMandatoryFields();

  protected abstract void filterOutIgnoredDeps(Bom sbom, List<Provider.PackageAggregator> ignoredDependencies);
  protected abstract void filterOutIgnoredDeps(Object sbom, List<Provider.PackageAggregator> ignoredDependencies);

}
