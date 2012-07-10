package org.freeswitch.esl.client.dptools;

import org.freeswitch.esl.client.internal.IModEslApi;
import org.freeswitch.esl.client.transport.SendMsg;

public class DpTools {

	private final IModEslApi api;

	public DpTools(IModEslApi api) {
		this.api = api;
	}

	public DpTools answer() {
		api.sendMessage(new SendMsg().addCallCommand("answer"));
		return this;
	}

}
