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
package org.freeswitch.esl.client.internal;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.freeswitch.esl.client.transport.message.EslHeaders;
import org.freeswitch.esl.client.transport.message.EslHeaders.Name;
import org.freeswitch.esl.client.transport.message.EslHeaders.Value;
import org.freeswitch.esl.client.transport.message.EslMessage;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.transform;
import static org.jboss.netty.channel.Channels.close;

/**
 * Specialised {@link ChannelUpstreamHandler} that implements the logic of an ESL connection that
 * is common to both inbound and outbound clients. This
 * handler expects to receive decoded {@link EslMessage} or {@link EslEvent} objects. The key
 * responsibilities for this class are:
 * <ul><li>
 * To synthesise a synchronous command/response api.  All IO operations using the underlying Netty
 * library are intrinsically asynchronous which provides for excellent response and scalability.  This
 * class provides for a blocking wait mechanism for responses to commands issued to the server.  A
 * key assumption here is that the FreeSWITCH server will process synchronous requests in the order they
 * are received.
 * </li><li>
 * Concrete sub classes are expected to 'terminate' the Netty IO processing pipeline (ie be the 'last'
 * handler).
 * </li></ul>
 * Note: implementation requirement is that an {@link ExecutionHandler} is placed in the processing
 * pipeline prior to this handler. This will ensure that each incoming message is processed in its
 * own thread (although still guaranteed to be processed in the order of receipt).
 */
public abstract class AbstractEslClientHandler extends SimpleChannelUpstreamHandler {

	public static final String MESSAGE_TERMINATOR = "\n\n";
	public static final String LINE_TERMINATOR = "\n";

	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	// used to preserve association between adding future to queue and sending message on channel
	private final ReentrantLock syncLock = new ReentrantLock();
	private final ConcurrentLinkedQueue<SettableFuture<EslMessage>> apiCalls =
		new ConcurrentLinkedQueue<SettableFuture<EslMessage>>();

	private final ConcurrentHashMap<String, SettableFuture<EslEvent>> backgroundJobs =
		new ConcurrentHashMap<String, SettableFuture<EslEvent>>();
	private final ExecutorService backgroundJobExecutor = Executors.newCachedThreadPool();

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {

		for (final SettableFuture<EslMessage> apiCall : apiCalls) {
			apiCall.setException(e.getCause());
		}

		for (final SettableFuture<EslEvent> backgroundJob : backgroundJobs.values()) {
			backgroundJob.setException(e.getCause());
		}

		close(ctx.getChannel());

		ctx.sendUpstream(e);

	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		if (e.getMessage() instanceof EslMessage) {
			final EslMessage message = (EslMessage) e.getMessage();
			final String contentType = message.getContentType();
			if (contentType.equals(Value.TEXT_EVENT_PLAIN) ||
				contentType.equals(Value.TEXT_EVENT_XML)) {
				//  transform into an event
				final EslEvent eslEvent = new EslEvent(message);
				if (eslEvent.getEventName().equals("BACKGROUND_JOB")) {
					final String backgroundUuid = eslEvent.getEventHeaders().get(EslHeaders.Name.JOB_UUID);
					final SettableFuture<EslEvent> future = backgroundJobs.remove(backgroundUuid);
					if (null != future) {
						future.set(eslEvent);
					}
				} else {
					handleEslEvent(ctx, eslEvent);
				}
			} else {
				handleEslMessage(ctx, (EslMessage) e.getMessage());
			}
		} else {
			throw new IllegalStateException("Unexpected message type: " + e.getMessage().getClass());
		}
	}

	protected void handleEslMessage(ChannelHandlerContext ctx, EslMessage message) {
		log.info("Received message: [{}]", message);
		final String contentType = message.getContentType();

		if (contentType.equals(Value.API_RESPONSE)) {
			log.debug("Api response received [{}]", message);
			apiCalls.poll().set(message);
		} else if (contentType.equals(Value.COMMAND_REPLY)) {
			log.debug("Command reply received [{}]", message);
			apiCalls.poll().set(message);
		} else if (contentType.equals(Value.AUTH_REQUEST)) {
			log.debug("Auth request received [{}]", message);
			handleAuthRequest(ctx);
		} else if (contentType.equals(Value.TEXT_DISCONNECT_NOTICE)) {
			log.debug("Disconnect notice received [{}]", message);
			handleDisconnectionNotice();
		} else {
			log.warn("Unexpected message content type [{}]", contentType);
		}
	}

	/**
	 * Synthesise a synchronous command/response by creating a callback object which is placed in
	 * queue and blocks waiting for another IO thread to process an incoming {@link EslMessage} and
	 * attach it to the callback.
	 *
	 * @param channel
	 * @param command single string to send
	 * @return the {@link EslMessage} attached to this command's callback
	 */
	public ListenableFuture<EslMessage> sendApiSingleLineCommand(Channel channel, final String command) {
		final SettableFuture<EslMessage> future = SettableFuture.create();
		try {
			syncLock.lock();
			apiCalls.add(future);
			channel.write(command + MESSAGE_TERMINATOR);
		} finally {
			syncLock.unlock();
		}

		return future;

	}

	/**
	 * Sends a FreeSWITCH API command to the channel and blocks, waiting for an immediate response from the
	 * server.
	 * <p/>
	 * The outcome of the command from the server is returned in an {@link EslMessage} object.
	 *
	 * @param channel
	 * @param command API command to send
	 * @param arg     command arguments
	 * @return an {@link EslMessage} containing command results
	 */
	public ListenableFuture<EslMessage> sendSyncApiCommand(Channel channel, String command, String arg) {

		checkArgument(!isNullOrEmpty(command), "command may not be null or empty");
		checkArgument(!isNullOrEmpty(arg), "arg may not be null or empty");

		return sendApiSingleLineCommand(channel, "api " + command + ' ' + arg);
	}

	/**
	 * Synthesise a synchronous command/response by creating a callback object which is placed in
	 * queue and blocks waiting for another IO thread to process an incoming {@link EslMessage} and
	 * attach it to the callback.
	 *
	 * @param channel
	 * @return the {@link EslMessage} attached to this command's callback
	 */
	public ListenableFuture<EslMessage> sendApiMultiLineCommand(Channel channel, final List<String> commandLines) {
		//  Build command with double line terminator at the end
		final StringBuilder sb = new StringBuilder();
		for (final String line : commandLines) {
			sb.append(line);
			sb.append(LINE_TERMINATOR);
		}
		sb.append(LINE_TERMINATOR);

		final SettableFuture<EslMessage> future = SettableFuture.create();
		try {
			syncLock.lock();
			apiCalls.add(future);
			channel.write(sb.toString());
		} finally {
			syncLock.unlock();
		}

		return future;

	}

	/**
	 * Returns the Job UUID of that the response event will have.
	 *
	 * @param channel
	 * @param command
	 * @return Job-UUID as a string
	 */
	public ListenableFuture<EslEvent> sendBackgroundApiCommand(Channel channel, final String command) {

		final ListenableFuture<EslMessage> backgroundIdFuture = sendApiSingleLineCommand(channel, command);
		final AsyncFunction<EslMessage, EslEvent> transformFunction =
			new AsyncFunction<EslMessage, EslEvent>() {
				@Override
				public ListenableFuture<EslEvent> apply(EslMessage result) throws Exception {
					if (result.hasHeader(Name.JOB_UUID)) {
						final String jobId = result.getHeaderValue(Name.JOB_UUID);
						final SettableFuture<EslEvent> resultFuture = SettableFuture.create();
						backgroundJobs.put(jobId, resultFuture);
						return resultFuture;
					} else {
						return immediateFailedFuture(new IllegalStateException("Missing Job-UUID header in bgapi response"));
					}
				}
			};

		return transform(backgroundIdFuture, transformFunction, backgroundJobExecutor);

	}

	protected abstract void handleEslEvent(ChannelHandlerContext ctx, EslEvent event);

	protected abstract void handleAuthRequest(ChannelHandlerContext ctx);

	protected abstract void handleDisconnectionNotice();

}
