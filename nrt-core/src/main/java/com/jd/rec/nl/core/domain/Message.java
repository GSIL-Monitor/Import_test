package com.jd.rec.nl.core.domain;

import java.io.Serializable;

/**
 * 用于包装外部系统的输入
 *
 * @author linmx
 * @date 2018/5/24
 */
public class Message<T> implements Serializable {

    private String messageSource;

    private T messageValue;

    public Message(String messageSource, T messageValue) {
        this.messageSource = messageSource;
        this.messageValue = messageValue;
    }

    @Override
    public String toString() {
        return "Message{" +
                "messageSource='" + messageSource + '\'' +
                ", messageValue=" + messageValue +
                '}';
    }

    public String getMessageSource() {
        return messageSource;
    }

    public void setMessageSource(String messageSource) {
        this.messageSource = messageSource;
    }

    public T getMessageValue() {
        return messageValue;
    }

    public void setMessageValue(T messageValue) {
        this.messageValue = messageValue;
    }

}
