package org.freeswitch.esl.client.outbound;

import org.freeswitch.esl.client.transport.event.EslEvent;

public interface IClientHandler {

  public void handleEslEvent(Context ctx, EslEvent event);

  public void onConnect(Context ctx, EslEvent event);

}
