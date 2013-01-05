package org.robotninjas.esl.outbound;

import org.freeswitch.esl.client.AbstractEventSocket;
import org.freeswitch.esl.client.EventSocketConnection;

public class OutboundEventSocket extends AbstractEventSocket {

  private final EventSocketConnection conn;

  public OutboundEventSocket(EventSocketConnection conn) {
    super();
    this.conn = conn;
  }

  @Override
  public EventSocketConnection getConnection() {
    return conn;
  }
}
