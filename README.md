# CodeReady Dependency Analytics Java API<br/>![latest-no-snapshot][0] ![latest-snapshot][1]

* Looking for our JavaScript/TypeScript API? Try [Exhort JavaScript API](https://github.com/RHEcosystemAppEng/exhort-javascript-api).
* Looking for our Backend implementation? Try [Exhort](https://github.com/RHEcosystemAppEng/exhort).

The _Exhort Java API_ module is deployed to _GitHub Package Registry_.

<details>
<summary>Click here for configuring <em>GHPR</em> registry access.</summary>
<h3>Configure Registry Access</h3>
<p>
Create a
<a href="https://docs.github.com/en/packages/learn-github-packages/introduction-to-github-packages#authenticating-to-github-packages">token</a>
with the <strong>read:packages</strong> scope<br/>

> Based on
> <a href="https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry">GitHub documentation</a>,
> In <em>Actions</em> you can use <em>GITHUB_TOKEN</em>
</p>

<ul>
<li>
<p><em>Maven</em> users</p>
<ol>
<li>Encrypt your token

```shell
$ mvn --encrypt-password your-ghp-token-goes-here

encrypted-token-will-appear-here
```
</li>
<li>Add a <em>server</em> definition in your <em>$HOME/.m2/settings.xml</em>

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
</ol>
</li>

<li>
<em>Gradle</em> users, save your token and username as environment variables
<ul>
<li><em>GITHUB_USERNAME</em></li>
<li><em>GITHUB_TOKEN</em></li>
</ul>
</li>
</ul>
</details>

<h3>Usage</h3>
<ol>
<li>Configure Registry</li>
<ul>
<li>
<em>Maven</em> users, add a <em>repository</em> definition in <em>pom.xml</em>

```xml
  <repositories>
    ...
    <repository>
      <id>github</id>
      <url>https://maven.pkg.github.com/RHEcosystemAppEng/exhort-java-api</url>
    </repository>
    ...
  </repositories>
```
</li>

<li>
<em>Gradle</em> users, add a <em>maven-type repository</em> definition in <em>build.gradle</em>

```groovy
repositories {
    ...
    maven {
        url 'https://maven.pkg.github.com/RHEcosystemAppEng/exhort-java-api'
        credentials {
            username System.getenv("GITHUB_USERNAME")
            password System.getenv("GITHUB_TOKEN")
        }
    }
    ...
}
```
</li>
</ul>

<li>Declare the dependency
<ul>
<li>
<em>Maven</em> users, add a dependency in <em>pom.xml</em>

```xml
<dependency>
    <groupId>com.redhat.exhort</groupId>
    <artifactId>exhort-java-api</artifactId>
    <version>0.0.7-SNAPSHOT</version>
</dependency>
```
</li>

<li>
<em>Gradle</em> users, add a dependency in <em>build.gradle</em>

```groovy
implementation 'com.redhat.exhort:exhort-java-api:${exhort-java-api.version}'
```
</li>
</ul>
</li>

<li>
If working with modules, configure module read

```java
module x { // module-info.java
    requires com.redhat.exhort;
}
```
</li>

<li>
Code example

```java
import com.redhat.exhort.Api.MixedReport;
import com.redhat.exhort.impl.ExhortApi;
import com.redhat.exhort.AnalysisReport;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

public class ExhortExample {
    public static void main(String... args) throws Exception {
        // instantiate the Exhort API implementation
        var exhortApi = new ExhortApi();

        // get a byte array future holding a html Stack Analysis report
        CompletableFuture<byte[]> htmlStackReport = exhortApi.stackAnalysisHtml("/path/to/pom.xml");

        // get a AnalysisReport future holding a deserialized Stack Analysis report
        CompletableFuture<AnalysisReport> stackReport = exhortApi.stackAnalysis("/path/to/pom.xml");

        // get a AnalysisReport future holding a mixed report object aggregating:
        // - (json) deserialized Stack Analysis report
        // - (html) html Stack Analysis report
        CompletableFuture<MixedReport> mixedStackReport = exhortApi.stackAnalysisMixed("/path/to/pom.xml");
        
        // get a AnalysisReport future holding a deserialized Component Analysis report
        var manifestContent = Files.readAllBytes(Paths.get("/path/to/pom.xml"));
        CompletableFuture<AnalysisReport> componentReport = exhortApi.componentAnalysis("pom.xml", manifestContent);
    }
}
```
</li>
</ol>

<h3>Supported Ecosystems</h3>
<ul>
<li><a href="https://www.java.com/">Java</a> - <a href="https://maven.apache.org/">Maven</a></li>
<li><a href="https://www.javascript.com//">JavaScript</a> - <a href="https://www.npmjs.com//">Npm</a></li>
<li><a href="https://go.dev//">Golang</a> - <a href="https://go.dev/blog/using-go-modules//">Go Modules</a></li>
</ul>

<h3>Excluding Packages</h3>
<p>
Excluding a package from any analysis can be achieved by marking the package for exclusion.
</p>

<ul>
<li>
<em>Java Maven</em> users can add a comment in <em>pom.xml</em>

```xml
<dependency> <!--exhortignore-->
  <groupId>...</groupId>
  <artifactId>...</artifactId>
  <version>0.0.7-SNAPSHOT</version>
</dependency>
```
</li>

</ul>
<ul>
<li>
<em>Javascript NPM </em> users can add a root (key, value) pair with value of list of names (strings) to be ignored (without versions), and key called <b>exhortignore</b> in <em>package.json</em>,  example:

```json
{
  "name": "sample",
  "version": "1.0.0",
  "description": "",
  "main": "index.js",
  "keywords": [],
  "author": "",
  "license": "ISC",
  "dependencies": {
    "dotenv": "^8.2.0",
    "express": "^4.17.1",
    "jsonwebtoken": "^8.5.1",
    "mongoose": "^5.9.18"
  },
  "exhortignore": [
    "jsonwebtoken"
  ]
}

```

<em>Golang</em> users can add in go.mod a comment with //exhortignore next to the package to be ignored, or to "piggyback" on existing comment ( e.g - //indirect) , for example:
```go
module github.com/RHEcosystemAppEng/SaaSi/deployer

go 1.19

require (
        github.com/gin-gonic/gin v1.9.1
        github.com/google/uuid v1.1.2
        github.com/jessevdk/go-flags v1.5.0 //exhortignore
        github.com/kr/pretty v0.3.1
        gopkg.in/yaml.v2 v2.4.0
        k8s.io/apimachinery v0.26.1
        k8s.io/client-go v0.26.1
)

require (
        github.com/davecgh/go-spew v1.1.1 // indirect exhortignore
        github.com/emicklei/go-restful/v3 v3.9.0 // indirect
        github.com/go-logr/logr v1.2.3 // indirect //exhortignore

)
```

All of the 3 above examples are valid for marking a package to be ignored 

</li>

</ul>

<h3>Customization</h3>
<p>
There are 2 approaches for customizing <em>Exhort Java API</em>. Using <em>Environment Variables</em> or
<em>Java Properties</em>:

```java
System.setProperty("EXHORT_SNYK_TOKEN", "my-private-snyk-token");
System.setProperty("EXHORT_MVN_PATH", "/path/to/custom/mvn");
System.setProperty("EXHORT_NPM_PATH", "/path/to/custom/npm");
System.setProperty("EXHORT_GO_PATH", "/path/to/custom/go");

```

> Environment variables takes precedence.
</p>

<h4>Customizing Tokens</h4>
<p>
For including extra vulnerability data and resolutions, otherwise only available only for vendor registered users. You
can use the following keys for setting various vendor tokens.
</p>

<table>
<tr>
<th>Vendor</th>
<th>Token Key</th>
</tr>
<tr>
<td><a href="https://app.snyk.io/redhat/snyk-token">Snyk</a></td>
<td>EXHORT_SNYK_TOKEN</td>
</tr>

</table>
<h4>Customizing HTTP Version</h4>
<p>
The HTTP Client Library can be configured to use HTTP Protocol version through environment variables, so if there is a problem with one of the HTTP Versions, the other can be configured through a dedicated environment variable.  
</p>

<table>
<tr>
<th>Environment Variable</th>
<th>Accepted Values</th>
<th>Default</th>


</tr>
<tr>
<td>HTTP_VERSION_EXHORT_CLIENT</td>
<td>[HTTP_1_1 , HTTP_2]</td>
<td>HTTP_1_1</td>
</tr>
</table>

<h4>Customizing Executables</h4>
<p>
This project uses each ecosystem's executable for creating dependency trees. These executables are expected to be
present on the system's PATH environment. If they are not, or perhaps you want to use custom ones. Use can use the
following keys for setting custom paths for the said executables.
</p>

<table>
<tr>
<th>Ecosystem</th>
<th>Default</th>
<th>Executable Key</th>
</tr>
<tr>
<td><a href="https://maven.apache.org/">Maven</a></td>
<td><em>mvn</em></td>
<td>EXHORT_MVN_PATH</td>
</tr>
<tr>
<td><a href="https://www.npmjs.com/">Node Package Manager (npm)</a></td>
<td><em>npm</em></td>
<td>EXHORT_NPM_PATH</td>
</tr>
<td><a href="https://go.dev/blog/using-go-modules/">Go Modules</a></td>
<td><em>go</em></td>
<td>EXHORT_GO_PATH</td>
</tr>

</table>

<!-- Badge links -->
[0]: https://img.shields.io/github/v/release/RHEcosystemAppEng/exhort-java-api?color=green&label=latest
[1]: https://img.shields.io/github/v/release/RHEcosystemAppEng/exhort-java-api?color=yellow&include_prereleases&label=snapshot
