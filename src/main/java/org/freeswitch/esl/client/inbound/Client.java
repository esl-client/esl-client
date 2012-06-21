/*
 * Copyright 2010 david varnes.
 *
 * Licensed under the Apache License, version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.freeswitch.esl.client.inbound;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.freeswitch.esl.client.transport.CommandResponse;
import org.freeswitch.esl.client.transport.SendMsg;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.freeswitch.esl.client.transport.message.EslHeaders;
import org.freeswitch.esl.client.transport.message.EslMessage;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Entry point to connect to a running FreeSWITCH Event Socket Library module, as a client.
 * <p/>
 * This class provides what the FreeSWITCH documentation refers to as an 'Inbound' connection
 * to the Event Socket module. That is, with reference to the socket listening on the FreeSWITCH
 * server, this client occurs as an inbound connection to the server.
 * <p/>
 * See <a href="http://wiki.freeswitch.org/wiki/Mod_event_socket">http://wiki.freeswitch.org/wiki/Mod_event_socket</a>
 *
 * @author david varnes
 */
public class Client {

  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private final List<IEslEventListener> eventListeners = new CopyOnWriteArrayList<IEslEventListener>();
  private final AtomicBoolean authenticatorResponded = new AtomicBoolean(false);
  private boolean authenticated;
  private CommandResponse authenticationResponse;
  private Channel channel;
  private final ConcurrentHashMap<String, SettableFuture<EslEvent>> backgroundJobs =
    new ConcurrentHashMap<String, SettableFuture<EslEvent>>();

  public boolean canSend() {
    return channel != null && channel.isConnected() && authenticated;
  }

  public void addEventListener(IEslEventListener listener) {
    if (listener != null) {
      eventListeners.add(listener);
    }
  }

  private void checkConnected() {
    if (!canSend()) {
      throw new IllegalStateException("Not connected to FreeSWITCH Event Socket");
    }
  }

  /**
   * Attempt to establish an authenticated connection to the nominated FreeSWITCH ESL server socket.
   * This call will block, waiting for an authentication handshake to occur, or timeout after the
   * supplied number of seconds.
   *
   * @param host           can be either ip address or hostname
   * @param port           tcp port that server socket is listening on (set in event_socket_conf.xml)
   * @param password       server event socket is expecting (set in event_socket_conf.xml)
   * @param timeoutSeconds number of seconds to wait for the server socket before aborting
   */
  public void connect(String host, int port, String password, int timeoutSeconds) throws InboundConnectionFailure {
    // If already connected, disconnect first
    if (canSend()) {
      close();
    }

    // Configure this client
    ClientBootstrap bootstrap = new ClientBootstrap(
      new NioClientSocketChannelFactory(
        Executors.newCachedThreadPool(),
        Executors.newCachedThreadPool()));

    // Add ESL handler and factory
    InboundClientHandler handler = new InboundClientHandler(password, protocolListener);
    bootstrap.setPipelineFactory(new InboundPipelineFactory(handler));

    // Attempt connection
    ChannelFuture future = bootstrap.connect(new InetSocketAddress(host, port));

    // Wait till attempt succeeds, fails or timeouts
    if (!future.awaitUninterruptibly(timeoutSeconds, TimeUnit.SECONDS)) {
      throw new InboundConnectionFailure("Timeout connecting to " + host + ":" + port);
    }
    // Did not timeout
    channel = future.getChannel();
    // But may have failed anyway
    if (!future.isSuccess()) {
      log.warn("Failed to connect to [{}:{}]", host, port);
      log.warn("  * reason: {}", future.getCause());

      channel = null;
      bootstrap.releaseExternalResources();

      throw new InboundConnectionFailure("Could not connect to " + host + ":" + port, future.getCause());
    }

    //  Wait for the authentication handshake to call back
    while (!authenticatorResponded.get()) {
      try {
        Thread.sleep(250);
      } catch (InterruptedException e) {
        // ignore
      }
    }

    if (!authenticated) {
      throw new InboundConnectionFailure("Authentication failed: " + authenticationResponse.getReplyText());
    }
  }

  /**
   * Sends a FreeSWITCH API command to the server and blocks, waiting for an immediate response from the
   * server.
   * <p/>
   * The outcome of the command from the server is retured in an {@link EslMessage} object.
   *
   * @param command API command to send
   * @param arg     command arguments
   * @return an {@link EslMessage} containing command results
   */
  public EslMessage sendApiCommand(String command, String arg) {

    checkArgument(!isNullOrEmpty(command), "command cannot be null or empty");
    checkConnected();

    try {

      final StringBuilder sb = new StringBuilder();
      sb.append("api ").append(command);
      if (!isNullOrEmpty(arg)) {
        sb.append(' ').append(arg);
      }

      final InboundClientHandler handler = (InboundClientHandler) channel.getPipeline().getLast();
      return handler.sendApiSingleLineCommand(channel, sb.toString()).get();

    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  /**
   * Submit a FreeSWITCH API command to the server to be executed in background mode. A synchronous
   * response from the server provides a UUID to identify the job execution results. When the server
   * has completed the job execution it fires a BACKGROUND_JOB Event with the execution results.<p/>
   * Note that this Client must be subscribed in the normal way to BACKGOUND_JOB Events, in order to
   * receive this event.
   *
   * @param command API command to send
   * @param arg     command arguments
   * @return String Job-UUID that the server will tag result event with.
   */
  public ListenableFuture<EslEvent> sendBackgroundApiCommand(String command, String arg) {

    checkArgument(!isNullOrEmpty(command), "command cannot be null or empty");
    checkConnected();

    final StringBuilder sb = new StringBuilder();
    sb.append("bgapi ").append(command);
    if (!isNullOrEmpty(arg)) {
      sb.append(' ').append(arg);
    }

    final InboundClientHandler handler = (InboundClientHandler) channel.getPipeline().getLast();
    final ListenableFuture<String> jobUuid = handler.sendBackgroundApiCommand(channel, sb.toString());
    final SettableFuture<EslEvent> future = SettableFuture.create();
    Futures.addCallback(jobUuid, new FutureCallback<String>() {
      @Override
      public void onSuccess(String result) {
        backgroundJobs.put(result, future);
      }

      @Override
      public void onFailure(Throwable t) {
        future.setException(t);
      }
    });
    return future;
  }

  /**
   * Set the current event subscription for this connection to the server.  Examples of the events
   * argument are:
   * <pre>
   *   ALL
   *   CHANNEL_CREATE CHANNEL_DESTROY HEARTBEAT
   *   CUSTOM conference::maintenance
   *   CHANNEL_CREATE CHANNEL_DESTROY CUSTOM conference::maintenance sofia::register sofia::expire
   * </pre>
   * Subsequent calls to this method replaces any previous subscriptions that were set.
   * </p>
   * Note: current implementation can only process 'plain' events.
   *
   * @param format can be { plain | xml }
   * @param events { all | space separated list of events }
   * @return a {@link CommandResponse} with the server's response.
   */
  public CommandResponse setEventSubscriptions(String format, String events) {
    // temporary hack
    checkState(format.equals("plain"), "Only 'plain' event format is supported at present");
    checkArgument(!isNullOrEmpty(format), "Format cannot be null or empty");
    checkConnected();

    try {

      final StringBuilder sb = new StringBuilder();
      sb.append("event ").append(format);
      if (!isNullOrEmpty(events)) {
        sb.append(' ').append(events);
      }

      final InboundClientHandler handler = (InboundClientHandler) channel.getPipeline().getLast();
      final EslMessage response = handler.sendApiSingleLineCommand(channel, sb.toString()).get();
      return new CommandResponse(sb.toString(), response);

    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }

  }

  /**
   * Cancel any existing event subscription.
   *
   * @return a {@link CommandResponse} with the server's response.
   */
  public CommandResponse cancelEventSubscriptions() {

    checkConnected();

    try {
      final InboundClientHandler handler = (InboundClientHandler) channel.getPipeline().getLast();
      final EslMessage response = handler.sendApiSingleLineCommand(channel, "noevents").get();
      return new CommandResponse("noevents", response);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  /**
   * Add an event filter to the current set of event filters on this connection. Any of the event headers
   * can be used as a filter.
   * </p>
   * Note that event filters follow 'filter-in' semantics. That is, when a filter is applied
   * only the filtered values will be received. Multiple filters can be added to the current
   * connection.
   * </p>
   * Example filters:
   * <pre>
   *    eventHeader        valueToFilter
   *    ----------------------------------
   *    Event-Name         CHANNEL_EXECUTE
   *    Channel-State      CS_NEW
   * </pre>
   *
   * @param eventHeader   to filter on
   * @param valueToFilter the value to match
   * @return a {@link CommandResponse} with the server's response.
   */
  public CommandResponse addEventFilter(String eventHeader, String valueToFilter) {

    checkArgument(!isNullOrEmpty(eventHeader), "eventHeader cannot be null or empty");
    checkConnected();

    try {
      final StringBuilder sb = new StringBuilder();
      sb.append("filter ").append(eventHeader);
      if (!isNullOrEmpty(valueToFilter)) {
        sb.append(' ');
        sb.append(valueToFilter);
      }

      final InboundClientHandler handler = (InboundClientHandler) channel.getPipeline().getLast();
      final EslMessage response = handler.sendApiSingleLineCommand(channel, sb.toString()).get();
      return new CommandResponse(sb.toString(), response);

    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  /**
   * Delete an event filter from the current set of event filters on this connection.  See
   *
   * @param eventHeader   to remove
   * @param valueToFilter to remove
   * @return a {@link CommandResponse} with the server's response.
   */
  public CommandResponse deleteEventFilter(String eventHeader, String valueToFilter) {

    checkArgument(!isNullOrEmpty(eventHeader), "eventHeader cannot be null or empty");
    checkConnected();

    try {

      final StringBuilder sb = new StringBuilder();
      sb.append("filter delete ").append(eventHeader);
      if (!isNullOrEmpty(valueToFilter)) {
        sb.append(' ');
        sb.append(valueToFilter);
      }

      final InboundClientHandler handler = (InboundClientHandler) channel.getPipeline().getLast();
      final EslMessage response = handler.sendApiSingleLineCommand(channel, sb.toString()).get();
      return new CommandResponse(sb.toString(), response);

    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  /**
   * Send a {@link SendMsg} command to FreeSWITCH.  This client requires that the {@link SendMsg}
   * has a call UUID parameter.
   *
   * @param sendMsg a {@link SendMsg} with call UUID
   * @return a {@link CommandResponse} with the server's response.
   */
  public CommandResponse sendMessage(SendMsg sendMsg) {

    checkNotNull(sendMsg, "sendMsg cannot be null");
    checkConnected();

    try {
      final InboundClientHandler handler = (InboundClientHandler) channel.getPipeline().getLast();
      final EslMessage response = handler.sendApiMultiLineCommand(channel, sendMsg.getMsgLines()).get();
      return new CommandResponse(sendMsg.toString(), response);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }

  }

  /**
   * Enable log output.
   *
   * @param level using the same values as in console.conf
   * @return a {@link CommandResponse} with the server's response.
   */
  public CommandResponse setLoggingLevel(String level) {

    checkArgument(!isNullOrEmpty(level), "level cannot be null or empty");
    checkConnected();

    try {
      final StringBuilder sb = new StringBuilder();
      sb.append("log ").append(level);

      final InboundClientHandler handler = (InboundClientHandler) channel.getPipeline().getLast();
      final EslMessage response = handler.sendApiSingleLineCommand(channel, sb.toString()).get();
      return new CommandResponse(sb.toString(), response);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  /**
   * Disable any logging previously enabled with setLogLevel().
   *
   * @return a {@link CommandResponse} with the server's response.
   */
  public CommandResponse cancelLogging() {

    checkConnected();

    try {
      final InboundClientHandler handler = (InboundClientHandler) channel.getPipeline().getLast();
      final EslMessage response = handler.sendApiSingleLineCommand(channel, "nolog").get();
      return new CommandResponse("nolog", response);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  /**
   * Close the socket connection
   *
   * @return a {@link CommandResponse} with the server's response.
   */
  public CommandResponse close() {
    checkConnected();

    try {
      final InboundClientHandler handler = (InboundClientHandler) channel.getPipeline().getLast();
      final EslMessage response = handler.sendApiSingleLineCommand(channel, "exit").get();
      return new CommandResponse("exit", response);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  /*
  *  Internal observer of the ESL protocol
  */
  private final IEslProtocolListener protocolListener = new IEslProtocolListener() {
    public void authResponseReceived(CommandResponse response) {
      authenticatorResponded.set(true);
      authenticated = response.isOk();
      authenticationResponse = response;
      log.debug("Auth response success={}, message=[{}]", authenticated, response.getReplyText());
    }

    public void eventReceived(final EslEvent event) {
      log.debug("Event received [{}]", event);
      if (event.getEventName().equals("BACKGROUND_JOB")) {
        final String backgroundUuid = event.getEventHeaders().get(EslHeaders.Name.JOB_UUID);
        final SettableFuture<EslEvent> future = backgroundJobs.remove(backgroundUuid);
        if (null != future) {
          future.set(event);
        }
      } else {
        for (final IEslEventListener listener : eventListeners) {
          listener.eventReceived(event);
        }
      }
    }

    public void disconnected() {
      log.info("Disconnected ...");
    }
  };
}
