package org.robotninjas.esl.inbound;

import com.google.common.base.Optional;
import com.google.common.net.HostAndPort;
import com.google.inject.Inject;
import org.freeswitch.esl.client.AbstractEventSocket;
import org.freeswitch.esl.client.EventSocketConnection;
import org.freeswitch.esl.client.EventSocketConnectionImpl;
import org.freeswitch.esl.client.transport.message.EslFrameDecoder;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.handler.codec.string.StringEncoder;

import java.io.IOException;
import java.net.InetSocketAddress;

public class InboundEventSocketImpl extends AbstractEventSocket implements InboundEventSocket {

  private final ClientSocketChannelFactory channelFactory;
  private Optional<ClientBootstrap> bootstrap;
  private Optional<EventSocketConnectionImpl> conn;

  @Inject
  public InboundEventSocketImpl(ClientSocketChannelFactory channelFactory) {
    super();
    this.channelFactory = channelFactory;
    this.bootstrap = Optional.absent();
    this.conn = Optional.absent();
  }

  @Override
  public EventSocketConnection getConnection() {
    return conn.get();
  }

  @Override
  public void connect(HostAndPort freeswitch, String password) throws InterruptedException {

    this.bootstrap = Optional.of(new ClientBootstrap(channelFactory));
    bootstrap.get().setPipelineFactory(new ChannelPipelineFactory() {
      @Override
      public ChannelPipeline getPipeline() throws Exception {
        final ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("encoder", new StringEncoder());
        pipeline.addLast("decoder", new EslFrameDecoder(8192));
        return pipeline;
      }
    });

    final InetSocketAddress address = InetSocketAddress.createUnresolved(freeswitch.getHostText(), freeswitch.getPort());
    final ChannelFuture connectFuture = bootstrap.get().connect(address);

    connectFuture.sync();

    final Channel channel = connectFuture.getChannel();
    this.conn = Optional.of(new EventSocketConnectionImpl(channel));
    channel.getPipeline().addLast("handler", this.conn.get());

    this.conn.get().addEventListener(this);
    this.conn.get().login(password).wait();

  }

  @Override
  public void close() throws IOException {
    if (conn.isPresent()) {
      conn.get().close();
      bootstrap.get().releaseExternalResources();
    }
  }
}
