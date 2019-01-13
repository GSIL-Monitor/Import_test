package com.jd.rec.nl.service.modules.user.domain;

import com.jd.zeus.convert.model.ConvertInfo;
import org.junit.Test;

import java.util.Map;

/**
 * @author linmx
 * @date 2018/7/18
 */
public class UserProfileTest {

    @Test
    public void test(){
        ConvertInfo convertInfoInit = ConvertInfo.InitializeUDPMappings();
        Map<String, ConvertInfo> colMap = convertInfoInit.getColumnMap();
    }

}