package org.freeswitch.esl.client;

import org.freeswitch.esl.client.transport.event.EslEvent;

public interface EslEventListener {

  void handleEslEvent(EslEvent e);

}
