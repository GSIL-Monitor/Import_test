package com.jd.rec.nl.connector.storm;

import backtype.storm.Config;
import backtype.storm.StormSubmitter;
import backtype.storm.spout.SchemeAsMultiScheme;
import backtype.storm.topology.BoltDeclarer;
import backtype.storm.topology.IRichSpout;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import com.jd.rec.nl.connector.storm.bolt.*;
import com.jd.rec.nl.connector.storm.domain.InitializeParameters;
import com.jd.rec.nl.connector.storm.spout.ConfigWatcherSpout;
import com.jd.rec.nl.connector.storm.spout.ScheduleSpout;
import com.jd.rec.nl.core.config.ConfigBase;
import com.jd.rec.nl.core.input.SourceInfo;
import com.jd.rec.nl.core.input.domain.SourceConfig;
import com.jd.rec.nl.service.base.quartet.domain.KafkaConfig;
import com.jd.rec.nl.service.common.quartet.ApplicationLoader;
import storm.kafka.KafkaSpout;
import storm.kafka.SpoutConfig;
import storm.kafka.StringScheme;
import storm.kafka.ZkHosts;

import java.util.ArrayList;
import java.util.List;

import static com.jd.rec.nl.connector.storm.Const.*;


public class DefaultStormRuntime {

    static final String APP_ID = "NRT/UserProfile";


    public static void main(String[] args) throws Exception {

        final InitializeParameters params = new InitializeParameters(args);

        ApplicationLoader loader = new ApplicationLoader();
        loader.load();

        final TopologyBuilder topologyBuilder = new TopologyBuilder();

        final List<String> spoutNames = createSpouts(topologyBuilder, loader, params);

        createBolts(topologyBuilder, spoutNames, loader, params);

        Config config = configure(params);

        StormSubmitter.submitTopology("nrt-origin", config, topologyBuilder.createTopology());
        //                LocalCluster cluster = new LocalCluster();
        //                cluster.submitTopology("storm", config, topologyBuilder.createTopology());

    }

    static List<String> createSpouts(final TopologyBuilder topologyBuilder, final ApplicationLoader loader, final
    InitializeParameters params) {
        List<String> spoutNames = new ArrayList<>();

        final SourceInfo sourceInfo = loader.getSourceInfo();
        for (SourceConfig sourceConfig : sourceInfo.getConfigs()) {
            KafkaConfig kafkaConfig = (KafkaConfig) sourceConfig;
            final String kafkaZk = kafkaConfig.getKafkaZk();
            final List<String> offsetZk = kafkaConfig.getOffsetZk();
            final int offsetZkPort = kafkaConfig.getOffsetPort();
            final String topic = kafkaConfig.getTopic();
            // 并行度与kafka分片数保持一致,因此在配置中事先确定好
            final int parallelism = kafkaConfig.getParallelism();
            final IRichSpout spout = newKafkaSpout(kafkaZk, offsetZk, offsetZkPort, topic);
            topologyBuilder.setSpout(topic, spout, parallelism);
            spoutNames.add(topic);
        }
        return spoutNames;
    }

    static void createBolts(final TopologyBuilder topologyBuilder, List<String> spoutNames,
                            final ApplicationLoader loader, final InitializeParameters params) {

        BoltDeclarer boltDeclarer = topologyBuilder.setBolt(PRE_BOLT, new PreBolt(loader), params
                .getPreBolts());
        for (String spoutName : spoutNames) {
            boltDeclarer.localOrShuffleGrouping(spoutName);
        }

        BoltDeclarer appBoltDC = topologyBuilder.setBolt(APP_BOLT, new AppBolt(loader), params.getAppBolts())
                .fieldsGrouping(PRE_BOLT, new Fields(KEY_FIELD_NAME));

        // 定时spout
        ScheduleSpout scheduleSpout = null;

        if (loader.hasSchedule()) {
            scheduleSpout = new ScheduleSpout();
            topologyBuilder.setSpout(SCHEDULE_SPOUT, scheduleSpout, 1);
            appBoltDC.allGrouping(SCHEDULE_SPOUT);
        }

        BoltDeclarer saveBoltDC = topologyBuilder.setBolt(SAVER_BOLT, new SaverBolt(loader), params.getSaverBolts())
                .fieldsGrouping(APP_BOLT, EXPORTER_STREAM_ID, new Fields(KEY_FIELD_NAME));


        // 监控config变化的spout
        BaseRichSpout watcherSpout = new ConfigWatcherSpout();
        topologyBuilder.setSpout(WATCHER_SPOUT, watcherSpout, 1);
        appBoltDC.allGrouping(WATCHER_SPOUT);
        saveBoltDC.allGrouping(WATCHER_SPOUT);

//        TraceBolt traceBolt = new TraceBolt();
//        BoltDeclarer traceBoltDC = topologyBuilder.setBolt(TRACE_BOLT, traceBolt, params.getTraceBolts())
//                .shuffleGrouping(APP_BOLT, TRACE_STREAM_ID).shuffleGrouping(SAVER_BOLT, TRACE_STREAM_ID);

        // 配置windowed reduce bolt
        BoltDeclarer windowBoltDC = null;
        if (loader.hasWindow()) {

            // 窗口操作是一种特殊的定时操作，因此复用scheduleSpout，此判断是为了防止没有schedule的情况
            if (scheduleSpout == null) {
                scheduleSpout = new ScheduleSpout();
                topologyBuilder.setSpout(SCHEDULE_SPOUT, scheduleSpout, 1);
            }

            // 前置操作包括 appBolt、scheduleSpout和watcherSpout
            WindowBolt windowBolt = new WindowBolt(loader);
            windowBoltDC = topologyBuilder.setBolt(WINDOW_BOLT, windowBolt, params.getWindowBolts())
                    .fieldsGrouping(APP_BOLT, WINDOW_STREAM_ID, new Fields(KEY_FIELD_NAME))
                    .allGrouping(SCHEDULE_SPOUT)
                    .allGrouping(WATCHER_SPOUT);

            // 后置操作固定有saveBolt
            saveBoltDC.fieldsGrouping(WINDOW_BOLT, EXPORTER_STREAM_ID, new Fields(KEY_FIELD_NAME));
//            traceBoltDC.shuffleGrouping(WINDOW_BOLT, TRACE_STREAM_ID);
        }

        if (loader.hasReduce()) {
            ReduceBolt reduceBolt = new ReduceBolt(loader, params.getWindowBolts());
            // 前置操作为windowBolt和watcherSpout
            topologyBuilder.setBolt(REDUCE_BOLT, reduceBolt, params.getReducerBolts())
                    .fieldsGrouping(WINDOW_BOLT, REDUCER_STREAM_ID, new Fields(KEY_FIELD_NAME))
                    .allGrouping(WINDOW_BOLT, REDUCER_STREAM_ID.concat(WindowBolt.signal))
                    .allGrouping(WATCHER_SPOUT);

            // 后置操作固定有saveBolt
            saveBoltDC.fieldsGrouping(REDUCE_BOLT, new Fields(KEY_FIELD_NAME));
//            traceBoltDC.shuffleGrouping(REDUCE_BOLT, TRACE_STREAM_ID);
        }


        for (int i = 1; i <= loader.keyedUpdaterTier(); i++) {
            String parallelismParma = params.getParam("k".concat(String.valueOf(i)));
            int parallelism = params.getAppBolts();
            if (parallelismParma != null) {
                parallelism = Integer.parseInt(parallelismParma);
            }
            KeyedStreamAppBolt keyedStreamAppBolt = new KeyedStreamAppBolt(loader, i);
            BoltDeclarer keyedBoltDC =
                    topologyBuilder.setBolt(KEYED_APP_BOLT.concat(String.valueOf(i)), keyedStreamAppBolt, parallelism)
                            .allGrouping(WATCHER_SPOUT)
                            .fieldsGrouping(i == 1 ? APP_BOLT : KEYED_APP_BOLT.concat(String.valueOf(i - 1)),
                                    KEYED_STREAM_ID,
                                    new Fields(KEY_FIELD_NAME));
            if (loader.hasSchedule()) {
                keyedBoltDC.allGrouping(SCHEDULE_SPOUT);
            }
            if (loader.hasWindow()) {
                windowBoltDC.fieldsGrouping(KEYED_APP_BOLT.concat(String.valueOf(i)), WINDOW_STREAM_ID,
                        new Fields(KEY_FIELD_NAME));
            }
            saveBoltDC.fieldsGrouping(KEYED_APP_BOLT.concat(String.valueOf(i)), EXPORTER_STREAM_ID,
                    new Fields(KEY_FIELD_NAME));
        }
    }

    static Config configure(InitializeParameters params) {

        final Config config = new backtype.storm.Config();
        int ackTaskNum = params.getAckTaskNum();
        if (ackTaskNum >= 0) {
            config.setNumAckers(ackTaskNum);
        }
        config.setNumWorkers(params.getWorkers());
        config.setMessageTimeoutSecs(params.getTimeout());
        config.setMaxSpoutPending(params.getMaxPending());
        config.put("k8s.image.jdk.version", "jdk8");

        return config;
    }

    static KafkaSpout newKafkaSpout(final String hosts, final List<String> offsetZK, int offsetPort,
                                    final String topic) {
        final String root = "/" + APP_ID + "/" + topic,
                id = ConfigBase.getSystemConfig().getString("appName");
        //                id = UUID.randomUUID().toString();
        final SpoutConfig conf = new SpoutConfig(new ZkHosts(hosts), topic, root, id);

        conf.zkServers = offsetZK;
        conf.zkPort = offsetPort;
        conf.maxOffsetBehind = 500000000;
        conf.scheme = new SchemeAsMultiScheme(new StringScheme());
        return new KafkaSpout(conf);
    }

}
