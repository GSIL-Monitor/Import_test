package com.jd.rec.nl.app.origin.modules.promotionalburst;

import com.jd.rec.nl.app.origin.modules.promotionalburst.domain.HistoryCUV;
import com.jd.rec.nl.core.utils.KryoUtils;
import com.jd.rec.nl.service.modules.user.service.UserService;
import org.junit.Test;
import p13.recsys.UnifiedUserProfile2Layers;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * @author linmx
 * @date 2018/7/26
 */
public class ComputerBurstInfoTest {

    @Test
    public void test() throws ExecutionException, InterruptedException {

        HistoryCUV historyCUV = new HistoryCUV();
        historyCUV.setTimestamp(System.currentTimeMillis());
        historyCUV.setCoefficient(123D);
        byte[] bytes = KryoUtils.serialize(historyCUV);
        HistoryCUV historyCUV1 = KryoUtils.deserialize(bytes, HistoryCUV.class);
        historyCUV1 = KryoUtils.deserialize(bytes);
    }

    @Test
    public void checkParallel() {
        String output =
                "WindowBolt-470=1536895276831, WindowBolt-471=1536895276831, WindowBolt-350=1536895276831, " +
                        "WindowBolt-351=1536895276831, WindowBolt-472=1536895276831, WindowBolt-356=1536895276831, " +
                        "WindowBolt-477=1536895276831, WindowBolt-357=1536895276831, WindowBolt-478=1536895276831, " +
                        "WindowBolt-358=1536895276831, WindowBolt-479=1536895276831, WindowBolt-359=1536895276831, " +
                        "WindowBolt-473=1536895276831, WindowBolt-352=1536895276831, WindowBolt-474=1536895276831, " +
                        "WindowBolt-353=1536895276831, WindowBolt-354=1536895276831, WindowBolt-475=1536895276831, " +
                        "WindowBolt-355=1536895276831, WindowBolt-476=1536895276831, WindowBolt-408=1536895276831, " +
                        "WindowBolt-409=1536895276831, WindowBolt-404=1536895276831, WindowBolt-405=1536895276831, " +
                        "WindowBolt-406=1536895276831, WindowBolt-407=1536895276831, WindowBolt-480=1536895276831, " +
                        "WindowBolt-360=1536895276831, WindowBolt-481=1536895276831, WindowBolt-482=1536895276831, " +
                        "WindowBolt-361=1536895276831, WindowBolt-362=1536895276831, WindowBolt-483=1536895276831, " +
                        "WindowBolt-488=1536895276831, WindowBolt-367=1536895276831, WindowBolt-400=1536895276831, " +
                        "WindowBolt-489=1536895276831, WindowBolt-401=1536895276831, WindowBolt-368=1536895276831, " +
                        "WindowBolt-402=1536895276831, WindowBolt-369=1536895276831, WindowBolt-403=1536895276831, " +
                        "WindowBolt-363=1536895276831, WindowBolt-484=1536895276831, WindowBolt-485=1536895276831, " +
                        "WindowBolt-364=1536895276831, WindowBolt-486=1536895276831, WindowBolt-365=1536895276831, " +
                        "WindowBolt-366=1536895276831, WindowBolt-487=1536895276831, WindowBolt-419=1536895276831, " +
                        "WindowBolt-415=1536895276831, WindowBolt-416=1536895276831, WindowBolt-417=1536895276831, " +
                        "WindowBolt-418=1536895276831, WindowBolt-491=1536895276831, WindowBolt-370=1536895276831, " +
                        "WindowBolt-371=1536895276831, WindowBolt-492=1536895276831, WindowBolt-372=1536895276831, " +
                        "WindowBolt-493=1536895276831, WindowBolt-494=1536895276831, WindowBolt-373=1536895276831, " +
                        "WindowBolt-490=1536895276831, WindowBolt-499=1536895276831, WindowBolt-378=1536895276831, " +
                        "WindowBolt-411=1536895276831, WindowBolt-412=1536895276831, WindowBolt-379=1536895276831, " +
                        "WindowBolt-413=1536895276831, WindowBolt-414=1536895276831, WindowBolt-495=1536895276831, " +
                        "WindowBolt-375=1536895276831, WindowBolt-496=1536895276831, WindowBolt-497=1536895276831, " +
                        "WindowBolt-376=1536895276831, WindowBolt-410=1536895276831, WindowBolt-377=1536895276831, " +
                        "WindowBolt-498=1536895276831, WindowBolt-426=1536895276831, WindowBolt-427=1536895276831, " +
                        "WindowBolt-428=1536895276831, WindowBolt-429=1536895276831, WindowBolt-381=1536895276831, " +
                        "WindowBolt-382=1536895276831, WindowBolt-383=1536895276831, WindowBolt-384=1536895276831, " +
                        "WindowBolt-380=1536895276831, WindowBolt-422=1536895276831, WindowBolt-389=1536895276831, " +
                        "WindowBolt-423=1536895276831, WindowBolt-424=1536895276831, WindowBolt-425=1536895276831, " +
                        "WindowBolt-385=1536895276831, WindowBolt-386=1536895276831, WindowBolt-420=1536895276831, " +
                        "WindowBolt-387=1536895276831, WindowBolt-421=1536895276831, WindowBolt-388=1536895276831, " +
                        "WindowBolt-437=1536895276831, WindowBolt-438=1536895276831, WindowBolt-439=1536895276831, " +
                        "WindowBolt-392=1536895276831, WindowBolt-393=1536895276831, WindowBolt-394=1536895276831, " +
                        "WindowBolt-395=1536895276831, WindowBolt-390=1536895276831, WindowBolt-391=1536895276831, " +
                        "WindowBolt-433=1536895276831, WindowBolt-434=1536895276831, WindowBolt-435=1536895276831, " +
                        "WindowBolt-436=1536895276831, WindowBolt-396=1536895276831, WindowBolt-397=1536895276831, " +
                        "WindowBolt-430=1536895276831, WindowBolt-431=1536895276831, WindowBolt-398=1536895276831, " +
                        "WindowBolt-432=1536895276831, WindowBolt-399=1536895276831, WindowBolt-448=1536895276831, " +
                        "WindowBolt-449=1536895276831, WindowBolt-444=1536895276831, WindowBolt-445=1536895276831, " +
                        "WindowBolt-446=1536895276831, WindowBolt-447=1536895276831, WindowBolt-440=1536895276831, " +
                        "WindowBolt-441=1536895276831, WindowBolt-442=1536895276831, WindowBolt-443=1536895276831, " +
                        "WindowBolt-459=1536895276831, WindowBolt-450=1536895276831, WindowBolt-455=1536895276831, " +
                        "WindowBolt-456=1536895276831, WindowBolt-457=1536895276831, WindowBolt-458=1536895276831, " +
                        "WindowBolt-451=1536895276831, WindowBolt-452=1536895276831, WindowBolt-453=1536895276831, " +
                        "WindowBolt-454=1536895276831, WindowBolt-349=1536895276831, WindowBolt-460=1536895276831, " +
                        "WindowBolt-461=1536895276831, WindowBolt-466=1536895276831, WindowBolt-345=1536895276831, " +
                        "WindowBolt-500=1536895276831, WindowBolt-467=1536895276831, WindowBolt-346=1536895276831, " +
                        "WindowBolt-347=1536895276831, WindowBolt-501=1536895276831, WindowBolt-468=1536895276831, " +
                        "WindowBolt-469=1536895276831, WindowBolt-348=1536895276831, WindowBolt-462=1536895276831, " +
                        "WindowBolt-342=1536895276831, WindowBolt-463=1536895276831, WindowBolt-464=1536895276831, " +
                        "WindowBolt-343=1536895276831, WindowBolt-465=1536895276831, WindowBolt-344=1536895276831";

        String workers = "342-342\n" +
                "343-343\n" +
                "344-344\n" +
                "345-345\n" +
                "346-346\n" +
                "347-347\n" +
                "348-348\n" +
                "349-349\n" +
                "350-350\n" +
                "351-351\n" +
                "352-352\n" +
                "353-353\n" +
                "354-354\n" +
                "355-355\n" +
                "356-356\n" +
                "357-357\n" +
                "358-358\n" +
                "359-359\n" +
                "360-360\n" +
                "361-361\n" +
                "362-362\n" +
                "363-363\n" +
                "364-364\n" +
                "365-365\n" +
                "366-366\n" +
                "367-367\n" +
                "368-368\n" +
                "369-369\n" +
                "370-370\n" +
                "371-371\n" +
                "372-372\n" +
                "373-373\n" +
                "374-374\n" +
                "375-375\n" +
                "376-376\n" +
                "377-377\n" +
                "378-378\n" +
                "379-379\n" +
                "380-380\n" +
                "381-381\n" +
                "382-382\n" +
                "383-383\n" +
                "384-384\n" +
                "385-385\n" +
                "386-386\n" +
                "387-387\n" +
                "388-388\n" +
                "389-389\n" +
                "390-390\n" +
                "391-391\n" +
                "392-392\n" +
                "393-393\n" +
                "394-394\n" +
                "395-395\n" +
                "396-396\n" +
                "397-397\n" +
                "398-398\n" +
                "399-399\n" +
                "400-400\n" +
                "401-401\n" +
                "402-402\n" +
                "403-403\n" +
                "404-404\n" +
                "405-405\n" +
                "406-406\n" +
                "407-407\n" +
                "408-408\n" +
                "409-409\n" +
                "410-410\n" +
                "411-411\n" +
                "412-412\n" +
                "413-413\n" +
                "414-414\n" +
                "415-415\n" +
                "416-416\n" +
                "417-417\n" +
                "418-418\n" +
                "419-419\n" +
                "420-420\n" +
                "421-421\n" +
                "422-422\n" +
                "423-423\n" +
                "424-424\n" +
                "425-425\n" +
                "426-426\n" +
                "427-427\n" +
                "428-428\n" +
                "429-429\n" +
                "430-430\n" +
                "431-431\n" +
                "432-432\n" +
                "433-433\n" +
                "434-434\n" +
                "435-435\n" +
                "436-436\n" +
                "437-437\n" +
                "438-438\n" +
                "439-439\n" +
                "440-440\n" +
                "441-441\n" +
                "442-442\n" +
                "443-443\n" +
                "444-444\n" +
                "445-445\n" +
                "446-446\n" +
                "447-447\n" +
                "448-448\n" +
                "449-449\n" +
                "450-450\n" +
                "451-451\n" +
                "452-452\n" +
                "453-453\n" +
                "454-454\n" +
                "455-455\n" +
                "456-456\n" +
                "457-457\n" +
                "458-458\n" +
                "459-459\n" +
                "460-460\n" +
                "461-461\n" +
                "462-462\n" +
                "463-463\n" +
                "464-464\n" +
                "465-465\n" +
                "466-466\n" +
                "467-467\n" +
                "468-468\n" +
                "469-469\n" +
                "470-470\n" +
                "471-471\n" +
                "472-472\n" +
                "473-473\n" +
                "474-474\n" +
                "475-475\n" +
                "476-476\n" +
                "477-477\n" +
                "478-478\n" +
                "479-479\n" +
                "480-480\n" +
                "481-481\n" +
                "482-482\n" +
                "483-483\n" +
                "484-484\n" +
                "485-485\n" +
                "486-486\n" +
                "487-487\n" +
                "488-488\n" +
                "489-489\n" +
                "490-490\n" +
                "491-491\n" +
                "492-492\n" +
                "493-493\n" +
                "494-494\n" +
                "495-495\n" +
                "496-496\n" +
                "497-497\n" +
                "498-498\n" +
                "499-499\n" +
                "500-500\n" +
                "501-501";
        String[] out = output.split(",");
        Set<String> outSet = Arrays.stream(out).map(str ->
                str.replaceAll("\\n", "").substring(str.indexOf("-") + 1, str.indexOf("=")))
                .collect(Collectors.toSet());

        Set<String> workerSet = Arrays.stream(workers.split("\\n")).map(str -> str.substring(0, str.indexOf("-")))
                .collect(Collectors.toSet());

        workerSet.forEach(worker -> {
            if (!outSet.contains(worker)) {
                System.out.println(worker);
            }
        });
    }

    @Test
    public void testLayII() {
        Map<Integer, Double> cid3Score = new HashMap<>();
        cid3Score.put(1, 0.324875D);
        cid3Score.put(2, 0.2D);
        cid3Score.put(3, 0.3D);
        cid3Score.put(4, 0.4D);
        String uid = "test";
        UnifiedUserProfile2Layers.UnifiedUserProfile2layersProto.Builder builder = UnifiedUserProfile2Layers
                .UnifiedUserProfile2layersProto.newBuilder();
        builder.setUid(uid);
        cid3Score.entrySet().stream().map(entry -> {
            UnifiedUserProfile2Layers.LevelTwo.Builder levelIIBuilder = UnifiedUserProfile2Layers.LevelTwo.newBuilder();
            levelIIBuilder.setProper(entry.getKey().toString());
            levelIIBuilder.setValue(entry.getValue());
            return levelIIBuilder.build();
        }).forEach(levelTwo -> builder.addLevelTwo(levelTwo));
        byte[] value = builder.build().toByteArray();

        UnifiedUserProfile2Layers.UnifiedUserProfile2layersProto ret = (UnifiedUserProfile2Layers
                .UnifiedUserProfile2layersProto) UserService.deserialize("recsys_p_pin_to_rbcid", ByteBuffer.wrap(value));
        System.out.println(String.format("uid:%s,%s", ret.getUid(), ret.getLevelTwoList()));
    }

    @Test
    public void testSout() {
        PrintStream err = System.err;
        System.setErr(new PrintStream(new ByteArrayOutputStream()));
        Exception e = new NullPointerException();
        e.printStackTrace();
        System.setErr(err);
        e.printStackTrace();
    }


    @Test
    public void diffRecall() throws IOException {
        File list = new File("/Users/linmx/Documents/jd/list.log");
        BufferedReader reader = new BufferedReader(new FileReader(list));
        String line;
        Set<String> ips1 = new HashSet<>();
        Set<String> ips2 = new HashSet<>();

        while ((line = reader.readLine()) != null) {
            //            line = line.trim().substring(1, line.trim().length() - 1);
            String ip = line.split("\t")[1].split("\\.")[0];
            String[] split = ip.split("-");
            ip = split[3].concat(".").concat(split[4]).concat(".").concat(split[5]).concat(".").concat(split[6]);
            ips1.add(ip);
        }
        System.out.println(ips1.size());
        list = new File("/Users/linmx/Documents/jd/ip.log");
        reader = new BufferedReader(new FileReader(list));
        while ((line = reader.readLine()) != null) {
            ips2.add(line.trim());
        }
        System.out.println(ips2.size());
        ips2.stream().forEach(ip -> {
            if (!ips1.contains(ip)) {
                System.out.println(ip);
            }
        });
    }
}