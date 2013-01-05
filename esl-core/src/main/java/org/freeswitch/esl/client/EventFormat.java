package org.freeswitch.esl.client;

public enum EventFormat {
  PLAIN("plain"),
  JSON("json"),
  XML("xml");

  private final String literal;

  private EventFormat(String literal) {
    this.literal = literal;
  }

  public String literal() {
    return literal;
  }
}
