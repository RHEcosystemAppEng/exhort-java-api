module com.redhat.exhort {
  requires java.net.http;

  requires com.fasterxml.jackson.annotation;
  requires com.fasterxml.jackson.core;
  requires transitive com.fasterxml.jackson.databind;
  requires jakarta.annotation;
  requires java.xml;
  requires jakarta.mail;
  requires cyclonedx.core.java;
  requires transitive packageurl.java;

  opens com.redhat.exhort.api to com.fasterxml.jackson.databind;
  opens com.redhat.exhort.providers to com.fasterxml.jackson.databind;

  exports com.redhat.exhort;
  exports com.redhat.exhort.api;
  exports com.redhat.exhort.api.serialization;
  exports com.redhat.exhort.impl;
  exports com.redhat.exhort.sbom;
  exports com.redhat.exhort.tools;

  opens com.redhat.exhort.sbom to com.fasterxml.jackson.databind, packageurl.java;
  opens com.redhat.exhort.api.serialization to com.fasterxml.jackson.databind;
}
