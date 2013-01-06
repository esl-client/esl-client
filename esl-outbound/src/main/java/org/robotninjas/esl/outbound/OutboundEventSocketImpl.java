package org.robotninjas.esl.outbound;

import org.freeswitch.esl.client.AbstractEventSocket;
import org.freeswitch.esl.client.EventSocketConnection;

import java.io.IOException;

public class OutboundEventSocketImpl extends AbstractEventSocket implements OutboundEventSocket {

  private final EventSocketConnection conn;

  public OutboundEventSocketImpl(EventSocketConnection conn) {
    super();
    this.conn = conn;
  }

  @Override
  public EventSocketConnection getConnection() {
    return conn;
  }

  @Override
  public void close() throws IOException {
    conn.close();
  }
}
