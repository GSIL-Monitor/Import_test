package com.jd.rec.nl.app.origin.modules.cid3relatedweight;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * 读取cid3_relevance_by_scan（打包到资源里）文档中的cid3的相关cid3的权重；
 * @author wl
 * @date 2018/8/25
 */
public class ReadCid3RelMapUtil {

    public Map<String, Map<String, Float>> readCid3RelMaps(){
        Map<String, Map<String, Float>> cid3RelMaps = new HashMap<>();
        InputStream inputStream = this.getClass().getResourceAsStream("/cid3_relevance_by_scan.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        try {
            while ((line = br.readLine()) != null){
                String[] strs = line.split("\t");
                String id3 = strs[0];
                String relatedId3 = strs[1];
                Float relWeight = Float.valueOf(strs[2]);
                if(!cid3RelMaps.containsKey(id3)){
                    Map<String, Float> cid3RelMap = new HashMap<>();
                    cid3RelMap.put(relatedId3, relWeight);
                    cid3RelMaps.put(id3, cid3RelMap);
                }
                else{
                    cid3RelMaps.get(id3).put(relatedId3, relWeight);
                }
            }
            inputStream.close();
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cid3RelMaps;
    }

    public static void main(String[] args) {
        Map<String, Map<String, Float>> result = new ReadCid3RelMapUtil().readCid3RelMaps();
        for(Map.Entry<String, Map<String, Float>> entry : result.entrySet()){
            String id3 = entry.getKey();
            Map<String, Float> valueMap = entry.getValue();
            for(Map.Entry<String, Float> entry1: valueMap.entrySet()){
                System.out.println(id3 + ":" + entry1.getKey() + ":" + entry1.getValue());
            }
        }
    }
}
