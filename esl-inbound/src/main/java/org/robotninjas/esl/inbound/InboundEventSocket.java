package org.robotninjas.esl.inbound;

import com.google.common.base.Optional;
import com.google.common.net.HostAndPort;
import com.google.inject.Inject;
import org.freeswitch.esl.client.AbstractEventSocket;
import org.freeswitch.esl.client.EventSocketConnection;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;

public class InboundEventSocket extends AbstractEventSocket {

  private final ClientSocketChannelFactory channelFactory;
  private Optional<EventSocketConnection> conn;

  @Inject
  public InboundEventSocket(ClientSocketChannelFactory channelFactory) {
    super();
    this.channelFactory = channelFactory;
    this.conn = Optional.absent();
  }

  @Override
  public EventSocketConnection getConnection() {
    return conn.get();
  }

  public void connect(HostAndPort freeswitch, String password) throws InterruptedException {
    final ClientBootstrap bootstrap = new ClientBootstrap(channelFactory);
    bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
      @Override
      public ChannelPipeline getPipeline() throws Exception {
        return null;
      }
    });
  }

}
