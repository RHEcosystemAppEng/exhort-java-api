# CodeReady Dependency Analytics Java API<br/>![latest-no-snapshot][0] ![latest-snapshot][1]

> This project is still a WIP. For analysis, currently, only Java's Maven ecosystem is implemented.

The _Crda JAVA API_ module is deployed to _GitHub Package Registry_.

<details>
<summary>Click here for configuring <em>GHPR</em> and gaining access to the <em>crda-java-api</em> module.</summary>

<h3>Create your token</h3>
<p>
Create a
<a href="https://docs.github.com/en/packages/learn-github-packages/introduction-to-github-packages#authenticating-to-github-packages">token</a>
with the <strong>read:packages</strong> scope<br/>

> Based on
> <a href="https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry">GitHub documentation</a>,
> In <em>Actions</em> you can use <em>GITHUB_TOKEN</em>

</p>

<details>
<summary>Click here for <em>Maven</em> instructions</summary>

<h3>Configure GHPR for <em>Maven</em></h3>
<ol>
<li>Encrypt your token:

```shell
$ mvn --encrypt-password created-token-goes-here

encrypted-token-will-appear-here
```

</li>
<li>Add a <em>server</em> definition in your <em>$HOME/.m2/settings.xml</em> (note the <em>id</em>):

```xml
<servers>
    ...
    <server>
        <id>github</id>
        <username>github-userid-goes-here</username>
        <password>encrypted-token-goes-here-including-curly-brackets</password>
    </server>
    ...
</servers>
```
</li>
<li> Add a <em>repository</em> definition in your <em>pom.xml</em> (note the <em>id</em>):

```xml
  <repositories>
    ...
    <repository>
      <id>github</id>
      <url>https://maven.pkg.github.com/RHEcosystemAppEng/crda-java-api</url>
      <snapshots>
        <enabled>true</enabled> <!-- omit or set to false if not using snapshots -->
      </snapshots>
    </repository>
    ...
  </repositories>
```

</li>
</ol>
</details>

<details>
<summary>Click here for <em>Gradle</em> instructions</summary>

<h3>Configure GHPR for <em>Gradle</em></h3>
<ol>
<li>Save your token and username as environment variables:
<ul>
<li><em>GITHUB_USERNAME</em></li>
<li><em>GITHUB_TOKEN</em></li>
</ul>
</li>
<li> Add a <em>maven-type repository</em> definition in your <em>build.gradle</em>:

```groovy
repositories {
    ...
    maven {
        url 'https://maven.pkg.github.com/RHEcosystemAppEng/crda-java-api'
        credentials {
            username System.getenv("GITHUB_USERNAME")
            password System.getenv("GITHUB_TOKEN")
        }
    }
    ...
}
```

</li>

</ol>
</details>

</details>

<h3>Usage</h3>
<ol>
<li>Declare the dependency:
<ul>
<li>For <em>Maven</em> in <em>pom.xml</em>:

```xml
<dependency>
    <groupId>com.redhat.crda</groupId>
    <artifactId>crda-java-api</artifactId>
    <version>${crda-java-api.version}</version>
</dependency>
```
</li>

<li>For <em>Gradle</em> in <em>build.gradle</em>:

```groovy
implementation 'com.redhat.crda:crda-java-api:${crda-java-api.version}'
```

</li>
</ul>

</li>
<li>If working with modules, configure module read:

```java
module x { // module-info.java
    requires com.redhat.crda;
}
```
</li>
<li>Code example:

```java
import com.redhat.crda.impl.CrdaApi;
import com.redhat.crda.backend.AnalysisReport;

import java.util.concurrent.CompletableFuture;

public class CrdaExample {
    public static void main(String... args) throws Exception {

        // instantiate the Crda API implementation
        var crdaApi = new CrdaApi();

        // get a String future holding a html report
        CompletableFuture<String> htmlReport = crdaApi.stackAnalysisHtmlAsync("/path/to/pom.xml");

        // get a AnalysisReport future holding a deserialized report
        CompletableFuture<AnalysisReport> analysisReport = crdaApi.stackAnalysisAsync("/path/to/pom.xml");
    }
}
```
</li>
</ol>

<!-- Badge links -->
[0]: https://img.shields.io/github/v/release/RHEcosystemAppEng/crda-java-api?color=green&label=latest
[1]: https://img.shields.io/github/v/release/RHEcosystemAppEng/crda-java-api?color=yellow&include_prereleases&label=snapshot
