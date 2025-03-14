package io.github.lwdjd.ipfs.manager.backup;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.github.lwdjd.ipfs.manager.api.ipfs.IpfsApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Dag {

    public static final String backupPath = "DagBackup/";


    /**
     * 备份DAG结构数据
     *
     * @param cid 需要备份的根CID
     * @return 已备份数据
     */
    public static JSONObject backup(String cid){
        badCid.clear();
        DAGresult.clear();
        JSONObject backupData =new JSONObject();
        backupData.put("rootCid",cid);
        try {
            getDAGLinks(cid);
        }catch (Exception e){
            System.out.println("获取DAG失败，可以提交报错信息给作者。");
        }
        while (true){
            if(count.get() == 0){
                if(badCid.size()!=0) {
                    for (HashMap<String, String> badc : badCid) {
                        Thread thread = new Thread(() -> {
                            try {
                                if (Long.parseLong(badc.get("Size")) > 1048576L) {
                                    try {
                                        getDAGLinks(badc.get("Hash"));
                                    } catch (Exception e) {
                                        System.out.println("此文件子块获取失败" + " cid:" + badc.get("Hash") + " 大小：" + badc.get("Size") + " 名称：" + badc.get("Name"));
                                        badCidLock.lock();
                                        try {
                                            badCid.add(badc);
                                        } finally {
                                            badCidLock.unlock();
                                        }
                                    }
//                            System.out.println(jsonObject.getJSONObject("Hash").getString("/") + "结束");
                                }
                            } finally {
                                count.getAndDecrement();
                            }
                        });
                        count.getAndIncrement();
                        executorGetDAGLinks.execute(thread);
                    }
                    continue;
                }
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("共有 "+DAGresult.size()+" 个块\n正在写入...");
        backupData.put("data",DAGresult);
        executorGetDAGLinks.shutdownNow();
        System.gc();
        return backupData;
    }

    public static List<HashMap<String,String>> badCid= new ArrayList<>();
    public static final Lock badCidLock = new ReentrantLock(true);
    public static JSONArray DAGresult = new JSONArray();
    public static Lock resultLock = new ReentrantLock(true);
    public static ExecutorService executorGetDAGLinks = new ThreadPoolExecutor(
            8, 8, 20, TimeUnit.SECONDS, // 核心线程数 = 最大线程数，空闲60秒后回收
            new LinkedBlockingQueue<>()
    );
    public static AtomicLong count = new AtomicLong();
    private static void getDAGLinks(String cid){
        try {
            JSONObject ipfsDAGLinksJsonObj = JSONObject.parseObject(IpfsApi.getDAG(cid));
//            System.out.println("获取到一个");
            resultLock.lock();
            try{
                DAGresult.add(ipfsDAGLinksJsonObj);
            }finally {
                resultLock.unlock();
            }
            JSONArray ipfsDAGLinksJsonObjObjArray = ipfsDAGLinksJsonObj.getJSONArray("Links");
            for (int i = 0 ;i<ipfsDAGLinksJsonObjObjArray.size();i++){
                int finalI = i;
                Thread thread = new Thread(()->{
                    try {
                        JSONObject jsonObject = ipfsDAGLinksJsonObjObjArray.getJSONObject(finalI);
                        HashMap<String, String> linkMap = new HashMap<>();
                        linkMap.put("Name", jsonObject.getString("Name"));
                        linkMap.put("Hash", jsonObject.getJSONObject("Hash").getString("/"));
                        linkMap.put("Size", jsonObject.getString("Tsize"));
                        if (Long.parseLong(jsonObject.getString("Tsize")) > 1048576L) {
                            try {
                                getDAGLinks(jsonObject.getJSONObject("Hash").getString("/"));
                            } catch (Exception e) {
                                System.out.println("此文件子块获取失败" + " cid:" + jsonObject.getJSONObject("Hash").getString("/") + " 大小：" + jsonObject.getString("Tsize") + " 名称：" + jsonObject.getString("Name"));
                                badCidLock.lock();
                                try {
                                    badCid.add(linkMap);
                                } finally {
                                    badCidLock.unlock();
                                }
                            }
//                            System.out.println(jsonObject.getJSONObject("Hash").getString("/") + "结束");
                        }
                    }finally {
                        count.getAndDecrement();
                    }
                });
                count.getAndIncrement();
                executorGetDAGLinks.execute(thread);
            }
        }catch (Exception e){
            throw new RuntimeException("获取DAG失败");
        }

    }


    private static JSONArray dataArray = new JSONArray();
    private static final Lock dataLock = new ReentrantLock(true);
    public static ArrayList<Thread> ListPutDAGLinks = new ArrayList<>();
    public static HashMap<String,Boolean> ListPutDAGLinksStatus = new HashMap<>();
    public static Lock ListPutDAGLinksStatusLock = new ReentrantLock(true);
    /**
     * 恢复数据
     *
     * @param backupData 已备份数据
     * @return 根CID
     */
    public static String restore(JSONObject backupData) throws InterruptedException {
        dataLock.lock();
        try {
            dataArray = backupData.getJSONArray("data");
        }finally {
            dataLock.unlock();
        }
        for (int i=0;i<8;i++){
            newPutDAGThread();
        }
        Thread stat = new Thread(()->{
            while (true) {
                dataLock.lock();
                try {
                    System.out.println("剩余块数：" + dataArray.size());
                }finally {
                    dataLock.unlock();
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                    return;
                }
            }
        });
        stat.start();
        for(Thread thread:ListPutDAGLinks){
            thread.join();
        }
        stat.interrupt();
        return backupData.getString("rootCid");
    }

    private static void newPutDAGThread(){
        Thread thread = new Thread(()->{
            boolean stat = true;
            while (true){
                if(dataArray.size()>0){
                    if(!stat) {
                        stat = true;
                        ListPutDAGLinksStatusLock.lock();
                        try {
                            ListPutDAGLinksStatus.put(Thread.currentThread().getName(), true);
                        } finally {
                            ListPutDAGLinksStatusLock.unlock();
                        }

                    }

                    JSONObject jsonObject ;
                    dataLock.lock();
                    try {
                        jsonObject = dataArray.getJSONObject(0);
                        dataArray.remove(0);
                    }catch (Exception e){
                        e.printStackTrace();
                        System.out.println("出现意料之外的错误");
                        System.exit(1);
                        continue;
                    } finally {
                        dataLock.unlock();
                    }
                    try {
                        IpfsApi.putDAG_json(IpfsApi.dagPb,IpfsApi.dagJson,jsonObject);
//                        System.out.println();
                    } catch (Exception e) {
                        System.out.println("出现一个失败块，已添加至重试队列");
                        dataLock.lock();
                        try {
                            dataArray.add(jsonObject);
                        }finally {
                            dataLock.unlock();
                        }
                    }
                }else {
                    stat = false;
                    ListPutDAGLinksStatusLock.lock();
                    try {
                        ListPutDAGLinksStatus.put(Thread.currentThread().getName(),false);
                        for(boolean s:ListPutDAGLinksStatus.values()){
                            if(s){
                                break;
                            }
                            return;
                        }
                    }finally {
                        ListPutDAGLinksStatusLock.unlock();
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {

                    }
                }
            }
        });
        thread.start();
        ListPutDAGLinks.add(thread);
    }
}

