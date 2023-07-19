# Contributing to *exhort-java-api*<br/>![java-version][10]

* Fork the repository
* Create a new branch
* Commit your changes
* Commits <strong>must</strong> be signed-off (see [Certificate of Origin](#certificate-of-origin))
* Create a pull request against the main branch
* Pull request titles <strong>must</strong> adhere the [Conventional Commits specification][0]

## Development

### Common Build Goals

* `mvn test` compile and run unit tests
* `mvn verify` verify the project, i.e. enforce project rules, verify existing license headers,
  and verify code coverage threshold
* `mvn verify -Pcov` create coverage reports in *target/site/jacoco*
* `mvn verify -Pits` run integration tests
* `mvn verify -Pmut` run mutation tests, will fail the build if success threshold failed to meet requirements
* `mvn verify -Pmut,dev` run mutation testing, will not fail the build
* `mvn verify -Pcov,its,mut` run the entire project, including coverage reports, unit, integration, and mutation tests 
* `mvn install` install the module locally to make at accessible to other modules running locally

### Profiles Info

* `dev` - use this profile for development stage, it will skip various enforcement and verification
  executed by the *verify* goal
* `cov` - use this profile to create jacoco execution reports, it will create *html*, *xml*, and *csv*
  reports in *target/site/jacoco*
* `its` - use this profile to execute integration testing, test specifications are in [src/it](src/it)
* `mut` - use this profile to execute mutation testing, executed with [pitest](https://pitest.org)
> The following are used strictly by CI workflows. Use `mvn install` to install the module locally.
* `prepare-deployment` - use this profile for packaging of jars to deploy to artifact repository,
  it will create a *flatten pom* and a *sources*, and *javadoc* *jars*
* `deploy-github` - use this profile to include github registry distribution definition,
  used for deploying and releasing

### Good to know

* You can override the default backend url by setting another one in the _EXHORT_URL_ environment variable.

### OpenAPI Specifications

We use our [Backend's OpenAPI spec file][1] for generating types used for deserialization of the Backend's
API responses.<br/>
The generated classes target package is the `com.redhat.exhort.api`. It is skipped when calculating coverage
thresholds. **Avoid writing code in this package.**<br/> 
When the [Backend's spec file][1] is modified, we need to **manually** copy it here in
[src/main/resources/exhort](src/main/resources/exhort/openapi.yaml),
for the *openapi-generator-maven-plugin* to pick it up at **build time**.

### Modular (JPMS)

This is a *modular* module. If you write new packages or require new dependencies.
Update the [module-info.java spec](src/main/java/module-info.java) accordingly.
Please make an effort to avoid the use of *unnamed modules*.<br/>
This module is also being tested in a *modular* environment. Use
[module-info.test](src/test/java/module-info.test) for configuring the environment.

### Code Walkthrough

* The [ExhortApi](src/main/java/com/redhat/exhort/impl/ExhortApi.java) is the *Exhort Service*. It implements the
  [Api](src/main/java/com/redhat/exhort/Api.java) interface and provide various methods for requesting analysis reports.
* The [providers](src/main/java/com/redhat/exhort/providers) are concrete implementations of the
  [Provider](src/main/java/com/redhat/exhort/Provider.java) interface. And are in charge of providing content and a
  content type to be encapsulated in the request to the Backend. At the time of writing this, we only have one
  implementation, the [JavaMavenProvider](src/main/java/com/redhat/exhort/providers/JavaMavenProvider.java)
  implementation.
* The [tools](src/main/java/com/redhat/exhort/tools) package hosts various utility tools and functions used throughout
  the project. Tools such as the [Ecosystem](src/main/java/com/redhat/exhort/tools/Ecosystem.java) for instantiating
  providers per manifest. And the [Operations](src/main/java/com/redhat/exhort/tools/Operations.java) for executing
  process on the operating system.

#### Adding a Provider

* Create a concrete implementation of the [Provider](src/main/java/com/redhat/exhort/Provider.java) abstraction and place
  it in the [providers](src/main/java/com/redhat/exhort/providers) package.
* In the [Ecosystem](src/main/java/com/redhat/exhort/tools/Ecosystem.java) class look for the *getProvider function*
  and add a *switch case* instantiating the new provider per file type.

### Integration Tests

Integration tests are performed with the _maven-invoker-plugin_ invoking _maven_ projects stored in [src/it](src/it).<br/>
Integration tests are bound to the _verify_ phase of the _default_ goal and activated using the designated _its_ profile:

```shell
# run integration tests
$ mvn verify -Pits
# clean build prior
$ mvn clean verify -Pits
# while in development, the dev profile will make the build not fail for coverage and other enforcements  
$ mvn clean verify -Pits,dev
```

Integration tests are executed against a mocked _Backend_ server.<br/>
If you need to run against the actual _Backend_ server, use the _EXHORT_ITS_USE_REAL_API_ environment variable:

```shell
EXHORT_ITS_USE_REAL_API=true mvn clean verify -Pits
```

> TIP: When working on a new integration test project, it's helpful opening the IDE directly in the test project folder.

## Certificate of Origin

By contributing to this project you agree to the Developer Certificate of
Origin (DCO). This document was created by the Linux Kernel community and is a
simple statement that you, as a contributor, have the legal right to make the
contribution. See the [DCO](DCO) file for details.

<!-- Real links -->
[0]: https://www.conventionalcommits.org/en/v1.0.0/
[1]: https://github.com/RHEcosystemAppEng/exhort/blob/main/src/main/resources/META-INF/openapi.yaml

<!-- Badge links -->
[10]: https://badgen.net/badge/Java%20Version/11/5382a1
