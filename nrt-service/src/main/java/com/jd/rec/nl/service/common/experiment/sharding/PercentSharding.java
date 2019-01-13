package com.jd.rec.nl.service.common.experiment.sharding;

import com.jd.rec.nl.core.domain.Named;
import com.jd.rec.nl.core.experiment.sharding.Sharding;
import com.jd.si.util.MurmurHash;
import com.typesafe.config.Config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author linmx
 * @date 2018/5/18
 */
public class PercentSharding implements Sharding<String> {

    List<ClosedInterval> slots = new ArrayList<>();

    @Override
    public boolean match(String in) {
        int condition = Math.abs(MurmurHash.hash32(in)) % 100;
        for (ClosedInterval slot : slots) {
            if (slot.contain(condition)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean matchConfig(Config config, Named named) {
        return config.hasPath("percents");
    }

    @Override
    public PercentSharding newInstance(Config config, Named named) {
        List percents = config.getAnyRefList("percents");
        PercentSharding percentSharding = new PercentSharding();
        percents.stream().forEach(list -> {
            ClosedInterval closedInterval = new ClosedInterval(((List<Integer>) list).get(0), ((List<Integer>) list).get(1));
            percentSharding.slots.add(closedInterval);
        });
        return percentSharding;
    }


    class ClosedInterval implements Serializable {
        private int begin = -1;
        private int end = -1;

        ClosedInterval(int begin, int end) {
            this.begin = begin;
            this.end = end;
        }

        int getBegin() {
            return begin;
        }

        int getEnd() {
            return end;
        }

        boolean contain(int code) {
            return code >= begin && code <= end;
        }

        @Override
        public String toString() {
            return "ClosedInterval{"
                    + "begin=" + begin
                    + ", end=" + end
                    + '}';
        }
    }
}
