package org.freeswitch.esl.client;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.freeswitch.esl.client.dptools.Execute;
import org.freeswitch.esl.client.dptools.ExecuteException;
import org.freeswitch.esl.client.inbound.Client;
import org.freeswitch.esl.client.internal.Context;
import org.freeswitch.esl.client.outbound.IClientHandler;
import org.freeswitch.esl.client.outbound.IClientHandlerFactory;
import org.freeswitch.esl.client.outbound.SocketClient;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.freeswitch.esl.client.transport.message.EslHeaders.Name;
import org.freeswitch.esl.client.transport.message.EslMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static com.google.common.base.Throwables.throwIfUnchecked;

public class InboundAndOutboundTest {
    private static Logger logger = LoggerFactory.getLogger(InboundAndOutboundTest.class);
    private static String music = "/usr/local/freeswitch/sounds/music/8000/ponce-preludio-in-e-major.wav";

    private static ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
            .setNameFormat("outbound-test-%d").build();

    private static ExecutorService poolExecutor = new ThreadPoolExecutor(4, 512,
            1000L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());

    public static void main(String[] args) {
        new InboundAndOutboundTest();
    }

    public InboundAndOutboundTest() {
        try {
            //outbound test
            final SocketClient outboundServer = new SocketClient(
                    new InetSocketAddress("localhost", 8086),
                    new OutboundHandlerFactory());
            outboundServer.startAsync();

            //inbound test
//            final Client inboundClient = new Client();
//            inboundClient.connect(new InetSocketAddress("localhost", 8021), "ClueCon", 10);
//            inboundClient.addEventListener((ctx, event) -> logger.info("INBOUND onEslEvent: {}", event.getEventName()));
//            inboundClient.sendApiCommand("originate user/1000", null);

        } catch (Throwable t) {
            throwIfUnchecked(t);
        }
    }

    public static String nameMapToString(Map<Name, String> map,
                                         List<String> lines) {
        StringBuilder sb = new StringBuilder("\nHeaders:\n");
        for (Name key : map.keySet()) {
            if (key == null)
                continue;
            sb.append(key.toString());
            sb.append("\n\t\t\t\t = \t ");
            sb.append(map.get(key));
            sb.append("\n");
        }
        if (lines != null) {
            sb.append("Body Lines:\n");
            for (String line : lines) {
                sb.append(line);
                sb.append("\n");
            }
        }
        return sb.toString();
    }


    public class OutboundHandlerFactory implements IClientHandlerFactory {

        @Override
        public IClientHandler createClientHandler() {
            //just for sample , recommend use singleton pattern, to avoid new too many instance
            return new OutboundHandler();
        }
    }


    public class OutboundHandler implements IClientHandler {

        private Map<String, Boolean> checkMusic = new ConcurrentHashMap<>();

        @Override
        public void onConnect(Context context, EslEvent eslEvent) {

            long threadId = Thread.currentThread().getId();
            System.out.println(threadId);


            poolExecutor.submit(() -> {

                logger.warn(nameMapToString(eslEvent.getMessageHeaders(), eslEvent.getEventBodyLines()));

                String uuid = eslEvent.getEventHeaders().get("Unique-ID");
                logger.info("Creating execute app for uuid {}", uuid);
                Execute exe = new Execute(context, uuid);


//                boolean hangup = false;
                try {

                    System.out.println("onConnect=>" + Thread.currentThread().getId());
                    //subscribe event
                    EslMessage eslMessage = context.sendCommand("event plain ALL");
                    if (eslMessage.getHeaderValue(Name.REPLY_TEXT).startsWith("+OK")) {
                        logger.info("subscribe event success!");
                    }

                    //linger 10 seconds
                    eslMessage = context.sendCommand("linger 10");
                    if (eslMessage.getHeaderValue(Name.REPLY_TEXT).startsWith("+OK")) {
                        logger.info("linger success!");
                    }

                    String dest = eslEvent.getEventHeaders().get("Channel-Destination-Number");
                    if (!dest.startsWith("400")) {
                        exe.bridge("user/" + dest);
                        return;
                    }

                    exe.answer();


                    exe.playback(music);
                    String musicKey = uuid + ":" + music;
                    checkMusic.put(musicKey, false);

                    // check music is over ?
                    long timeout = 60 * 5 * 1000;
                    long start = System.currentTimeMillis();
                    while (true) {
                        if (checkMusic.get(musicKey)) {
                            checkMusic.remove(musicKey);
                            exe.hangup("playback over ,auto hangup");
//                            hangup = true;
                            break;
                        }
                        long elapsed = System.currentTimeMillis() - start;
                        if (elapsed > timeout) {
                            logger.warn("music playback timeout!");
                            break;
                        }
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                } catch (ExecuteException e) {
                    logger.error("Could not prompt for digits", e);
                } finally {
//                    try {
//                        if (!hangup) {
//                            exe.hangup(null);
//                        }
//                    } catch (ExecuteException e) {
//                        logger.error("Could not hangup", e);
//                    }
                    context.closeChannel();
                }
            });

        }

        @Override
        public void onEslEvent(Context ctx, EslEvent event) {
//            long threadId = Thread.currentThread().getId();
//            System.out.println(threadId);

//            System.out.println("onEslEvent=>" + Thread.currentThread().getId());
            logger.info("OUTBOUND onEslEvent: {}", event.getEventName());
            if (event.getEventName().equalsIgnoreCase("PLAYBACK_STOP")) {
                String uuid = event.getEventHeaders().get("Unique-ID");
                String music = event.getEventHeaders().get("Playback-File-Path");
                String musicKey = uuid + ":" + music;
                if (checkMusic.containsKey(musicKey)) {
                    checkMusic.put(musicKey, true);
                }
            }

            if (event.getEventName().equalsIgnoreCase("CHANNEL_HANGUP_COMPLETE")) {
                int hashCode = this.hashCode();
                logger.info(hashCode + ":" + this.toString());
                checkMusic.clear();
            }
        }
    }
}
