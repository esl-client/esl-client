package org.freeswitch.esl.client.internal;

import com.google.common.util.concurrent.ListenableFuture;
import org.freeswitch.esl.client.transport.CommandResponse;
import org.freeswitch.esl.client.transport.SendMsg;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.freeswitch.esl.client.transport.message.EslMessage;

public interface IModEslApi {
  boolean canSend();

  EslMessage sendApiCommand(String command, String arg);

  ListenableFuture<EslEvent> sendBackgroundApiCommand(String command, String arg);

  CommandResponse setEventSubscriptions(String format, String events);

  CommandResponse cancelEventSubscriptions();

  CommandResponse addEventFilter(String eventHeader, String valueToFilter);

  CommandResponse deleteEventFilter(String eventHeader, String valueToFilter);

  CommandResponse sendMessage(SendMsg sendMsg);

  CommandResponse setLoggingLevel(String level);

  CommandResponse cancelLogging();
}
