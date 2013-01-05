package org.freeswitch.esl.client;

import com.google.common.util.concurrent.ListenableFuture;
import org.freeswitch.esl.client.transport.CommandResponse;
import org.freeswitch.esl.client.transport.event.EslEvent;

import java.io.Closeable;
import java.util.UUID;

public interface EventSocketConnection extends Closeable {

  ListenableFuture<CommandResponse> api(Command cmd, String arg);

  ListenableFuture<CommandResponse> bgapi(Command cmd, String arg);

  ListenableFuture<CommandResponse> sendevent(EslEvent e);

  ListenableFuture<CommandResponse> sendmsg(UUID call, SendMsg msg);

  ListenableFuture<CommandResponse> linger();

  ListenableFuture<CommandResponse> nolinger();

  ListenableFuture<CommandResponse> myevents();

  ListenableFuture<CommandResponse> divertevents(boolean divert);

  ListenableFuture<CommandResponse> event(EventFormat fmt, EventType type);

  ListenableFuture<CommandResponse> filter(EventHeader header, String value);

  ListenableFuture<CommandResponse> filterdelete(EventHeader header, String value);

  ListenableFuture<CommandResponse> log(LogLevel level);

  ListenableFuture<CommandResponse> nolog();

  ListenableFuture<CommandResponse> nixevent(EventType type);

  ListenableFuture<CommandResponse> noevents();

  void addEventListener(EslEventListener listener);

  void removeEslEventListener(EslEventListener listener);

}
