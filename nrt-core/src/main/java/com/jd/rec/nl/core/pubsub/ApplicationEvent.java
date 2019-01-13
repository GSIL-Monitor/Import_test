package com.jd.rec.nl.core.pubsub;

/**
 * @author linmx
 * @date 2018/7/13
 */
public abstract class ApplicationEvent implements java.io.Serializable {

    /**
     * 时间戳
     */
    protected final long timestamp;

    /**
     * 来源
     */
    protected final String source;

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public ApplicationEvent(String source) {
        this.source = source;
        this.timestamp = System.currentTimeMillis();
    }

    public ApplicationEvent() {
        this(Thread.currentThread().getName());
    }

    public String getSource() {
        return source;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ApplicationEvent)) {
            return false;
        }
        return this.timestamp == ((ApplicationEvent) obj).getTimestamp() && this.source.equals(((ApplicationEvent) obj)
                .getSource());
    }
}
