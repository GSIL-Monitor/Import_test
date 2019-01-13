package com.jd.rec.nl.app.origin.common.kafka.parser;

import com.jd.rec.nl.core.domain.Message;
import com.jd.rec.nl.service.common.quartet.domain.ImporterContext;
import org.junit.Test;

/**
 * @author linmx
 * @date 2018/9/29
 */
public class DongGardenShowParseTest {

    @Test
    public void parse() throws Exception {
        String value = "{\"jid\":\"2895ec1beecad3e72b7b9bd994859dfbcc384531\",\"typ\":\"cl\"," +
                "\"pid\":\"NdEDWHqXFhi1ffsfn98I-w\",\"osp\":\"iphone\",\"dvc\":\"iPhone9,2\",\"osv\":\"7.1.4\"," +
                "\"clientType\":3,\"pin\":\"youran31\",\"user_site_province_name\":\"北京\",\"click_time\":\"20180929 16:04:28\"," +
                "\"app_device\":\"IOS\",\"report_ts\":\"1538208291521\",\"net\":\"4g\",\"ctm\":\"1538208268896\"," +
                "\"ctp\":\"DJXYHomeViewController\",\"lat\":\"39.673347\",\"mct\":\"iPhone\",\"chf\":\"apple_store\"," +
                "\"par\":\"{\\\"themeId\\\":\\\"56\\\",\\\"pageStyle\\\":\\\"2\\\",\\\"babelChannel\\\":\\\"356440\\\"," +
                "\\\"pageVersion\\\":\\\"1.0.0\\\"}\",\"ver\":\"5.0\",\"std\":\"JA2015_311210\",\"scr\":\"1242*2208\"," +
                "\"apv\":\"7.1.4\",\"clp\":\"1.0.0-33410068601-3-0-0-0@@1#4#abe72671-69aa-47fd-8109-22a8200e7d0e#177140_1.0.0" +
                "-21129980625-2-1-0-0@@1#4#abe72671-69aa-47fd-8109-22a8200e7d0e#177140_1.0.0-18854873294-5-0-0-0@@1#4#abe72671" +
                "-69aa-47fd-8109-22a8200e7d0e#177140_1.0.0-19264400628-4-0-0-0@@1#2#7a0495e7f16686fe4b9b6177dfe0e0d6acb62013" +
                "-115-621057#177140_1.0.0-31991213696-1-0-0-0@@1#2#7a0495e7f16686fe4b9b6177dfe0e0d6acb62013-115-621057#177140_" +
                "\",\"idfa\":\"1956BA26-253A-4235-9EE7-88B71D5D0D27\",\"jvr\":\"5.3.1\",\"clt\":\"app\"," +
                "\"cls\":\"AllGardens_Main_ProductExp\",\"proj_id\":\"1\",\"user_site_cy_name\":\"中国\"," +
                "\"report_time\":\"20180929 16:05:04\",\"lon\":\"116.111788\",\"imsi\":\"46002\",\"m_source\":\"1\"," +
                "\"ims\":\"中国移动\",\"uid\":\"2895ec1beecad3e72b7b9bd994859dfbcc384531\",\"page_id\":\"AllGardens_Main\"," +
                "\"bld\":\"164771\",\"uip\":\"124.65.83.182\",\"user_agent\":\"%E4%BA%AC%E4%B8%9C/7.1.4 CFNetwork/974.2.1 " +
                "Darwin/18.0.0\",\"machineType\":\"iPhone\",\"seq\":\"50\",\"pv_sid\":\"926\"," +
                "\"unpl\":\"V2_ZzNsbRUCRxB1CE8HZx0MDWUfF18VAF8cdgpCSC5OXlYyAxVURF8QRmlJKFRzEVQZJkB8XUNRSwklTShUehtVB2czEVxCXl8UchRGUmoZXw5iChlcR2dDJXUJR1V6Gl4HbgYibXJXQSV0OEZdexhaAW4CG1lyXxEUcwlPVHMFVVUwUA5YEAJECSZdTgRnTlVXNFZHXBICFEchOEBS|ADC_H9T5YiraKmGa+9uKWVXVt+KTDsnQPC0cJiCc7XyrNx7qgqq2PNR+Re49JjBpWBuD3VG84GIj2xGvY2DHcQ0e0wgh+JcVEabrB78Qp5rW6qoz92slCH+GLf2Q6XAmUl/545UnPh84GCWhje1iULp19OORhltOXJ8Zc+Xhf3f7KHW79ebhDGDT3JfoeKkn7pU+KGEuVsXSQnxM3yBhqceV3g1jEdy3NFaDBUlagYvxm7pTnCcnDn+jeJTAg+7T6KIXt60WUSRmzmLnkbYUMm3xAvuhCVqgQzknItOsgXmMGFdm9Dq1W8FAPlyZGmeaMBkF99SWrGuCFRE+yfcoJN6iUvcsZeUvgqelV/BlCEflN7Fdkc+VpA9KCUlP4TLCkOJ/1AANmxOVs7Wk0wgBCtmrdY6bTZ87ADuH39xxgkD0ARABaDH3bMQ5JyZadHp1uYwlGDAevRrzGvwhroArnpUU0knT+eORJjhomuhAxjfwP9e3VOe0DSPRyqAJ0Y1Pvr8vX/A3vaonEvZ7QG3cKThlsDhtijwh5yST14qLI0WeSTU7EKGWh7w74O+nI7FTZgOqmVdFA7Xni2MByWA6MUb75Rl/KWpNjDaqVoTqhi7fG5LwZgXnzxNtimNPZmjtVx7X8/pCHYmOHckztPwQxLYjfA==\",\"open_flag\":\"0\",\"rtm\":1538208304178,\"pv_seq\":\"74\",\"token\":\"8a9ab79b7999eab43356940b0d42b302\",\"user_site_city_name\":\"北京\",\"nty\":\"4g\",\"vts\":\"3234\",\"jdv\":\"0|kong|t_1000170135|tuiguang|notset|4006183638000|1535183621\"}";
        DongGardenShowParse parse = new DongGardenShowParse();
        Message message = new Message("test", value);
        ImporterContext context = parse.parse(message);
        System.out.println(context.getBehaviorField());
        System.out.println(context.getThemeId());
        System.out.println(context.getTimestamp());
    }
}