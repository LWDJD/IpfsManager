package io.github.lwdjd.ipfs.manager.api.ipfs;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.github.lwdjd.ipfs.manager.JsonProcess;
import io.github.lwdjd.ipfs.manager.config.ConfigManager;
import io.github.lwdjd.ipfs.manager.network.Network;

import java.util.HashMap;

public class IpfsApi {
    public static final String ifpsDefaultApiUrl = "http://127.0.0.1:5001/";
    public static final String apiV0FilesLs = "api/v0/files/ls";
    public static final String apiV0Ls = "api/v0/ls";

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

    public static void main(String[] args){
        ConfigManager.loadConfig("config.json");
        HashMap<String,Object> resultMap = JsonProcess.ipfsApiV0LsJsonProcess(getIpfsFileLinks("bafybeih4f5mo4so2kvbl5bxnrdgzcoahzjfbfk2fku72galglg2czbj45e"));
        System.out.println(
                JSONObject.parseObject(
                        JSONArray.parseArray(
                                resultMap.get("Links").toString()
                        ).get(0).toString()
                ).get("Type")
        );

        System.out.println(resultMap.get("Hash"));
    }
}
