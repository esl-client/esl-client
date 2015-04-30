/*
 * Copyright 2010 david varnes.
 *
 * Licensed under the Apache License, version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.freeswitch.esl.client.outbound;

import com.google.common.util.concurrent.AbstractService;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.concurrent.Executors;

/**
 * Entry point to run a socket client that a running FreeSWITCH Event Socket Library module can
 * make outbound connections to.
 * <p/>
 * This class provides for what the FreeSWITCH documentation refers to as 'Outbound' connections
 * from the Event Socket module. That is, with reference to the module running on the FreeSWITCH
 * server, this client accepts an outbound connection from the server module.
 * <p/>
 * See <a href="http://wiki.freeswitch.org/wiki/Mod_event_socket">http://wiki.freeswitch.org/wiki/Mod_event_socket</a>
 */
public class SocketClient extends AbstractService {

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private final ChannelFactory channelFactory;
	private final IClientHandlerFactory clientHandlerFactory;
	private final SocketAddress bindAddress;

	private Channel serverChannel;

	public SocketClient(SocketAddress bindAddress, IClientHandlerFactory clientHandlerFactory) {
		this.bindAddress = bindAddress;
		this.clientHandlerFactory = clientHandlerFactory;
		this.channelFactory = new NioServerSocketChannelFactory(
			Executors.newCachedThreadPool(),
			Executors.newCachedThreadPool());
	}

	@Override
	protected void doStart() {
		final ServerBootstrap bootstrap = new ServerBootstrap(channelFactory);
		bootstrap.setPipelineFactory(new OutboundPipelineFactory(clientHandlerFactory));
		bootstrap.setOption("child.tcpNoDelay", true);
		bootstrap.setOption("child.keepAlive", true);
		serverChannel = bootstrap.bind(bindAddress);
		notifyStarted();
		log.info("SocketClient waiting for connections on [{}] ...", bindAddress);
	}

	@Override
	protected void doStop() {
		if (null != serverChannel) {
			serverChannel.close().awaitUninterruptibly();
		}
		channelFactory.releaseExternalResources();
		notifyStopped();
		log.info("SocketClient stopped");
	}

}
