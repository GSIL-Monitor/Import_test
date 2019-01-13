package com.jd.rec.nl.core.pubsub;

import com.google.inject.Singleton;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 事件广播者
 *
 * @author linmx
 * @date 2018/7/13
 */
@Singleton
public class EventPublisher {

    private BlockingQueue<ApplicationEvent> events = new ArrayBlockingQueue<>(200);

    /**
     * 事件消费线程
     */
    private ExecutorService publishExecutor = Executors.newSingleThreadExecutor();

    public EventPublisher() {
        publishExecutor.submit(() -> {
            while (true) {
                ApplicationEvent event = events.take();
                EventListenerRegister.getMatchedListeners(event.getClass())
                        .forEach(applicationListener -> applicationListener.onEvent(event));
            }
        });
    }

    /**
     * 广播事件
     *
     * @param event
     */
    public void publishEvent(ApplicationEvent event) throws InterruptedException {
        if (!events.contains(event)) {
            events.put(event);
        }
    }
}
