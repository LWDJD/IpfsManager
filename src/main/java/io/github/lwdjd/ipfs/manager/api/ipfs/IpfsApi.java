package io.github.lwdjd.ipfs.manager.api.ipfs;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.github.lwdjd.ipfs.manager.config.ConfigManager;
import io.github.lwdjd.ipfs.manager.network.Network;

import java.util.Objects;

public class IpfsApi {
    public static final String ifpsDefaultApiUrl = "http://127.0.0.1:5001";
    public static final long getCidMaxSize = 1073741824L;
    public static final long packagingSize = 1073741824L;
    public static final int maxPreheatThreads = 64;
    public static final long maxPreheatBlockSize = 1048576L*2;
    public static final String apiV0FilesLs = "/api/v0/files/ls";
    public static final String apiV0Ls = "/api/v0/ls";
    public static final String apiV0DagGet = "/api/v0/dag/get";
    public static final String apiV0DagPut ="/api/v0/dag/put";
    public static final String dagPb = "dag-pb";
    public static final String dagJson = "dag-json";
    public static final String dagCbor = "dag-cbor";

    /**
     * 获取IPFS文件列表
     *
     * @param filePath 需要获取的文件路径，必须以/开头。
     * @param bLong 使用长列表格式。
     * @param bU 不排序;按目录顺序列出条目。
     * @return json数据
     */
    public static String getIpfsFileList(String filePath,boolean bLong,boolean bU){
        String result = "";
        String ipfsApiUrl = "";
        JSONObject config =ConfigManager.getConfig("config.json")==null?new JSONObject():ConfigManager.getConfig("config.json");
        if(config.get("ipfsApiUrl")==null||config.get("ipfsApiUrl")==""){
            ipfsApiUrl = ifpsDefaultApiUrl;
        }else {
            ipfsApiUrl = config.get("ipfsApiUrl").toString();
        }
        String sendData ="?";
        sendData = sendData+"arg="+filePath;
        sendData =sendData+"&long="+bLong;
        sendData =sendData+"&U="+bU;

        try {
            result = Network.Post(ipfsApiUrl+ apiV0FilesLs +sendData,"");
        } catch (Exception e) {
            result = "Error : "+e.getMessage();
        }
        return result;

    }

    /**
     * 获取CID的links信息
     *
     * @param cid 需要获取的CID。
     * @return json数据
     */
    public static String getIpfsFileLinks(String cid){
        String result = "";
        String ipfsApiUrl = "";
        JSONObject config =ConfigManager.getConfig("config.json")==null?new JSONObject():ConfigManager.getConfig("config.json");
        if(config.get("ipfsApiUrl")==null||config.get("ipfsApiUrl")==""){
            ipfsApiUrl = ifpsDefaultApiUrl;
        }else {
            ipfsApiUrl = config.get("ipfsApiUrl").toString();
        }


        String sendData ="?arg="+cid;
        try {
            result = Network.Post(ipfsApiUrl+ apiV0Ls +sendData,"");
        } catch (Exception e) {
            result = "Error : "+e.getMessage();
        }
        return result;

    }

    public static String getDAG(String cid){
        String result = "";
        String ipfsApiUrl = "";
        JSONObject config =ConfigManager.getConfig("config.json")==null?new JSONObject():ConfigManager.getConfig("config.json");
        if(config.get("ipfsApiUrl")==null||config.get("ipfsApiUrl")==""){
            ipfsApiUrl = ifpsDefaultApiUrl;
        }else {
            ipfsApiUrl = config.get("ipfsApiUrl").toString();
        }


        String sendData ="?arg="+cid+"&output-codec=dag-json";
//        System.out.println(ipfsApiUrl+ apiV0DagGet +sendData);
        try {
            result = Network.Post(ipfsApiUrl+ apiV0DagGet +sendData,"");
        } catch (Exception e) {
            result = "Error : "+e.getMessage();
        }
        return result;
    }

    public static String putDAG_json(String storeCodec,String inputCodec,String fromData) throws Exception {
        String ipfsApiUrl;
        JSONObject config =ConfigManager.getConfig("config.json")==null?new JSONObject():ConfigManager.getConfig("config.json");
        if(config.get("ipfsApiUrl")==null||config.get("ipfsApiUrl")==""){
            ipfsApiUrl = ifpsDefaultApiUrl;
        }else {
            ipfsApiUrl = config.get("ipfsApiUrl").toString();
        }
        String url = ipfsApiUrl+apiV0DagPut+"?store-codec="+storeCodec+"&input-codec="+inputCodec;
        JSONObject report = JSONObject.parseObject(Network.fromDataPost(url,fromData));
        JSONObject cid = report.getJSONObject("Cid");
        return cid.getString("/");
    }

    /**
     *
     * @param storeCodec 存储格式
     * @param inputCodec 输入格式
     * @param fromData 输入数据
     * @return 返回CID
     */
    public static String putDAG_json(String storeCodec,String inputCodec,JSONObject fromData) throws Exception {
        String ipfsApiUrl;
        JSONObject config =ConfigManager.getConfig("config.json")==null?new JSONObject():ConfigManager.getConfig("config.json");
        if(config.get("ipfsApiUrl")==null||config.get("ipfsApiUrl")==""){
            ipfsApiUrl = ifpsDefaultApiUrl;
        }else {
            ipfsApiUrl = config.get("ipfsApiUrl").toString();
        }
        String url = ipfsApiUrl+apiV0DagPut+"?store-codec="+storeCodec+"&input-codec="+inputCodec;
        JSONObject report = JSONObject.parseObject(Network.fromDataPost(url,fromData.toJSONString()));
        JSONObject cid;
        if(!Objects.equals(report.getJSONObject("Cid"),null)) {
            cid = report.getJSONObject("Cid");
        }else {
            throw new RuntimeException("Error: "+report);
        }
        return cid.getString("/");
    }
    public static void main(String[] args) throws Exception {
        ConfigManager.loadConfig("config.json");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("Data",new JSONObject());
        jsonObject.put("Links",new JSONArray());
        jsonObject.getJSONObject("Data").put("/",new JSONObject());
        jsonObject.getJSONObject("Data").getJSONObject("/").put("bytes","CAE");

        for(long i=0L;i<170L;i++) {
            JSONObject file = new JSONObject();
            JSONObject hash = new JSONObject();
            hash.put("/", "bafybeiaekwwgyyrvvnx5fusu53xd6eno5o5qwgq56lln73m2xiks6jgqmi");
            file.put("Hash", hash);
            file.put("Name", "bafybeiaekwwgyyrvvnx5fusu53xd6eno5o5qwgq56lln73m2xiks6jgqmi");
            file.put("Tsize", 4L);
            jsonObject.getJSONArray("Links").add(file);
        }
//        System.out.println(jsonObject.toJSONString());
        System.out.println(putDAG_json(dagPb,dagJson,jsonObject));

    }
}
