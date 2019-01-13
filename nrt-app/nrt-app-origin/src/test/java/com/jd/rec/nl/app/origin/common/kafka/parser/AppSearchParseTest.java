package com.jd.rec.nl.app.origin.common.kafka.parser;

import org.junit.Test;

import java.net.URLDecoder;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @version :v1.0
 * @authour :zhangencheng
 * @date :2018/11/16
 */
public class AppSearchParseTest {
    private static final Pattern vPattern = Pattern.compile("key_word=([^\\$]*)\\$\\S+");
    public final static String CHARSET = "utf-8";
    @Test
    public void getKeywordFromUrl() throws Exception{
      //  String v = "key_word%3d%25CA%25D6%25B5%25E7%25CD%25B2$hc_cid1s%3d1318%3b1620%3b9855%3b$hc_cid2s%3d1462%3b1623%3b9856%3b$hc_cid3s%3d1476%3b1654%3b9902%3b$query_type%3dquery_default$query_flag%3d$query_info%3d1_34";
        String v = "key_word%253D%2525D1%2525A9%2525B5%2525D8%2525D1%2525A5%2525C5%2525AE%252520%2525BE%2525A9%2525B6%2525AB%2525D7%2525D4%2525D3%2525AA%2524sir_key%253D%2525D1%2525A9%2525B5%2525D8%2525D1%2525A5%2525C5%2525AE%2524coupon_sid%253D%2524activity_id%253D%2524isRedEnvelope%253D%2524hc_cid1s%253D11729%253B%2524hc_cid2s%253D11731%253B%2524hc_cid3s%253D6920%253B9776%253B6919%253B%2524query_type%253D%2524query_flag%253D%2524query_info%253D%2524mtest%253Drelevproductv2rank%252Cmtest_2_a2%252Cmobile_7_0%252Cmodelrank_adjust%252Cp13n_pairwise_imp%252Chottag_table_ver1%252Cfinery_disable_ranker_op%252Cmobile_8_0%252Cfeature_log%252Cmtestopab%252Cp13n_pairwise_clock%252Cp13n_737_base%252Cp13n_670_exp_conv%252Cp13n_6728_v1%252Cp13n_relevance_label_v3%252Ccvr_base%252Cp13n_652_eb_v1%252Cp13n_12473_click%252Cp13n_9855_v1%252Cp13n_9847_base%252Cp13n_cosmetics_act_only%252Cp13n_1319_v4%252Cp13n_6196_v2%252Cp13n_1318_v3%252Cp13n_9192_v4%252Cgroup_buyt%252Cp13n_1315_attr_embd%252Cp13n_turn_base1%252Crelev_30fea_v5_rank%252Crelev_tail_base%252Crelev_30fea_v3_rank%252Cp13n_15248_v3%252Cp13n_recall_v3%252Cp13n_1320_b%252Cp13n_12218_b%252Cp13n_12259_b%252Cmobile_xinpin_base%252Cp13n_gender_b2%252Cp13n_6233_v4%252Cp13n_1672_v4%252Carticle_daren_insert%2524user_psn_flag%253D262144%2524mtest_act%253D11%252C12%252C17%252C28%252C29%252C%2524filt_type%253Dsku_state%253A1%253Bproductext2%253Ab15v0%253Bredisstore%253A12%253Blocationid%253A12%252C978%252C4459%252C36511%253B%2524logid%253D013f4d8f07824ba7b82fa51267f9a3d2%2524pvid%253D2351f66b0c674245b2963aef3468ce8e%2524deviceid%253D342460832%2524page%253D17%2524pagesize%253D10%2524forcebot%253D%2524client%253Dandroid%2524client_version%253D7.2.3%2524hot_tags%253D%2524hot_tags2%253D%2524my_search_filter%253D%2524sort_type%253Dsort_default%2524wids_info%253D100001560028%252C70334288008%252C934%252C100%252C189.00%252C1%252C%252C1%252C1%252C189.00%253B100000341675%252C70351065224%252C933%252C99%252C189.00%252C1%252C%252C1%252C1%252C189.00%253B100000564208%252C70351065224%252C929%252C92%252C259.00%252C1%252C%252C1%252C1%252C259.00%253B100000969534%252C70351065216%252C922%252C46%252C298.00%252C1%252C%252C1%252C1%252C298.00%253B100000478418%252C70351065216%252C920%252C5%252C249.00%252C1%252C%252C1%252C1%252C249.00%253B100000970508%252C70351065216%252C920%252C48%252C259.00%252C1%252C%252C1%252C1%252C259.00%253B100000259226%252C70334288008%252C917%252C97%252C239.00%252C1%252C%252C1%252C1%252C239.00%253B100000826698%252C70351065216%252C916%252C5%252C499.00%252C1%252C%252C1%252C1%252C499.00%253B100000903944%252C70334288008%252C914%252C130%252C149.90%252C1%252C%252C1%252C1%252C149.90%253B100000543763%252C70351065216%252C907%252C5%252C249.00%252C1%252C%252C1%252C1%252C249.00%253B%2524feature_log%253Dnull%25253Bnull%25253Bnull%25253Bnull%25253Bnull%25253Bnull%25253Bnull%25253Bnull%25253Bnull%25253Bnull%25253B%2524timestamp%253D1542364794399%2524merge_sku_type%253Dmergesku_not_hit_cate%2524show_word_one%253D%2524requery_search%253D0%2524plus_moniter%253D0%2524filter_wids_info%253D%2524expand_query%253DNULL%2524url%253D%253Fkey%253D%2525D1%2525A9%2525B5%2525D8%2525D1%2525A5%2525C5%2525AE%252520%2525BE%2525A9%2525B6%2525AB%2525D7%2525D4%2525D3%2525AA%2526gz%253Dyes%2526merge_sku%253Dyes%2526log_id%253D013f4d8f07824ba7b82fa51267f9a3d2%2526pvid%253D2351f66b0c674245b2963aef3468ce8e%2526uuid%253D864168037197464-6091f33a9c62%2526re%253Dyes%2526oc%253Dyes%2526multi_suppliers%253Dyes%2526shop_col%253Dyes%2526area_ids%253D12%252C978%252C4459%252C36511%2526user_pin%253Djd_4811e42d6029a%2526page%253D17%2526pagesize%253D10%2526qp_disable%253Dno%2526article_tab%253Dyes%2526at_filt_type%253Darticle_type%25252CL2M1%25253Bnot_style%25252CL2M2%25253Bapp_limit%2526storetab%253Dyes%2526client%253D1431503009201%2526filt_type%253Dredisstore%252C12%253B%253Bproductext2%252Cb15v0%253Bsku_state%252C1%2526expression_key%253D%2526shoptab_valid%253Dyes%2526article_daren%253Dyes%2526delivertime%253Dyes%2526onebox_mod%253D1%2526auction%253Dyes%2526shop_multi%253Dyes%2526sc%253Dyes%2526hottag_fl%253Dyes%2526coordinates%253D31.760851%252C120.111535%2526sender_col%253Dyes%2526client_version%253D7.2.3%2526lightspeed%253Dyes%2526enc_url_gbk%253Dyes%2526brand_col%253Dyes%2526price_col%253Dyes%2526color_col%253Dyes%2526size_col%253Dyes%2526ext_attr%253Dyes%2526ext_attr_sort%253Dyes%2526new_brand_val%253Dyes%2526oldware%253Dyes%2526exist_col%253Dyes$cdt=3";
        v = URLDecoder.decode(v, CHARSET);
        int index = v.indexOf("key_word");
        if (!v.substring((index + 8)).startsWith("=")) {
            v = URLDecoder.decode(v, CHARSET);
        }
        Matcher matcher = vPattern.matcher(v);
        if (matcher.find()) {
            String parseKey = matcher.group(1);
            parseKey = URLDecoder.decode(parseKey, "GB18030");
            System.out.println(parseKey);
        }

    }

    @Test
    public void test1(){
        String value="i \t am \t xiaoming";
        String[] msg = value.split("\t");

        if (msg.length==3) {
            for (int i = 0; i <3 ; i++) {
                System.out.println(msg[i]);
            }
        }
    }

    @Test
    public void testIndex(){
        Set<String> rbcids=new HashSet<>();
        rbcids.add("{\"v\":\"龙润茶\"}");
        rbcids.add("{\"v\":\"龙润w茶\"}");
        rbcids.forEach(value->{
            value=value.substring(value.indexOf(":")+2,value.length()-2);
            System.out.println(value);
        });
    }
@Test
    public void testkey(){
        String table="1";
    Collection<String> keys=new HashSet<>();
    keys.add("a");
    keys.add("b");
    keys.stream().map(key -> key.concat("~~~pw-").concat(table)).collect(Collectors.toList());
    System.out.println();
}

}
