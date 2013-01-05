package org.freeswitch.esl.client;

import com.google.common.util.concurrent.ListenableFuture;
import org.freeswitch.esl.client.transport.CommandResponse;
import org.freeswitch.esl.client.transport.event.EslEvent;

import java.io.IOException;

public interface EventSocket {

  CommandResponse makeApiCall(Command cmd, String args) throws InterruptedException, IOException;

  ListenableFuture<EslEvent> makeBackgroundApiCall(Command cmd, String args);

  CommandResponse sendMessage(SendMsg msg);

  void addEventListener(EventType event, EventListener listener);

  void removeEventListener(EventType event, EventListener listener);

  EventSocketConnection getConnection();

}
