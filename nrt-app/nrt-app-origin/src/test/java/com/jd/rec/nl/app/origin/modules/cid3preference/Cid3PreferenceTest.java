package com.jd.rec.nl.app.origin.modules.cid3preference;

import org.junit.Test;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author linmx
 * @date 2018/9/4
 */
public class Cid3PreferenceTest {
    private static long untilNow(Integer time) throws ParseException {
        LocalDate localDate = LocalDate.parse(time.toString(), DateTimeFormatter.ofPattern("yyyyMMdd"));
        return localDate.until(LocalDate.now(), ChronoUnit.DAYS);
    }

    @Test
    public void addOrRemoveUserClickInfo() throws Exception {
        double alpha = -0.1;
        Cid3Preference cid3Preference = new Cid3Preference(alpha);

    }

    @Test
    public void limitMaxSize() throws Exception {
        Cid3Preference cid3Preference = new Cid3Preference();
        Map<Integer, Double> in = new HashMap<>();
        in.put(1, 10D);
        in.put(3, 30D);
        in.put(2, 20D);
        in.put(5, 50D);
        in.put(8, 80D);
        in.put(4, 40D);

        cid3Preference.limitMaxSize(in, 5);
        System.out.println(in);
    }

    @Test
    public void test() throws ParseException {
        int survivalTime = 7;
        System.out.println(untilNow(20180829));
        System.out.println(untilNow(20180828));
        System.out.println(untilNow(20180904));
        System.out.println(untilNow(20180905));
    }

    @Test
    public void testDoubt() {
        double alpha = -0.1;
        double[] decayCoefficients = {1D, Math.exp(alpha * 1), Math.exp(alpha * 2), Math.exp(alpha * 3), Math.exp(alpha * 4), Math
                .exp(alpha * 5), Math.exp(alpha * 6)};
        Arrays.stream(decayCoefficients).forEach(decayCoefficient -> System.out.println(decayCoefficient));
        //        double preScore = 1 * decayCoefficients[(int) Cid3Preference.untilNow(20180910)];
        //        System.out.println(preScore);
    }


}