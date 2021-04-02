package org.freeswitch.esl.client;

import org.freeswitch.esl.client.dptools.Execute;
import org.freeswitch.esl.client.dptools.ExecuteException;
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
import java.util.regex.Pattern;

import static com.google.common.base.Throwables.throwIfUnchecked;

public class OutboundDTMFTest {
    private static Logger logger = LoggerFactory.getLogger(OutboundDTMFTest.class);
    private static String sb = "/usr/local/freeswitch/sounds/en/us/callie/ivr/8000/";
    String prompt = sb + "ivr-please_enter_extension_followed_by_pound.wav";
    String failed = sb + "ivr-that_was_an_invalid_entry.wav";

    public static void main(String[] args) {
        new OutboundDTMFTest();
    }

    public OutboundDTMFTest() {
        try {
            //outbound test
            final SocketClient outboundServer = new SocketClient(
                    new InetSocketAddress("localhost", 8086),
                    new OutboundHandlerFactory());
            outboundServer.startAsync();
        } catch (Throwable t) {
            throwIfUnchecked(t);
        }
    }


    public class OutboundHandlerFactory implements IClientHandlerFactory {

        @Override
        public IClientHandler createClientHandler() {
            //just for sample , recommend use singleton pattern, to avoid new too many instance
            return new OutboundHandler();
        }
    }


    public class OutboundHandler implements IClientHandler {

        StringBuffer buffer = new StringBuffer(10);
        String pattern1 = "^\\d+";
        String pattern2 = "^\\d+#$";

        @Override
        public void onConnect(Context context, EslEvent eslEvent) {
            try {
                Execute exe = new Execute(context, "");

                //订阅DTMF事件
                EslMessage eslMessage = context.sendCommand("event plain DTMF");
                if (eslMessage.getHeaderValue(Name.REPLY_TEXT).startsWith("+OK")) {
                    logger.info("subscribe event success!");
                }

                exe.answer();

                int timeOutSeconds = 30;

                //放音采集
                exe.playAndGetDigits(1,
                        1, 10, timeOutSeconds * 1000, "#", prompt,
                        failed, pattern1, timeOutSeconds * 1000);

                //等待用户输入按键
                long start = System.currentTimeMillis();
                while (true) {
                    if (System.currentTimeMillis() - start > timeOutSeconds * 1000) {
                        break;
                    }

                    if (buffer.length() > 0 && Pattern.matches(pattern2, buffer.toString())) {
                        break;
                    }

                    Thread.sleep(50);
                }

                System.out.println("you pressed:" + buffer.toString());

            } catch (ExecuteException | InterruptedException e) {
                logger.error("Could not prompt for digits", e);
            } finally {
                context.closeChannel();
            }

        }

        @Override
        public void onEslEvent(Context ctx, EslEvent event) {
//            System.out.println(event.getEventName());
            if (event.getEventName().equalsIgnoreCase("DTMF")) {
                String key = event.getEventHeaders().get("DTMF-Digit");
                if ("#".equalsIgnoreCase(key)) {
                    //检查是否输入正确（如果错误，请将之前输入的清空掉）
                    if (!Pattern.matches(pattern1, buffer.toString())) {
                        buffer.setLength(0);
                        return;
                    }
                }
                buffer.append(key);
            }
        }
    }
}
