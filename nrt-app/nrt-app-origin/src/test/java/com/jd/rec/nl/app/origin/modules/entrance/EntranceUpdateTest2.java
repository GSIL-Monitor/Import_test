package com.jd.rec.nl.app.origin.modules.entrance;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.junit.Test;
import p13.nearline.NrtQrqmEntrance;

import javax.annotation.Nonnull;
import java.util.*;

public class EntranceUpdateTest2 {
    @Test
    public void test2() {
        Map<String, String> attrs = new HashMap<>();
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            String contentStr = attrs.get("111");
            if (contentStr == null || contentStr.isEmpty()) {
                continue;
            }
            System.out.println(contentStr);
            String[] contents = contentStr.split(",");
            System.out.println(contents);
            keys.add(contentStr);
        }
        System.out.println(keys);

    }

    @Test
    public void test3() {
        Map<String, Integer> map = new HashMap();
        boolean change = (map == null || map.isEmpty());
        System.out.println(change);
    }

    @Test
    public void test4() {
        String actKey = generateKey("nl_entrance_recalled_contents", "862187030212718-021a11fdd0cc");
        System.out.println(actKey);
    }

    protected static String generateKey(String... keyFields) {
        String dbKey = "nrt_db_";
        String key = StringUtils.join(keyFields, "-");
        return dbKey.concat(key);
    }

    @Test
    public void test5() {
        Map<Long, Set<Long>> floorToContents = new HashMap<>();
        Set<Long> set = new HashSet<>();
        set.add(1l);
        set.add(2l);
        floorToContents.put(110l, set);
        String uid = "862187030212718-021a11fdd0cc";
        String message = floorToContents + "," + uid + ",fllor:" + floorToContents;
        System.out.println(message);
    }

    @Test
    public void test6() {
        Map<Long, Set<Long>> floorToContents = new HashMap<>();
        Set<Long> set = new HashSet<>();
        set.add(1l);
        set.add(2l);
        floorToContents.put(110l, set);
        Map<Long, Set<Long>> channelToContents = null;
        if (floorToContents == null && channelToContents == null) {
            System.out.println("111");
        }
    }


    @Test
    public void test11() {
        String s1 = "1";
        String s2 = "1";
        System.out.println(s1.equals(s2));
    }

    @Test
    public void test7() {
        Map<Long, Set<Long>> floorToContents = new HashMap<>();
        Set<Long> set = new HashSet<>();
        set.add(1l);
        set.add(2l);
        floorToContents.put(110l, set);
        Map<Long, Set<Long>> floorToContents2 = new HashMap<>();
        Set<Long> set2 = new HashSet<>();
        set.add(1l);
        set.add(2l);
        floorToContents2.put(110l, set2);
        //不能equals比较2个map
        System.out.println(floorToContents.equals(floorToContents2));
    }

    @Test
    public void test8() throws InvalidProtocolBufferException {
        Map<Long, Set<Long>> floorToContents = new HashMap<>();
        Set<Long> set = new HashSet<>();
        set.add(1l);
        set.add(2l);
        floorToContents.put(110l, set);
        byte[] byte1 = serializeRecalledContent("uid1", floorToContents);
        Map<Long, Set<Long>> floorToContents2 = new HashMap<>();
        Set<Long> set2 = new HashSet<>();
        set2.add(1l);
        set2.add(2l);
        floorToContents2.put(110l, set2);
        byte[] byte2 = serializeRecalledContent("uid1", floorToContents2);
        //byte[]数组的比较出错了,不能equals比较
        //这个是比较2个数组是否是同一个数组
        System.out.println(byte1.equals(byte2));
        /**
         * 下面的这2种方法可以比较
         */
        //使用arrays比较2个字节数组的情况
        System.out.println(Arrays.equals(byte1, byte2));
        System.out.println("length:" + byte1.length + "," + byte2.length);
        System.out.println("输出：" + byte1 + "," + byte2);
        //String方式
        String s1 = Arrays.toString(byte1);
        String s2 = Arrays.toString(byte2);
        System.out.println("String方式：" + s1.equals(s2));

    }


    @Test
    public void test16() {
        Map<Long, Set<Long>> floorToContents = new HashMap<>();
        Set<Long> set = new HashSet<>();
        set.add(1l);
        set.add(2l);
        floorToContents.put(110l, set);
        add(1, 2);
        System.out.println(floorToContents);
    }


    public void add(int a, int b) {
        return;
    }

    /**
     * 序列化
     *
     * @param uid
     * @param recalledItemResult
     * @return
     */
    private byte[] serializeRecalledContent(String uid, Map<Long, Set<Long>> recalledItemResult) {
        NrtQrqmEntrance.NrtEntranceProto.Builder nep = NrtQrqmEntrance.NrtEntranceProto.newBuilder();
        nep.setUid(uid);
        for (Map.Entry<Long, Set<Long>> entry : recalledItemResult.entrySet()) {
            NrtQrqmEntrance.RecalledItems.Builder ri = NrtQrqmEntrance.RecalledItems.newBuilder();
            Long channelID = entry.getKey();
            ri.setChannelId(channelID);
            Set<Long> itemMap = entry.getValue();
            for (long sku : itemMap) {
                NrtQrqmEntrance.ItemsWithRule.Builder ir = NrtQrqmEntrance.ItemsWithRule.newBuilder();
                ir.setSkuId(sku);
                ri.addIRule(ir);
            }
            nep.addRItems(ri);
        }
        NrtQrqmEntrance.NrtEntranceProto result = nep.build();
        return result.toByteArray();
    }

    /**
     * 反序列化
     *
     * @param values
     * @return
     * @throws InvalidProtocolBufferException
     */
    public Map<Long, Set<Long>> deserializationPoolToContentIds(byte[] values) throws InvalidProtocolBufferException {
        Map<Long, Set<Long>> poolToContents = new HashMap<>();
        if (values == null) {
            return null;
        }
        NrtQrqmEntrance.NrtEntranceProto qrqmEntrance = NrtQrqmEntrance.NrtEntranceProto.parseFrom(values);
        NrtQrqmEntrance.RecalledItems recalledItems = qrqmEntrance.getRItems(0);
        long channelId = recalledItems.getChannelId();
        Set<Long> contentIds = new HashSet<>();
        for (NrtQrqmEntrance.ItemsWithRule itemsWithRule : recalledItems.getIRuleList()) {
            contentIds.add(itemsWithRule.getSkuId());
        }
        poolToContents.put(channelId, contentIds);
        return poolToContents;
    }

    @Test
    public void test9() {
        String a[] = {"a", "b", "c"};
        String[] b = {"a", "b", "c"};
        Arrays.equals(a, b);
    }

    @Test
    public void test10() {
        Set<Long> set = new HashSet<>();
        set.add(1l);
        set.add(2l);

        Set<Long> set2 = new HashSet<>();
        set.add(1l);
        set.add(2l);

    }

    @Test
    public void test12() {
        Map<Long, Map<Integer, Set<Long>>> channelToCid3ToSkusOld = new HashMap<>();
        Map<Integer, Set<Long>> map = new HashMap<>();
        Set<Long> set = new HashSet<>();
        set.add(1L);
        map.put(1, set);
        channelToCid3ToSkusOld.put(1L, map);
        System.out.println(channelToCid3ToSkusOld.size());
    }

    @Test
    public void test13() {
        Map<Long, Map<Integer, Set<Long>>> channelToCid3ToSkusOld = new HashMap<>();
        if (channelToCid3ToSkusOld.isEmpty()) {

        }
        add(1, 2);
        System.out.println("111");

    }

    @Test
    public void test14() {
        Map<String, Map<Long, Float>> relatedSkus = new HashMap<>();
       for(Map<Long,Float> map:relatedSkus.values()){
           for(Long sku:map.keySet()){
               System.out.println(String.valueOf(sku));
           }
       }
    }

    @Test
    public void time() {
        System.out.println(System.currentTimeMillis());
    }

    public Set<String> mergeRelatedAndSimilarSkus(Set<String> clickedSkus, Map<String, Map<Long, Float>> relatedSkus, Map<String, Map<Long, Float>> simSkus) {
        Set<String> skus = new HashSet<>();
        skus.addAll(clickedSkus);
        if(relatedSkus!=null){
            for(Map<Long,Float> map:relatedSkus.values()){
                for(Long sku:map.keySet()){
                    skus.add(String.valueOf(sku));
                }
            }
        }
        if(simSkus!=null){
            for(Map<Long,Float> map:simSkus.values()){
                for(Long sku:map.keySet()){
                    skus.add(String.valueOf(sku));
                }
            }
        }
        return skus;
    }

    @Nonnull
    @SafeVarargs
    private final Set<Long> mergeRelatedAndSimilarSkus(final Set<Long> clickedSkus, final Collection<Map<Long, Float>>... args) {
        final Set<Long> skus = new HashSet<>();
        skus.addAll(clickedSkus);
        for (final Collection<Map<Long, Float>> maps : args)
            for (final Map<Long, Float> map : maps)
                skus.addAll(map.keySet());
        return skus;
    }

    @Test
    public void test45(){
        Set<Long> skus = new HashSet<>();
        skus.add(1l);
        skus.add(2l);
        skus.add(3l);
        skus.add(4l);
        Map<String, Map<Long, Float>> relatedSkus = new HashMap<>();
        Map<String, Map<Long, Float>> simSkus = new HashMap<>();

        Set<String> skus2 = new HashSet<>();
        skus2.add("1");
        skus2.add("2");
        skus2.add("3");
        skus2.add("4");
       Set<Long> old= mergeRelatedAndSimilarSkus(skus,relatedSkus.values(),simSkus.values());
        System.out.println("old:"+old);
        Set<String> nw = mergeRelatedAndSimilarSkus(skus2,relatedSkus,simSkus);
        System.out.println("nw:"+nw);
    }

    @Test
    public void test44(){
      Map<Integer,String> map = new HashMap();
      map.put(1,"hello");
      for(Map.Entry<Integer,String> m : map.entrySet()){
          if(m.getKey()==1){
              break;
          }
          System.out.println("fadsaf");
      }
    }
}
