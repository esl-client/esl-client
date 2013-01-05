package org.freeswitch.esl.client;

import org.freeswitch.esl.client.transport.CommandResponse;
import org.freeswitch.esl.client.transport.event.EslEvent;

public interface MessageHandler {

  void handleEvent(EslEvent e);

  void handleCommandResponse(CommandResponse cr);

}
