package org.freeswitch.esl.client.outbound;

import org.freeswitch.esl.client.internal.AbstractEslClientHandler;
import org.freeswitch.esl.client.transport.SendMsg;
import org.jboss.netty.channel.Channel;

public class Context {

  private final AbstractEslClientHandler clientHandler;
  private final Channel channel;

  public Context(Channel channel, AbstractEslClientHandler clientHandler) {
    this.clientHandler = clientHandler;
    this.channel = channel;
  }

  public void sendMessage(SendMsg msg) {
    clientHandler.sendApiMultiLineCommand(channel, msg.getMsgLines());
  }

}
