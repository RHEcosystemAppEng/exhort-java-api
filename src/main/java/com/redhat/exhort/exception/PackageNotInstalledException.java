package com.redhat.exhort.exception;

public class PackageNotInstalledException extends RuntimeException {
  public PackageNotInstalledException(String message) {
    super(message);
  }
}
