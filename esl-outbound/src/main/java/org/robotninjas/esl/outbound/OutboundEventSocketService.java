package org.robotninjas.esl.outbound;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.AbstractService;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.socket.ServerSocketChannelFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class OutboundEventSocketService extends AbstractService {

  private final ServerSocketChannelFactory channelFactory;
  private final ConnectionHandlerFactory handlerFactory;
  private final Executor callbackPool;
  private final ServerBootstrap bootstrap;
  private Optional<Channel> channel;

  @Inject
  public OutboundEventSocketService(ServerSocketChannelFactory channelFactory,
                                    ConnectionHandlerFactory handlerFactory,
                                    @Named("listenAddress")SocketAddress listenAddress) {
    this.channelFactory = channelFactory;
    this.handlerFactory = handlerFactory;
    this.callbackPool = Executors.newCachedThreadPool();
    this.bootstrap = new ServerBootstrap(channelFactory);
    bootstrap.setOption("listenAddress", listenAddress);
    bootstrap.setPipelineFactory(new OutboundChannelPipelineFactory(handlerFactory, callbackPool));
    this.channel = Optional.absent();
  }

  @Override
  protected void doStart() {
    try {
      channel = Optional.of(bootstrap.bind());
      notifyStarted();
    } catch (Throwable t) {
      notifyFailed(t);
      Throwables.propagate(t);
    }
  }

  @Override
  protected void doStop() {
    if (channel.isPresent()) {
      channel.get().close();
      notifyStopped();
    }
  }

}
