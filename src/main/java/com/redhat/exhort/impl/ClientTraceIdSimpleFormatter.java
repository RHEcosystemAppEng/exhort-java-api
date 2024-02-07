package com.redhat.exhort.impl;

import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class ClientTraceIdSimpleFormatter extends SimpleFormatter {
  @Override
  public String format(LogRecord record) {
    return String.format("%s, ex-client-trace-id: %s",super.format(record).trim(),RequestManager.getInstance().getTraceIdOfRequest() + System.lineSeparator());
  }
}
