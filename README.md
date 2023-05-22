# CodeReady Dependency Analytics Java API<br/>![java-version][0] ![latest-no-snapshot][1] ![latest-snapshot][2]

> This project is still in development mode. For analysis, currently, only Java's Maven ecosystem is implemented.

The _Crda JAVA API_ module is deployed to _GitHub Package Registry_.<br/>
Follow [this][20] to incorporate _GHPR_ in you build and gain access to the _crda-java-api_ module.   

```xml
<dependency>
    <groupId>com.redhat.crda</groupId>
    <artifactId>crda-java-api</artifactId>
    <version>${crda-java-api.version}</version>
</dependency>
```

```java
requires com.redhat.crda; // module-info.java
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
        // get a AnalysisReport future holding a json report
        CompletableFuture<AnalysisReport> jsonReport = crdaApi.getStackAnalysisJson("/path/to/pom.xml");
    }
}
```


<!-- Badge links -->
[0]: https://badgen.net/badge/Java%20Version/17/5382a1
[1]: https://img.shields.io/github/v/release/RHEcosystemAppEng/crda-java-api?color=green&label=latest
[2]: https://img.shields.io/github/v/release/RHEcosystemAppEng/crda-java-api?color=yellow&include_prereleases&label=snapshot

<!-- Real links -->
[20]: https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-with-a-personal-access-token
