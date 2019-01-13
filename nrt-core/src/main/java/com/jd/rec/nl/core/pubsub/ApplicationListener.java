package com.jd.rec.nl.core.pubsub;

import java.util.EventListener;

/**
 * 应用监听器
 *
 * @author linmx
 * @date 2018/7/13
 */
public interface ApplicationListener<E extends ApplicationEvent> extends EventListener {

    /**
     * 触发事件
     *
     * @param event
     */
    void onEvent(E event);

    /**
     * 监听的事件类型
     *
     * @return
     */
    Class<E> eventType();
}
