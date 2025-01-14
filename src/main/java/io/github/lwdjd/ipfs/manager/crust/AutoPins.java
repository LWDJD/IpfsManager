package io.github.lwdjd.ipfs.manager.crust;

import io.github.lwdjd.ipfs.manager.listener.PinsListener;
import io.github.lwdjd.ipfs.manager.network.Network;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AutoPins {
//    public static Boolean isAutoRetryPins = false;
    public static final String publicSign = "Bearer c3Vic3RyYXRlLWNUTTQ5elppcEE1cFRnSGVqWVFSU1dmQUdSYTFXQjlNNmZtODVBOTEzZVJwZkY1cEQ6MHg4OGMyOTEzMzdkOTY3N2Y1NGJiMDE4MTQ3OWI0NTc0NTJlNDk3OWVkMGQyMDgwZjFlYTMxYjMxZjJmYzBiMjBmYTY5OTcyNjlhODY0MjJjODlkODRkOGNjMDI2ODJlZjdlMTc5ODYyYzljZTI5NTkyNTk4MWM5MWQ1YzRlZjM4Zg==";
    public static final String crustPinUrl = "https://pin.crustcode.com/psa/pins";
    public static final String pinsFile = "pinsList/";
    //监听器列表
    private static final List<PinsListener> pinsListenerList = new ArrayList<>();
    //重试列表的公平锁
    private static final Lock retryListLock = new ReentrantLock(true);
    //重试列表,哈希表：1.cid 2.fileName
    private static final List<HashMap<String,String>> retryList = new ArrayList<>();




    public static void retryListAdd(HashMap<String,String> item){
        try {
            retryListLock.lock();
            retryList.add(item);
        } finally {
            retryListLock.unlock();
        }
    }

    public static void retryListClear(String[] item){
        try {
            retryListLock.lock();
            retryList.clear();
        } finally {
            retryListLock.unlock();
        }
    }

    public static List<HashMap<String, String>> getRetryList() {
        try {
            retryListLock.lock();
            return new ArrayList<>(retryList);
        } finally {
            retryListLock.unlock();
        }
    }

    public static List<HashMap<String, String>> getAndClearRetryList() {
        try {
            retryListLock.lock();
            return new ArrayList<>(retryList);
        } finally {
            try {
                retryList.clear();
            }finally {
                retryListLock.unlock();
            }
        }
    }

    public static void addPinsListener(PinsListener pinsListener){
        pinsListenerList.add(pinsListener);
    }





    /**
     *
     * @param cid 文件的CID
     * @param fileName 文件名
     * @param sign 签名数据，可以在https://apps.crust.network/?rpc=wss%3A%2F%2Frpc.crust.network#/pins使用F12捕获https://pin.crustcode.com/psa/pins的请求头中的authorization字段
     * @return 返回的数据
     */
    public static String sendPin(String cid,String fileName,String sign) throws Exception {

        String json = "{\"cid\":\""+cid+"\"}";
        return Network.pinPost(crustPinUrl, json,sign);
    }

    /**
     * 单线程一键发送多个pin请求,fileName不存在时会使用CID作为文件名。
     *
     * @param cids CID列表
     * @param sign 签名数据，可以在https://apps.crust.network/?rpc=wss%3A%2F%2Frpc.crust.network#/pins使用F12捕获https://pin.crustcode.com/psa/pins的请求头中的authorization字段
     * @return 返回 请求返回列表（Error : 为请求报错信息）
     */
    public static List<String> sendPins(List<HashMap<String,String>> cids,String sign){
        List<String> result = new ArrayList<>();
        for(int i=0;i<cids.size();i++){

            //如果此项没有cid则跳过
            if(cids.get(i).get("fileCid")==null){
                continue;
            }

            String fileName = "";
            //没有fileName时使用CID作为文件名
            if(cids.get(i).get("fileName")!=null){
                fileName = cids.get(i).get("fileName");
            }else{
                fileName = cids.get(i).get("fileCid");
            }


            String repost = "";
            try {
                repost = sendPin(cids.get(i).get("fileCid"),fileName,sign);
            } catch (Exception e) {
                repost = "Error : "+e.getMessage();
            }
            //添加响应信息
            result.add(repost);
            //返回监听数据
            for(PinsListener pinsListener:pinsListenerList){
                try {
                    pinsListener.onPins(cids.size(),i+1,cids.get(i).get("fileCid"),fileName,repost);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

//    /**
//     * 单线程一键发送多个pin请求（文件名为CID）
//     *
//     * @param cids CID列表
//     * @param sign 签名数据，可以在https://apps.crust.network/?rpc=wss%3A%2F%2Frpc.crust.network#/pins使用F12捕获https://pin.crustcode.com/psa/pins的请求头中的authorization字段
//     * @return 返回 请求返回列表（Error : 为请求报错信息）
//     */
//    public static List<String> sendPins(List<String> cids,String sign){
//        List<String> result = new ArrayList<>();
//        //循环发送固定请求，并设置文件名为CID。
//        for (int i = 0; i < cids.size(); i++) {
//            String repost = "";
//            try {
//                repost = sendPin(cids.get(i),cids.get(i), sign);
//            } catch (Exception e) {
//                repost = "Error : " + e.getMessage();
//            }
//            //添加响应信息
//            result.add(repost);
//            //返回监听数据
//            for(PinsListener pinsListener:pinsListenerList){
//                try {
//                    pinsListener.onPins(cids.size(), i + 1, cids.get(i),cids.get(i), repost);
//                }catch (Exception e){
//                    e.printStackTrace();
//                }
//            }
//        }
//        return result;
//    }

}
