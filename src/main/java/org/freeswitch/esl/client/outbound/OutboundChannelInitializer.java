package org.freeswitch.esl.client.outbound;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.freeswitch.esl.client.transport.message.EslFrameDecoder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class OutboundChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final IClientHandlerFactory clientHandlerFactory;
    private final ExecutorService callbackExecutor = Executors.newSingleThreadExecutor();
    private final EventExecutorGroup customLogicExecutor;

    public OutboundChannelInitializer(IClientHandlerFactory clientHandlerFactory, int outboundEventThreadCount) {
        this.clientHandlerFactory = clientHandlerFactory;
        this.customLogicExecutor = new DefaultEventExecutorGroup(outboundEventThreadCount, new OutboundClientEventThreadFactory());
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        // Add the text line codec combination first
        pipeline.addLast("encoder", new StringEncoder());
        // Note that outbound mode requires the decoder to treat many 'headers' as body lines
        pipeline.addLast("decoder", new EslFrameDecoder(8092, true));

        // now the outbound client logic
        pipeline.addLast(this.customLogicExecutor, "clientHandler",
                new OutboundClientHandler(
                        clientHandlerFactory.createClientHandler(),
                        callbackExecutor));
    }

    static class OutboundClientEventThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        OutboundClientEventThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = "outboundClientEventExecutor-" +
                    poolNumber.getAndIncrement() +
                    "-thread-";
        }

        public Thread newThread(Runnable runnable) {
            Thread rabbitmqTaskWorker = new Thread(group, runnable,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (rabbitmqTaskWorker.isDaemon())
                rabbitmqTaskWorker.setDaemon(false);
            if (rabbitmqTaskWorker.getPriority() != Thread.NORM_PRIORITY)
                rabbitmqTaskWorker.setPriority(Thread.NORM_PRIORITY);
            return rabbitmqTaskWorker;
        }
    }
}
