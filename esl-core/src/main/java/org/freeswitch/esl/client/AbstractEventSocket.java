package org.freeswitch.esl.client;

import com.google.common.base.Supplier;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.freeswitch.esl.client.transport.CommandResponse;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.freeswitch.esl.client.transport.message.EslHeaders;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.base.Throwables.propagateIfInstanceOf;
import static com.google.common.util.concurrent.Futures.transform;

public abstract class AbstractEventSocket implements EslEventListener, EventSocket {

  private final Multimap<EventType, EventListener> listeners;
  private final ConcurrentMap<UUID, SettableFuture<EslEvent>> backgroundJobs;

  public abstract EventSocketConnection getConnection();

  @Inject
  public AbstractEventSocket() {

    this.listeners = Multimaps.newMultimap(
      Maps.<EventType, Collection<EventListener>>newHashMap(),
      new Supplier<Collection<EventListener>>() {
        @Override
        public Collection<EventListener> get() {
          return new CopyOnWriteArraySet<EventListener>();
        }
      }
    );

    this.backgroundJobs = Maps.newConcurrentMap();
    addEventListener(EventTypes.BACKGROUND_JOB, new EventListener() {
      @Override
      public void handle(EslEvent e) {
        backgroundJobs.remove(EslHeaders.Name.JOB_UUID).set(e);
      }
    });

  }

  @Override
  public CommandResponse makeApiCall(Command cmd, String args) throws InterruptedException, IOException {
    try {
      return Futures.getUnchecked(getConnection().api(cmd, args));
    } catch (Throwable t) {
      propagateIfInstanceOf(t, IOException.class);
      propagateIfInstanceOf(t, InterruptedException.class);
      throw propagate(t);
    }
  }

  @Override
  public ListenableFuture<EslEvent> makeBackgroundApiCall(Command cmd, String args) {
    try {

      return transform(getConnection().bgapi(cmd, args), new AsyncFunction<CommandResponse, EslEvent>() {
        @Override
        public ListenableFuture<EslEvent> apply(CommandResponse response) throws Exception {
          final SettableFuture<EslEvent> eventSettable = SettableFuture.create();
          final UUID uuid = UUID.fromString(response.getResponse().getHeaderValue(EslHeaders.Name.JOB_UUID));
          backgroundJobs.put(uuid, eventSettable);
          return eventSettable;
        }
      });

    } catch (Throwable t) {
      throw propagate(t);
    }
  }

  @Override
  public CommandResponse sendMessage(SendMsg msg) {
    return null;
  }

  @Override
  public void addEventListener(EventType event, EventListener listener) {
    synchronized (listeners) {
      final boolean addFilter = !listeners.containsKey(event);
      listeners.put(event, listener);
      if (addFilter) {
        getConnection().filter(EventHeaders.EVENT_TYPE, event.toString());
      }
    }
  }

  @Override
  public void removeEventListener(EventType event, EventListener listener) {
    synchronized (listeners) {
      listeners.remove(event, listener);
      if (!listeners.containsKey(event)) {
        getConnection().filterdelete(EventHeaders.EVENT_TYPE, event.toString());
      }
    }
  }


  @Override
  public void handleEslEvent(EslEvent e) {
    for (final EventListener listener : listeners.get(e.getEventType())) {
      listener.handle(e);
    }
  }
}
