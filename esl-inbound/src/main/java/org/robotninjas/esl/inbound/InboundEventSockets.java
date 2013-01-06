package org.robotninjas.esl.inbound;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class InboundEventSockets {

  private static final Injector injector = Guice.createInjector(new InboundEventSocketModule());

  public static InboundEventSocketImpl makeInboundEventSocket() {
    return injector.getInstance(InboundEventSocketImpl.class);
  }

}
