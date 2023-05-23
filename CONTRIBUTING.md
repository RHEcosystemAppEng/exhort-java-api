# Contributing to *crda-java-api*<br/>![java-version][10]

* Fork the repository
* Create a new branch
* Commit your changes
* Commits <strong>must</strong> be signed-off (see [Certificate of Origin](#certificate-of-origin))
* Create a pull request against the main branch
* Pull request titles <strong>must</strong> adhere the [Conventional Commits specification][0]

## Development

### Common Build Goals

* `mvn test` compile and run unit tests
* `mvn verify` verify the project, i.e. enforce project rules, verify existing license headers, and verify code coverage threshold
* `mvn verify -Pcov` create coverage reports in *target/site/jacoco*
* `mvn verify -Pits` run integration tests
* `mvn verify -Pmut` run mutation tests, will fail the build if success threshold failed to meet requirements
* `mvn verify -Pmut,dev` run mutation testing, will not fail the build
* `mvn verify -Pcov,its,mut` run the entire project, including enforcement and unit, integration, and mutation tests 
* `mvn install` install the module locally to make at accessible to other modules running locally

### Profiles Info

* `dev` - use this profile for development stage, it will skip various enforcement and verification executed by the *verify* goal
* `cov` - use this profile to create jacoco execution reports, it will create *html*, *xml*, and *csv* reports in *target/site/jacoco*
* `its` - use this profile to execute integration testing, test specifications are in [src/it](src/it)
* `mut` - use this profile to execute mutation testing, executed with [pitest](https://pitest.org)
> The following are used strictly by CI workflows. Use `mvn install` to install the module locally.
* `prepare-deployment` - use this profile for packaging of jars to deploy to artifact repository, it will create a *flatten pom* and a *sources*, and *javadoc* *jars*
* `deploy-github` - use this profile to include github registry distribution definition, used for deploying and releasing

### OpenAPI Specifications

We use our [Backend's OpenAPI spec file][1] for generating types used for deserialization of the Backend's API responses.<br/>
The generated classes target package is the `com.redhat.crda.backend`. It is skipped when calculating coverage thresholds. **Avoid writing code in this package.**<br/> 
When the [Backend's spec file][1] is modified, we need to **manually** copy it here in [src/main/resources/crda_backend](src/main/resources/crda_backend/openapi.yaml),
for the *openapi-generator-maven-plugin* to pick it up at **build time**.

### Modular (JPMS)

This is a *modular* module. If you write new packages or require new dependencies. Update the [module-info.java spec](src/main/java/module-info.java) accordingly.
Please make an effort to avoid the use of *unnamed modules*.<br/>
This module is also being tested in a *modular* environment. You can use [module-info.test](src/test/java/module-info.test) for configuring the environment.

## Certificate of Origin

By contributing to this project you agree to the Developer Certificate of
Origin (DCO). This document was created by the Linux Kernel community and is a
simple statement that you, as a contributor, have the legal right to make the
contribution. See the [DCO](DCO) file for details.

<!-- Real links -->
[0]: https://www.conventionalcommits.org/en/v1.0.0/
[1]: https://github.com/RHEcosystemAppEng/crda-backend/blob/main/src/main/resources/META-INF/openapi.yaml

<!-- Badge links -->
[10]: https://badgen.net/badge/Java%20Version/17/5382a1
