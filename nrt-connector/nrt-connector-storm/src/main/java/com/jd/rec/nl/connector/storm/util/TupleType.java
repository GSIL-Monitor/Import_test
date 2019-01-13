package com.jd.rec.nl.connector.storm.util;

import backtype.storm.tuple.Tuple;

import static com.jd.rec.nl.connector.storm.Const.SCHEDULE_SPOUT;
import static com.jd.rec.nl.connector.storm.Const.WATCHER_SPOUT;

public abstract class TupleType {

    public static Type check(Tuple tuple) {
        if (tuple.getSourceComponent().equals(SCHEDULE_SPOUT)) {
            return Type.schedule;
        }
        if (tuple.getSourceComponent().equals(WATCHER_SPOUT)) {
            return Type.appEvent;
        }
        return Type.normal;
    }

    public enum Type {
        schedule, appEvent, normal
    }
}
