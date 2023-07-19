module com.redhat.crda {
  requires java.net.http;

  requires com.fasterxml.jackson.annotation;
  requires com.fasterxml.jackson.core;
  requires com.fasterxml.jackson.databind;
  requires jakarta.annotation;
  requires java.xml;
  requires jakarta.mail;

  opens com.redhat.crda.backend to com.fasterxml.jackson.databind;
  opens com.redhat.crda.providers to com.fasterxml.jackson.databind;

  exports com.redhat.crda;
  exports com.redhat.crda.backend;
  exports com.redhat.crda.impl;
}
