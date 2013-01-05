package org.freeswitch.esl.client;

public enum EventHeaders implements EventHeader {

  EVENT_TYPE("Event-Name");

  private final String literal;

  private EventHeaders(String literal) {
    this.literal = literal;
  }

  public String literal() {
    return literal;
  }

}
