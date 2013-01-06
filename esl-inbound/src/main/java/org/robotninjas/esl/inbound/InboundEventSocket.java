package org.robotninjas.esl.inbound;

import com.google.common.net.HostAndPort;
import org.freeswitch.esl.client.EventSocket;

public interface InboundEventSocket extends EventSocket {

  public void connect(HostAndPort freeswitch, String password) throws InterruptedException;

}
