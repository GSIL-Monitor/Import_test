package com.jd.rec.nl.core.pubsub;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author linmx
 * @date 2018/11/6
 */
public class EventListenerRegister {

    protected static Map<Class<? extends ApplicationEvent>, Set<ApplicationListener>> listeners = new HashMap<>();

    /**
     * 注册监听器
     *
     * @param listener
     */
    public static void register(ApplicationListener listener) {
        Class eventType = listener.eventType();
        synchronized (listeners) {
            if (listeners.containsKey(eventType)) {
                listeners.get(eventType).add(listener);
            } else {
                Set<ApplicationListener> applicationListeners = new HashSet<>();
                applicationListeners.add(listener);
                listeners.put(eventType, applicationListeners);
            }
        }
    }

    public static Set<ApplicationListener> getMatchedListeners(Class<? extends ApplicationEvent> eventClass) {
        return listeners.get(eventClass);
    }
}
