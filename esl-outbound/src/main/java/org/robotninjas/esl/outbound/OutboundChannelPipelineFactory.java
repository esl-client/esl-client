package org.robotninjas.esl.outbound;

import org.freeswitch.esl.client.EventSocketConnectionImpl;
import org.freeswitch.esl.client.transport.message.EslFrameDecoder;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.string.StringEncoder;

import java.util.concurrent.Executor;

class OutboundChannelPipelineFactory implements ChannelPipelineFactory {

  private final ConnectionHandlerFactory handlerFactory;
  private final Executor callbackPool;

  OutboundChannelPipelineFactory(ConnectionHandlerFactory handlerFactory, Executor callbackPool) {
    this.handlerFactory = handlerFactory;
    this.callbackPool = callbackPool;
  }

  @Override
  public ChannelPipeline getPipeline() throws Exception {
    final ChannelPipeline pipeline = Channels.pipeline();
    pipeline.addLast("encoder", new StringEncoder());
    pipeline.addLast("decoder", new EslFrameDecoder(8192));
    pipeline.addLast("handler", new SimpleChannelUpstreamHandler() {
      @Override
      public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        final EventSocketConnectionImpl conn = new EventSocketConnectionImpl(e.getChannel());
        final OutboundEventSocketImpl oesl = new OutboundEventSocketImpl(conn);
        ctx.getPipeline().addLast("handler", conn);
        ctx.sendUpstream(e);
        final ConnectionHandler handler = handlerFactory.newOutboundConnectionHandler();
        callbackPool.execute(new Runnable() {
          @Override
          public void run() {
            handler.handleOutboundConnection(oesl);
          }
        });
      }
    });
    return pipeline;
  }
}
