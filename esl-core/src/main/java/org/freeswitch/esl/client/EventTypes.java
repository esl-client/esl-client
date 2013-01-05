package org.freeswitch.esl.client;

public enum EventTypes implements EventType {

  BACKGROUND_JOB("BACKGROUND_JOB");

  private final String value;

  EventTypes(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }
}
