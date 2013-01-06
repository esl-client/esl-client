package org.robotninjas.esl.outbound;

import com.google.inject.PrivateModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import org.jboss.netty.channel.socket.ServerSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import java.net.SocketAddress;

import static java.util.concurrent.Executors.newCachedThreadPool;

public class OutboundEventSocketModule extends PrivateModule {

  private final ServerSocketChannelFactory channelFactory;
  private final Class<? extends ConnectionHandler> connectionHandlerClass;
  private final SocketAddress listenAddress;

  public OutboundEventSocketModule(Class<? extends ConnectionHandler> connectionHandlerClass,
                                   SocketAddress listenAddress) {

    this.channelFactory = new NioServerSocketChannelFactory(newCachedThreadPool(), newCachedThreadPool());
    this.connectionHandlerClass = connectionHandlerClass;
    this.listenAddress = listenAddress;
  }

  public OutboundEventSocketModule(Class<? extends ConnectionHandler> connectionHandlerClass,
                                   ServerSocketChannelFactory channelFactory,
                                   SocketAddress listenAddress) {

    this.channelFactory = channelFactory;
    this.connectionHandlerClass = connectionHandlerClass;
    this.listenAddress = listenAddress;
  }

  @Override
  protected void configure() {

    bind(SocketAddress.class).annotatedWith(Names.named("listenAddress")).toInstance(listenAddress);

    install(new FactoryModuleBuilder()
      .implement(ConnectionHandler.class, connectionHandlerClass)
      .build(ConnectionHandlerFactory.class));

    bind(ServerSocketChannelFactory.class).toInstance(channelFactory);
    bind(OutboundEventSocketService.class);
    expose(OutboundEventSocket.class);

  }
}
