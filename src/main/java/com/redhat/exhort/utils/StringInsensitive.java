package com.redhat.exhort.utils;

import java.util.Objects;

public class StringInsensitive {

  public StringInsensitive(String value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    StringInsensitive that = (StringInsensitive) o;
    return Objects.equals(value.toLowerCase(), that.value.toLowerCase());
  }

  @Override
  public int hashCode() {
    return Objects.hash(value.toLowerCase());
  }

  private String value;

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
