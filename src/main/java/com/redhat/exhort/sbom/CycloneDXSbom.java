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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.cyclonedx.BomGeneratorFactory;
import org.cyclonedx.CycloneDxSchema.Version;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Component.Type;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.Metadata;

import com.github.packageurl.PackageURL;

public class CycloneDXSbom implements Sbom {

    private static final Version VERSION = Version.VERSION_14;

    private Bom bom;
    private PackageURL root;

    public CycloneDXSbom() {
        bom = new Bom();
        bom.setVersion(1);
        Metadata metadata = new Metadata();
        metadata.setTimestamp(new Date());
        bom.setMetadata(metadata);
        bom.setComponents(new ArrayList<>());
        bom.setDependencies(new ArrayList<>());
    }

    public Sbom addRoot(PackageURL rootRef) {
        this.root = rootRef;
        Component rootComponent = newRootComponent(rootRef);
        bom.getMetadata().setComponent(rootComponent);
        bom.getComponents().add(rootComponent);
        bom.getDependencies().add(newDependency(rootRef));
        return this;
    }

    public PackageURL getRoot() {
        return root;
    }

    private Component newRootComponent(PackageURL ref) {
        Component c = new Component();
        c.setBomRef(ref.getCoordinates());
        c.setName(ref.getName());
        c.setGroup(ref.getNamespace());
        c.setVersion(ref.getVersion());
        c.setType(Type.APPLICATION);
        c.setPurl(ref);
        return c;
    }

    private Component newComponent(PackageURL ref) {
        Component c = new Component();
        c.setBomRef(ref.getCoordinates());
        c.setName(ref.getName());
        c.setGroup(ref.getNamespace());
        c.setVersion(ref.getVersion());
        c.setPurl(ref);
        c.setType(Type.LIBRARY);
        return c;
    }

    private Dependency newDependency(PackageURL ref) {
        return new Dependency(ref.getCoordinates());
    }

    @Override
    public Sbom filterIgnoredDeps(Collection<String> ignoredDeps) {
        List<String> initialIgnoreRefs = bom.getComponents()
                .stream()
                .filter(c -> ignoredDeps.contains(c.getName()))
                .map(Component::getBomRef).collect(Collectors.toList());
        List<String> refsToIgnore = createIgnoreFilter(bom.getDependencies(),
                initialIgnoreRefs);
        bom.setComponents(bom.getComponents()
                .stream()
                .filter(c -> !refsToIgnore.contains(c.getBomRef()))
                .collect(Collectors.toList()));
        var newDeps = bom.getDependencies()
                .stream()
                .filter(d -> !refsToIgnore.contains(d.getRef()))
                .collect(Collectors.toList());
        bom.setDependencies(newDeps);
        bom.getDependencies().stream().forEach(d -> {
            if (d.getDependencies() != null) {
                var filteredDeps = d.getDependencies()
                        .stream()
                        .filter(td -> !refsToIgnore.contains(td.getRef()))
                        .collect(Collectors.toList());
                d.setDependencies(filteredDeps);
            }
        });
        return this;
    }

    private List<String> createIgnoreFilter(List<Dependency> deps, Collection<String> toIgnore) {
        List<String> result = new ArrayList<>(toIgnore);
        List<String> t = deps.stream()
                .filter(d -> toIgnore.contains(d.getRef()))
                .dropWhile(d -> d.getDependencies() == null)
                .map(Dependency::getDependencies)
                .flatMap(Collection::stream)
                .map(Dependency::getRef)
                .collect(Collectors.toList());
        if (t.isEmpty()) {
            return result;
        }
        result.addAll(t);
        return createIgnoreFilter(deps, result);
    }

    @Override
    public Sbom addDependency(PackageURL sourceRef, PackageURL targetRef) {
        Component srcComp = newComponent(sourceRef);
        Dependency srcDep;
        if (bom.getComponents().stream().noneMatch(c -> c.getBomRef().equals(srcComp.getBomRef()))) {
            bom.addComponent(srcComp);
            srcDep = newDependency(sourceRef);
            bom.addDependency(srcDep);
        } else {
            Optional<Dependency> existingDep = bom.getDependencies().stream()
                    .filter(d -> d.getRef().equals(srcComp.getBomRef())).findFirst();
            if (existingDep.isPresent()) {
                srcDep = existingDep.get();
            } else {
                srcDep = newDependency(sourceRef);
                bom.addDependency(srcDep);
            }
        }
        Dependency targetDep = newDependency(targetRef);
        srcDep.addDependency(targetDep);
        if (bom.getDependencies().stream().noneMatch(d -> d.getRef().equals(targetDep.getRef()))) {
            bom.addDependency(targetDep);
        }
        if (bom.getComponents().stream().noneMatch(c -> c.getBomRef().equals(targetDep.getRef()))) {
            bom.addComponent(newComponent(targetRef));
        }
        return this;
    }

    @Override
    public String getAsJsonString() {
        return BomGeneratorFactory.createJson(VERSION, bom).toJsonString();
    }

}
