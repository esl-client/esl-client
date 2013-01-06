package org.freeswitch.esl.client;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.freeswitch.esl.client.transport.CommandResponse;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import javax.inject.Inject;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;

public class EventSocketConnectionImpl extends SimpleChannelUpstreamHandler implements EventSocketConnection {

  private final CopyOnWriteArraySet<EslEventListener> listeners = new CopyOnWriteArraySet<EslEventListener>();
  private final ConcurrentLinkedQueue<SettableFuture<CommandResponse>> responses = new ConcurrentLinkedQueue<SettableFuture<CommandResponse>>();
  private final Channel channel;

  @Inject
  public EventSocketConnectionImpl(Channel channel) {
    this.channel = channel;
  }

  public ListenableFuture<Boolean> login(String password) {
    return null;
  }

  @Override
  public ListenableFuture<CommandResponse> api(Command cmd, String arg) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public ListenableFuture<CommandResponse> bgapi(Command cmd, String arg) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public ListenableFuture<CommandResponse> sendevent(EslEvent e) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public ListenableFuture<CommandResponse> sendmsg(UUID call, SendMsg msg) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public ListenableFuture<CommandResponse> linger() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public ListenableFuture<CommandResponse> nolinger() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public ListenableFuture<CommandResponse> myevents() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public ListenableFuture<CommandResponse> divertevents(boolean divert) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public ListenableFuture<CommandResponse> event(EventFormat fmt, EventType type) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public ListenableFuture<CommandResponse> filter(EventHeader header, String value) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public ListenableFuture<CommandResponse> filterdelete(EventHeader header, String value) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public ListenableFuture<CommandResponse> log(LogLevel level) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public ListenableFuture<CommandResponse> nolog() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public ListenableFuture<CommandResponse> nixevent(EventType type) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public ListenableFuture<CommandResponse> noevents() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void addEventListener(EslEventListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeEslEventListener(EslEventListener listener) {
    listeners.remove(listener);
  }

  @Override
  public void close() throws IOException {
    channel.close();
  }

  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {

    final Object message = e.getMessage();

    if (message instanceof EslEvent) {
      final EslEvent event = (EslEvent) message;
      for (final EslEventListener listener : listeners) {
        listener.handleEslEvent(event);
      }
    } else if (message instanceof CommandResponse) {
      final CommandResponse response = (CommandResponse) message;
      responses.poll().set(response);
    }

  }
}
