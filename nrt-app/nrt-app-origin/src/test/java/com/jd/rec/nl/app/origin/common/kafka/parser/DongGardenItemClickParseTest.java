package com.jd.rec.nl.app.origin.common.kafka.parser;

import com.jd.rec.nl.core.domain.Message;
import com.jd.rec.nl.service.common.quartet.domain.ImporterContext;
import org.junit.Test;

/**
 * @author linmx
 * @date 2018/9/29
 */
public class DongGardenItemClickParseTest {

    @Test
    public void test() throws Exception {
        String value = "{\"jid\":\"868454036658334-449ef9e6e35f\",\"typ\":\"cl\",\"pid\":\"xU9DwlHzS24ouxe8OEu4a7V9-x-f3wj7\",\"osp\":\"android\",\"dvc\":\"vivo X21UD A\",\"osv\":\"8.1.0\",\"clientType\":3,\"pin\":\"jd_689a0e4f9a7fd\",\"user_site_province_name\":\"云南\",\"click_time\":\"20180929 11:54:45\",\"app_device\":\"ANDROID\",\"report_ts\":\"1538193285956\",\"net\":\"wifi\",\"ctm\":\"1538193285254\",\"ctp\":\"com.jd.lib.jdcustomchannel.view.activity.JDFamilyActivity\",\"lat\":\"24.965923\",\"mct\":\"vivo\",\"chf\":\"vivo\",\"par\":\"{\\\"themeId\\\":\\\"1498\\\",\\\"testId\\\":1,\\\"matStyle\\\":\\\"1.0.0\\\",\\\"pageStyle\\\":\\\"1\\\",\\\"babelChannel\\\":\\\"356440\\\",\\\"did\\\":\\\"491369\\\",\\\"ord\\\":1,\\\"alid\\\":\\\"2\\\",\\\"clickType\\\":1,\\\"pageVersion\\\":\\\"1.0.0\\\",\\\"matId\\\":\\\"19430129048\\\",\\\"alclk\\\":\\\"28dafca24c97ee886ab414e8d9c471dabfeea613-101-621057\\\",\\\"skuIndex\\\":1,\\\"udi\\\":\\\"N\\\",\\\"top\\\":\\\"Y\\\"}\",\"ver\":\"5.2.0\",\"std\":\"JA2015_311210\",\"scr\":\"2154*1080\",\"apv\":\"7.1.6\",\"clp\":\"0_0\",\"jvr\":\"5.3.3\",\"clt\":\"app\",\"cls\":\"AllGardens_Main_Product\",\"proj_id\":\"1\",\"user_site_cy_name\":\"中国\",\"report_time\":\"20180929 11:54:46\",\"aid\":\"01d2159552518aa1\",\"imi\":\"868454036658334\",\"lon\":\"102.684106\",\"imsi\":\"460110550878024\",\"ims\":\"46011\",\"uid\":\"868454036658334-449ef9e6e35f\",\"page_id\":\"AllGardens_Main\",\"bld\":\"62280\",\"uip\":\"220.163.33.178\",\"user_agent\":\"Dalvik/2.1.0 (Linux; U; Android 8.1.0; vivo X21UD A Build/OPM1.171019.011)\",\"machineType\":\"vivo X21UD A\",\"seq\":\"7\",\"pv_sid\":\"50\",\"unpl\":\"null\",\"rtm\":1538193286178,\"pv_seq\":\"7\",\"token\":\"4a518dd0a1d7df918d3a140ecfbfdcad\",\"user_site_city_name\":\"昆明\",\"nty\":\"wifi\",\"vts\":\"69\",\"jdv\":\"0|kong|t_1000170135|tuiguang|notset|1537631538807|1537631538\"}";
        DongGardenItemClickParse parse = new DongGardenItemClickParse();
        Message message = new Message("test", value);
        ImporterContext context = parse.parse(message);
        System.out.println(context.getBehaviorField());
        System.out.println(context.getThemeId());
        System.out.println(context.getTimestamp());
    }


}