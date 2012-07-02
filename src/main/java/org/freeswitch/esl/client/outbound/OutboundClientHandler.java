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
package org.freeswitch.esl.client.outbound;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import org.freeswitch.esl.client.internal.AbstractEslClientHandler;
import org.freeswitch.esl.client.internal.Context;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.freeswitch.esl.client.transport.message.EslMessage;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.handler.execution.ExecutionHandler;

import java.util.concurrent.ExecutorService;

import static com.google.common.util.concurrent.Futures.addCallback;

/**
 * Specialised {@link AbstractEslClientHandler} that implements the base connecction logic for an
 * 'Outbound' FreeSWITCH Event Socket connection.  The responsibilities for this class are:
 * <ul><li>
 * To send a 'connect' command when the FreeSWITCH server first establishes a new connection with
 * the socket client in Outbound mode.  This will result in an incoming {@link EslMessage} that is
 * transformed into an {@link EslEvent} that sub classes can handle.
 * </ul>
 * Note: implementation requirement is that an {@link ExecutionHandler} is placed in the processing
 * pipeline prior to this handler. This will ensure that each incoming message is processed in its
 * own thread (although still guaranteed to be processed in the order of receipt).
 *
 * @author david varnes
 */
class OutboundClientHandler extends AbstractEslClientHandler {

  private final IClientHandler clientHandler;
  private final ExecutorService callbackExecutor;

  public OutboundClientHandler(IClientHandler clientHandler, ExecutorService callbackExecutor) {
    this.clientHandler = clientHandler;
    this.callbackExecutor = callbackExecutor;
  }

  @Override
  public void channelConnected(final ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    // Have received a connection from FreeSWITCH server, send connect response
    log.debug("Received new connection from server, sending connect message");

    final ListenableFuture<EslMessage> connectFuture = sendApiSingleLineCommand(ctx.getChannel(), "connect");

    addCallback(
      connectFuture,
      new FutureCallback<EslMessage>() {
        @Override
        public void onSuccess(EslMessage response) {
          final EslEvent channelDataEvent = new EslEvent(response, true);
          handleConnectResponse(ctx.getChannel(), channelDataEvent);
        }

        @Override
        public void onFailure(Throwable throwable) {
          ctx.getChannel().close();
          handleDisconnectionNotice();
        }
      }
    );
  }

  void handleConnectResponse(final Channel channel, final EslEvent event) {
    callbackExecutor.execute(new Runnable() {
      @Override
      public void run() {
        clientHandler.onConnect(new Context(channel, OutboundClientHandler.this), event);
      }
    });
  }

  @Override
  protected void handleEslEvent(final ChannelHandlerContext ctx, final EslEvent event) {
    callbackExecutor.execute(new Runnable() {
      @Override
      public void run() {
        clientHandler.onEslEvent(new Context(ctx.getChannel(), OutboundClientHandler.this), event);
      }
    });
  }

  @Override
  protected void handleAuthRequest(ChannelHandlerContext ctx) {
    // This should not happen in outbound mode
    log.warn("Auth request received in outbound mode, ignoring");
  }

  @Override
  protected void handleDisconnectionNotice() {
    log.debug("Received disconnection notice");
  }
}
