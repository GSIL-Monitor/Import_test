package com.jd.rec.nl.service.modules.user.domain;

import com.jd.rec.nl.core.exception.WrongConfigException;
import com.jd.rec.nl.core.utils.StringUtils;

import java.time.Duration;

/**
 * @author linmx
 * @date 2018/9/6
 */
public class RequiredBehavior implements Comparable<RequiredBehavior> {

    BehaviorField type;

    Duration period;

    Integer limit = -1;

    public RequiredBehavior(BehaviorField type, String period, Integer limit) {
        this.type = type;
        if (limit == null && period == null) {
            throw new WrongConfigException("behavior required limit or period");
        }
        if (limit != null) {
            this.limit = limit;
        }
        if (period != null) {
            this.period = StringUtils.parseTypesafeDuration(period);
        }
    }

    public Duration getPeriod() {
        return period;
    }

    public BehaviorField getType() {
        return type;
    }

    public Integer getLimit() {
        return limit;
    }

    @Override
    public int hashCode() {
        return this.type.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RequiredBehavior)) {
            return false;
        }
        return ((RequiredBehavior) obj).getType() == this.getType();
    }

    @Override
    public String toString() {
        return type.getFieldName().concat("-").concat(period.toString());
    }

    @Override
    public int compareTo(RequiredBehavior o) {
        if (period.equals(o.getPeriod())) {
            long ret = this.limit - o.getLimit();
            return ret > 0 ? 1 : (ret == 0 ? 0 : -1);
        } else {
            return period.compareTo(o.getPeriod());
        }
    }
}
