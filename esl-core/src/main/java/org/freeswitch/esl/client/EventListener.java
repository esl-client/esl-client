package org.freeswitch.esl.client;

import org.freeswitch.esl.client.transport.event.EslEvent;

public interface EventListener {

  void handle(EslEvent e);

}
