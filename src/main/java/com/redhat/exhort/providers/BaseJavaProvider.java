package com.redhat.exhort.providers;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.redhat.exhort.Provider;
import com.redhat.exhort.sbom.Sbom;
import com.redhat.exhort.tools.Ecosystem;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public abstract class BaseJavaProvider extends Provider {

  protected BaseJavaProvider(Ecosystem.Type ecosystem) {
    super(ecosystem);
  }

  void parseDependencyTree(String src, int srcDepth, String [] lines, Sbom sbom) {
    if(lines.length == 0) {
      return;
    }
    if(lines.length == 1 && lines[0].trim().equals("")){
      return;
    }
    int index = 0;
    String target = lines[index];
    int targetDepth = getDepth(target);
    while(targetDepth > srcDepth && index < lines.length )
    {
      if(targetDepth == srcDepth + 1) {
        PackageURL from = parseDep(src);
        PackageURL to = parseDep(target);
        if(dependencyIsNotTestScope(from) && dependencyIsNotTestScope(to)) {
          sbom.addDependency(from, to);
        }
      }
      else {
        String[] modifiedLines = Arrays.copyOfRange(lines, index, lines.length);
        parseDependencyTree(lines[index-1],getDepth(lines[index-1]),modifiedLines,sbom);
      }
      if(index< lines.length - 1) {
        target = lines[++index];
        targetDepth = getDepth(target);
      }
      else
      {
        index++;
      }
    }
  }

  static boolean dependencyIsNotTestScope(PackageURL artifact) {
    return (Objects.nonNull(artifact.getQualifiers()) && !artifact.getQualifiers().get("scope").equals("test")) || Objects.isNull(artifact.getQualifiers());
  }

  PackageURL parseDep(String dep) {
    //root package
    DependencyAggregator dependencyAggregator = new DependencyAggregator();
    // in case line in dependency tree text starts with a letter ( for root artifact).
    if(dep.matches("^\\w.*"))
    {
      dependencyAggregator = new DependencyAggregator();
      String[] parts = dep.split(":");
      dependencyAggregator.groupId = parts[0];
      dependencyAggregator.artifactId = parts[1];
      dependencyAggregator.version = parts[3];

      return dependencyAggregator.toPurl();

    }
    int firstDash = dep.indexOf("-");
    String dependency = dep.substring(++firstDash).trim();
    if(dependency.startsWith("("))
    {
      dependency = dependency.substring(1);
    }
    dependency = dependency.replace(":runtime", ":compile").replace(":provided", ":compile");
    int endIndex = Math.max(dependency.indexOf(":compile"),dependency.indexOf(":test"));
    int scopeLength;
    if(dependency.indexOf(":compile") > -1) {
      scopeLength =   ":compile".length();
    }
    else {
      scopeLength =   ":test".length();
    }
    dependency = dependency.substring(0,endIndex + scopeLength);
    String[] parts = dependency.split(":");
    // contains only GAV + packaging + scope
    if(parts.length == 5)
    {
      dependencyAggregator.groupId = parts[0];
      dependencyAggregator.artifactId= parts[1];
      dependencyAggregator.version = parts[3];

      String conflictMessage = "omitted for conflict with";
      if (dep.contains(conflictMessage))
      {
        dependencyAggregator.version = dep.substring(dep.indexOf(conflictMessage) + conflictMessage.length()).replace(")", "").trim();
      }
    }
    // In case there are 6 parts, there is also a classifier for artifact (version suffix)
    // contains GAV + packaging + classifier + scope
    else if(parts.length == 6)
    {
      dependencyAggregator.groupId = parts[0];
      dependencyAggregator.artifactId= parts[1];
      dependencyAggregator.version = String.format("%s-%s",parts[4],parts[3]);
      String conflictMessage = "omitted for conflict with";
      if (dep.contains(conflictMessage))
      {
        dependencyAggregator.version = dep.substring(dep.indexOf(conflictMessage) + conflictMessage.length()).replace(")", "").trim();
      }

    }
    else{
      throw new RuntimeException(String.format("Cannot parse dependency into PackageUrl from line = \"%s\"",dep));
    }
    if(parts[parts.length - 1].matches(".*[a-z]$")) {
      dependencyAggregator.scope = parts[parts.length - 1];
    }
    else {
      int endOfLine = Integer.min(parts[parts.length - 1].indexOf(""), parts[parts.length - 1].indexOf("-"));
      dependencyAggregator.scope = parts[parts.length - 1].substring(0, endOfLine).trim();
    }
    return dependencyAggregator.toPurl();
  }

  int getDepth(String line) {
    if(line == null || line.trim().equals("")){
      return -1;
    }

    if(line.matches("^\\w.*"))
    {
      return 0;
    }

    return  ( (line.indexOf('-') -1 ) / 3) + 1;
  }

  // NOTE if we want to include "scope" tags in ignore,
  // add property here and a case in the start-element-switch in the getIgnored method

  /**
   * Aggregator class for aggregating Dependency data over stream iterations,
   **/
  final static class DependencyAggregator {
    String scope = "*";
    String groupId;
    String artifactId;
    String version;
    boolean ignored = false;

    /**
     * Get the string representation of the dependency to use as excludes
     *
     * @return an exclude string for the dependency:tree plugin, ie. group-id:artifact-id:*:version
     */
    @Override
    public String toString() {
      // NOTE if you add scope, don't forget to replace the * with its value
      return String.format("%s:%s:%s:%s", groupId, artifactId, scope, version);
    }

    boolean isValid() {
      return Objects.nonNull(groupId) && Objects.nonNull(artifactId) && Objects.nonNull(version);
    }

    boolean isTestDependency() {
      return scope.trim().equals("test");
    }

    PackageURL toPurl() {
      try {
        return new PackageURL(Ecosystem.Type.MAVEN.getType(), groupId, artifactId, version, this.scope == "*" ? null : new TreeMap<>(Map.of("scope", this.scope)), null);
      } catch (MalformedPackageURLException e) {
        throw new IllegalArgumentException("Unable to parse PackageURL", e);
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof DependencyAggregator)) return false;
      var that = (DependencyAggregator) o;
      // NOTE we do not compare the ignored field
      // This is required for comparing pom.xml with effective_pom.xml as the latter doesn't
      // contain comments indicating ignore
      return Objects.equals(this.groupId, that.groupId) &&
        Objects.equals(this.artifactId, that.artifactId) &&
        Objects.equals(this.version, that.version);

    }

    @Override
    public int hashCode() {
      return Objects.hash(groupId, artifactId, version);
    }
  }
}
