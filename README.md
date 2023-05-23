# CodeReady Dependency Analytics Java API<br/>![latest-no-snapshot][0] ![latest-snapshot][1]

> This project is still in development mode. For analysis, currently, only Java's Maven ecosystem is implemented.

The _Crda JAVA API_ module is deployed to _GitHub Package Registry_.

<details>
<summary>Click here for configuring <em>GHPR</em> and gaining access to the <em>crda-java-api</em> module.</summary>
<ul>
<li>Create a <a href="https://docs.github.com/en/packages/learn-github-packages/introduction-to-github-packages#authenticating-to-github-packages">token</a> with the <strong>read:packages</strong> scope</li>
<li>Encrypt your token:

```shell
$ mvn --encrypt-password created-token-goes-here

encrypted-token-will-be-here-including-curly-braces
```

</li>
<li>Add a <em>server</em> definition in your <em>$HOME/.m2/settings.xml</em> (note the <em>id</em>):

```xml
<servers>
    ...
    <server>
        <id>github</id>
        <username>github-userid-goes-here</username>
        <password>encrypted-token-goes-here</password>
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
</ul>
</details>

```xml
<dependency>
    <groupId>com.redhat.crda</groupId>
    <artifactId>crda-java-api</artifactId>
    <version>${crda-java-api.version}</version>
</dependency>
```

```java
module x { // module-info.java
    requires com.redhat.crda;
}
```

```java
import com.redhat.crda.impl.CrdaApi;
import com.redhat.crda.backend.AnalysisReport;

import java.util.concurrent.CompletableFuture;

public class CrdaExample {
    public static void main(String... args) throws Exception {

        // instantiate the Crda API implementation
        var crdaApi = new CrdaApi();

        // get a String future holding a html report
        CompletableFuture<String> htmlReport = crdaApi.getStackAnalysisHtml("/path/to/pom.xml");

        // get a AnalysisReport future holding a deserialized report
        CompletableFuture<AnalysisReport> jsonReport = crdaApi.getStackAnalysisJson("/path/to/pom.xml");
    }
}
```

<!-- Badge links -->
[0]: https://img.shields.io/github/v/release/RHEcosystemAppEng/crda-java-api?color=green&label=latest
[1]: https://img.shields.io/github/v/release/RHEcosystemAppEng/crda-java-api?color=yellow&include_prereleases&label=snapshot
