package org.freeswitch.esl.client.outbound;

import org.freeswitch.esl.client.inbound.IEslEventListener;
import org.freeswitch.esl.client.internal.Context;
import org.freeswitch.esl.client.transport.event.EslEvent;

public interface IClientHandler extends IEslEventListener {
	void onConnect(Context ctx, EslEvent event);
}
