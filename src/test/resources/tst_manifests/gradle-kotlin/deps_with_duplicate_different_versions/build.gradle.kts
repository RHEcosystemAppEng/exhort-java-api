plugins {
    id("java")
}

group = "org.acme.dbaas"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("log4j:log4j:1.2.17")
    implementation("log4j:log4j:1.2.14")
}
tasks.test {
    useJUnitPlatform()
}
