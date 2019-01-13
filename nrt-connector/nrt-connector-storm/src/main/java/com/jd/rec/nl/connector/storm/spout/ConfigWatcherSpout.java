package com.jd.rec.nl.connector.storm.spout;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import com.jd.rec.nl.core.guice.InjectorService;
import com.jd.rec.nl.service.common.experiment.ParameterChangeEvent;
import com.jd.rec.nl.service.infrastructure.ConfigWatcher;
import com.jd.rec.nl.service.infrastructure.ConfigWatcher.ChangeType;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static com.jd.rec.nl.connector.storm.Const.TIMESTAMP;
import static com.jd.rec.nl.connector.storm.Const.VALUE_FIELD_NAME;
import static org.slf4j.LoggerFactory.getLogger;

public class ConfigWatcherSpout extends BaseRichSpout {

    public static final String eventSource = "watcherSpout";

    private static final Logger LOGGER = getLogger(ConfigWatcherSpout.class);

    private ConfigWatcher configWatcher;

    private SpoutOutputCollector outputCollector;

    @Override
    public void open(Map map, TopologyContext topologyContext, SpoutOutputCollector spoutOutputCollector) {
        this.outputCollector = spoutOutputCollector;
        configWatcher = InjectorService.getCommonInjector().getInstance(ConfigWatcher.class);
        configWatcher.registerAppChangeListener(changedValue -> {
            ChangeType changeType = changedValue.getType();
            String path = changedValue.getPath();
            String data = changedValue.getValue();
            int version = changedValue.getVersion();
            LOGGER.debug("{} : {} --> {} , {}", changeType, path, data, version);
            if (changeType == ChangeType.disableApp) {
                String appName = path.lastIndexOf("/") < 0 ? path : path.substring(path.lastIndexOf("/"));
                Map configMap = new HashMap();
                configMap.put("placementId", 0);
                configMap.put("expId", 0);
                configMap.put("enable", false);
                ParameterChangeEvent event =
                        new ParameterChangeEvent(eventSource, appName, 0, 0, ConfigFactory.parseMap(configMap), -1);
                outputCollector.emit(new Values(event, System.currentTimeMillis()));
            } else if (changeType == ChangeType.enableExp || changeType == ChangeType.experimentChange) {
                String[] paths = path.split("/");
                if (paths.length < 2) {
                    LOGGER.error("change value is wrong:{}", changedValue.toString());
                    return;
                }
                String appName = paths[paths.length - 2];
                String[] node = paths[paths.length - 1].split("-");
                ParameterChangeEvent event = new ParameterChangeEvent(eventSource, appName, Long.parseLong(node[0]),
                        Integer.parseInt(node[1]), ConfigFactory.parseString(data), version);
                outputCollector.emit(new Values(event, System.currentTimeMillis()));
            } else if (changeType == ChangeType.disableExp) {
                String[] paths = path.split("/");
                if (paths.length < 2) {
                    LOGGER.error("change value is wrong:{}", changedValue.toString());
                    return;
                }
                String appName = paths[paths.length - 2];
                String[] node = paths[paths.length - 1].split("-");
                Map configMap = new HashMap();
                configMap.put("placementId", Long.parseLong(node[0]));
                configMap.put("expId", Integer.parseInt(node[1]));
                configMap.put("enable", false);
                ParameterChangeEvent event = new ParameterChangeEvent(eventSource, appName, Long.parseLong(node[0]),
                        Integer.parseInt(node[1]), ConfigFactory.parseMap(configMap), -1);
                outputCollector.emit(new Values(event, System.currentTimeMillis()));
            }
        });
    }

    @Override
    public void nextTuple() {
        try {
            Thread.sleep(100L);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        Fields sigFields = new Fields(VALUE_FIELD_NAME, TIMESTAMP);
        outputFieldsDeclarer.declare(sigFields);
    }
}
