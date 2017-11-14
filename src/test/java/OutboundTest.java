import com.google.common.base.Throwables;

import org.freeswitch.esl.client.dptools.Execute;
import org.freeswitch.esl.client.dptools.ExecuteException;
import org.freeswitch.esl.client.inbound.Client;
import org.freeswitch.esl.client.internal.Context;
import org.freeswitch.esl.client.outbound.IClientHandler;
import org.freeswitch.esl.client.outbound.SocketClient;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.freeswitch.esl.client.transport.message.EslHeaders.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

public class OutboundTest {
    private static Logger logger = LoggerFactory.getLogger(OutboundTest.class);
    private static String sb = "/usr/local/freeswitch/sounds/en/us/callie/ivr/8000/";
    String prompt = sb + "ivr-please_enter_extension_followed_by_pound.wav";
    String failed = sb + "ivr-that_was_an_invalid_entry.wav";
    public static void main(String[] args) {
        new OutboundTest();
    }

    public OutboundTest() {
        try {

            final Client inboudClient = new Client();
            inboudClient.connect(new InetSocketAddress("localhost", 8021), "ClueCon", 10);
            inboudClient.addEventListener((ctx, event) -> logger.info("INBOUND onEslEvent: {}", event.getEventName()));

            final SocketClient outboundServer = new SocketClient(
                    new InetSocketAddress("localhost", 8084),
                    () -> new IClientHandler() {
                        @Override
                        public void onConnect(Context context,
                                EslEvent eslEvent) {


                            logger.warn(nameMapToString(eslEvent
                                    .getMessageHeaders(), eslEvent.getEventBodyLines()));

                            String uuid = eslEvent.getEventHeaders()
                                    .get("unique-id");

                            logger.warn(
                                    "Creating execute app for uuid {}",
                                    uuid);

                            Execute exe = new Execute(context, uuid);

                            try {

                                exe.answer();

                                String digits = exe.playAndGetDigits(3,
                                        5, 10, 10 * 1000, "#", prompt,
                                        failed, "^\\d+", 10 * 1000);
                                logger.warn("Digits collected: {}",
                                        digits);


                            } catch (ExecuteException e) {
                                logger.error(
                                        "Could not prompt for digits",
                                        e);

                            } finally {
                                try {
                                    exe.hangup(null);
                                } catch (ExecuteException e) {
                                    logger.error(
                                            "Could not hangup",e);
                                }
                            }

                        }

                        @Override
                        public void onEslEvent(Context ctx,
                                EslEvent event) {
                            logger.info("OUTBOUND onEslEvent: {}",
                                    event.getEventName());

                        }
                    });
            outboundServer.startAsync();

        } catch (Throwable t) {
            Throwables.propagate(t);
        }
    }

    public static String nameMapToString(Map<Name, String> map,
            List<String> lines) {
        StringBuilder sb = new StringBuilder("\nHeaders:\n");
        for (Name key : map.keySet()) {
            if(key == null)
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
}
