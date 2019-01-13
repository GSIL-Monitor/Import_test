package com.jd.rec.nl.app.origin.modules.promotionalburst;

import com.google.gson.Gson;
import org.junit.Test;
import recsys.prediction_service.BurstFeature;

import java.util.Random;

/**
 * @author linmx
 * @date 2018/8/24
 */
public class AccumulateUVAndScoreTest {

    @Test
    public void test() {
        Msg msg = new Msg(0, 0, 0);
        msg = count(2, 0, msg);
        System.out.println("gap:0-->".concat(msg.toString()));
        msg = count(1, 0, msg);
        System.out.println("gap:0-->".concat(msg.toString()));
        msg = count(5, 1, msg);
        System.out.println("gap:1-->".concat(msg.toString()));
    }

    @Test
    public void test1() {
        Random random = new Random();
        for (int i = 0; i < 100; i++)
            System.out.println(random.nextInt(3));
    }

    @Test
    public void testGson() {
        BurstFeature.BurstFeatureInfo.Builder builder = BurstFeature.BurstFeatureInfo.newBuilder();
        builder.setTime(System.currentTimeMillis() / 1000);
        builder.setHc(new Double(0.121 * 10000).intValue());
        builder.setItemId(123545);
        builder.setUv(10);
        Gson gson = new Gson();
        System.out.println(gson.toJson(builder));
    }

    private Msg count(int uv, int gap, Msg msg) {
        double preHc = msg.hc * Math.pow(0.999, gap);
        double hc = preHc * 0.999 + (1 - 0.999) * uv;
        double score = (uv + 10) / (preHc + 10);
        Msg newMsg = new Msg(preHc, hc, score);
        return newMsg;
    }


    class Msg {
        double hc;
        double preHc;
        double score;

        public Msg(double preHc, double hc, double score) {
            this.preHc = preHc;
            this.hc = hc;
            this.score = score;
        }

        @Override
        public String toString() {
            return "prehc:".concat(String.valueOf(preHc)).concat(", hc:").concat(String.valueOf(hc)).concat(", score:").concat
                    (String.valueOf(score));
        }
    }
}