package com.redhat.exhort.logging;

import com.redhat.exhort.impl.ClientTraceIdSimpleFormatter;

import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

public class LoggersFactory {
  public static Logger getLogger(String loggerName) {
    Logger logger = Logger.getLogger(loggerName);
    ConsoleHandler handler = new ConsoleHandler();
    handler.setFormatter(new ClientTraceIdSimpleFormatter());
    logger.addHandler(handler);
    logger.setUseParentHandlers(false);
    return logger;

  }
}
