package com.jd.rec.nl.connector.storm;

public class Const {
    //region const
    public static final String KEY_FIELD_NAME = "key";
    public static final String VALUE_FIELD_NAME = "value";
    public static final String TIMESTAMP = "timestamp";

    public static final String KEYED_STREAM_ID = "keyedUpdater";
    public static final String WINDOW_STREAM_ID = "windowCollector";
    public static final String REDUCER_STREAM_ID = "reducer";
    public static final String EXPORTER_STREAM_ID = "exporter";
    public static final String TRACE_STREAM_ID = "trace";

    public static final String SEP_TAB = "\t";
    public static final String SEP_COMMA = ",";
    public static final String SEP_COLON = ":";
    //endregion

    //region bolt and spout names
    public static final String SCHEDULE_SPOUT = "ScheduleSpout";
    public static final String WATCHER_SPOUT = "WatcherSpout";
    public static final String PRE_BOLT = "PreBolt";
    public static final String APP_BOLT = "AppBolt";
    public static final String WINDOW_BOLT = "WindowBolt";
    public static final String REDUCE_BOLT = "ReduceBolt";
    public static final String SAVER_BOLT = "SaverBolt";
    public static final String KEYED_APP_BOLT = "KeyedAppBolt_";
    public static final String TRACE_BOLT = "TraceBolt";
}
