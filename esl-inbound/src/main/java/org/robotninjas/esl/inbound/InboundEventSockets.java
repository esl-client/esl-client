package org.robotninjas.esl.inbound;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.freeswitch.esl.client.EventSocket;

public class InboundEventSockets {

  private static final Injector injector = Guice.createInjector(new InboundEventSocketModule());

  public static EventSocket makeInboundEventSocket() {
    return injector.getInstance(EventSocket.class);
  }

}
