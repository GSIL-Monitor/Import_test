package com.jd.rec.nl.core.input;

import com.jd.rec.nl.core.domain.Message;
import com.jd.rec.nl.core.domain.Named;

import java.io.Serializable;

/**
 * @author linmx
 * @date 2018/5/29
 */
public interface Source<OUT extends Message> extends Named, Serializable {

    OUT get();

}
