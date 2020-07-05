package org.freeswitch.esl.client;

import com.google.common.base.Throwables;
import org.freeswitch.esl.client.inbound.Client;
import org.freeswitch.esl.client.internal.IModEslApi.EventFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

import static com.google.common.base.Throwables.throwIfUnchecked;

public class ClientExample {
    private static final Logger L = LoggerFactory.getLogger(ClientExample.class);

    public static void main(String[] args) {
        try {
/*            if (args.length < 1) {
                System.out.println("Usage: java ClientExample PASSWORD");
                return;
            }

            String password = args[0];*/

            String password = "ClueCon";

            Client client = new Client();

            client.addEventListener((ctx, event) -> L.info("Received event: {}", event.getEventName()));

            client.connect(new InetSocketAddress("localhost", 8021), password, 10);
            client.setEventSubscriptions(EventFormat.PLAIN, "all");

            client.sendApiCommand("originate user/1000", null);

            System.out.println("done!");

        } catch (Throwable t) {
            throwIfUnchecked(t);
        }
    }
}
