package io.github.lwdjd.ipfs.manager.network;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.github.lwdjd.ipfs.manager.Main;
import io.github.lwdjd.ipfs.manager.api.ipfs.IpfsApi;
import io.github.lwdjd.ipfs.manager.config.ConfigManager;
import io.github.lwdjd.ipfs.manager.process.StorageFormatter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * 文件预热器
 */
public class FilePreheater {
//    //这条为测试使用
//    public static ConcurrentHashMap<String, AtomicLong> locations = new ConcurrentHashMap<>();
    public FilePreheater(String gateway){
        this.gateway = gateway;
    }
    private final String gateway;
    /**
     * 每个文件分块的范围
     * key:文件URL
     * value:文件分块的范围
     */
    private final ConcurrentHashMap<String, FileRanges> filesRange = new ConcurrentHashMap<>();
    /**
     * 每个文件不同分块已经读取到的位置
     * key:文件URL
     * value:不同文件分块已经读取到的位置
     * key:分块ID
     * value:已经读取到的位置
     */
    private final ConcurrentHashMap<String,ConcurrentHashMap<Integer,AtomicLong>> filesLocationsMap = new ConcurrentHashMap<>();

    ExecutorService executor;
    //检查下载进度
    private void inspectDownload(String fileURL,Integer blockID){
//        System.out.println("\n\n信息："+fileURL+"+"+blockID);
//        System.out.println("起始位置："+filesRange.get(fileURL).getRange(blockID)[0]+"   格式化："+ StorageFormatter.formatBytes(filesRange.get(fileURL).getRange(blockID)[0]));
//        System.out.println("结束位置："+filesRange.get(fileURL).getRange(blockID)[1]+"   格式化："+ StorageFormatter.formatBytes(filesRange.get(fileURL).getRange(blockID)[1]));
//        System.out.println("总大小："+(filesRange.get(fileURL).getRange(blockID)[1]-filesRange.get(fileURL).getRange(blockID)[0]+1)+"   格式化："+ StorageFormatter.formatBytes((filesRange.get(fileURL).getRange(blockID)[1]-filesRange.get(fileURL).getRange(blockID)[0])));
//        System.out.println("当前已下载量："+filesLocationsMap.get(fileURL).get(blockID).get()+"   格式化："+ StorageFormatter.formatBytes(filesLocationsMap.get(fileURL).get(blockID).get()));
//        System.out.println("下载停止位置："+(filesRange.get(fileURL).getRange(blockID)[0]+filesLocationsMap.get(fileURL).get(blockID).get()-1)+"   格式化："+ StorageFormatter.formatBytes((filesRange.get(fileURL).getRange(blockID)[0]+filesLocationsMap.get(fileURL).get(blockID).get()-1)));

        executor.execute(() -> {
            try {
                if (!filesLocationsMap.containsKey(fileURL)) {
                    filesLocationsMap.put(fileURL, new ConcurrentHashMap<>());
                }
                if (filesLocationsMap.get(fileURL).containsKey(blockID)) {
                    //已经下载过的文件
                    //检查是否下载完成
                    if (!(filesLocationsMap.get(fileURL).get(blockID).get() > (filesRange.get(fileURL).getRange(blockID)[1] - filesRange.get(fileURL).getRange(blockID)[0]))) {
                        //未下载完成
                        String url = gateway + fileURL;
                        long startByte = filesLocationsMap.get(fileURL).get(blockID).get() + filesRange.get(fileURL).getRange(blockID)[0];
                        Network.fakeDownload(url, startByte, filesRange.get(fileURL).getRange(blockID)[1], filesLocationsMap.get(fileURL).get(blockID));
                    }else {
                        return;
                    }
                } else {
                    //未下载过的文件
                    String url = gateway + fileURL;
                    filesLocationsMap.get(fileURL).put(blockID, new AtomicLong(0));
                    Network.fakeDownload(url, filesRange.get(fileURL).getRange(blockID)[0], filesRange.get(fileURL).getRange(blockID)[1], filesLocationsMap.get(fileURL).get(blockID));

                }
            }catch (Exception ignored){

            }
             inspectDownload(fileURL, blockID);

        });
    }
    /**
     * 启动全部线程
     * @param fileURL 文件URL
     */
    private void startThreads(String fileURL){
        if(filesRange.containsKey(fileURL)) {
            for (int i = 0; filesRange.get(fileURL).size() > i; i++) {
                int finalI = i;
                executor.execute(() -> {
                    try {
                        if (!filesLocationsMap.containsKey(fileURL)) {
                            filesLocationsMap.put(fileURL, new ConcurrentHashMap<>());
                        }
                        if (filesLocationsMap.get(fileURL).containsKey(finalI)) {
                            //已经下载过的文件
                            //检查是否下载完成
                            if (!(filesLocationsMap.get(fileURL).get(finalI).get() > (filesRange.get(fileURL).getRange(finalI)[1] - filesRange.get(fileURL).getRange(finalI)[0]))) {
                                //未下载完成
                                String url = gateway + fileURL;
                                long startByte = filesLocationsMap.get(fileURL).get(finalI).get() + filesRange.get(fileURL).getRange(finalI)[0];
                                Network.fakeDownload(url, startByte, filesRange.get(fileURL).getRange(finalI)[1], filesLocationsMap.get(fileURL).get(finalI));

                            }else {
                                return;
                            }
                        } else {
                            //未下载过的文件
                            String url = gateway + fileURL;
                            filesLocationsMap.get(fileURL).put(finalI, new AtomicLong(0));
                            Network.fakeDownload(url, filesRange.get(fileURL).getRange(finalI)[0], filesRange.get(fileURL).getRange(finalI)[1], filesLocationsMap.get(fileURL).get(finalI));

                        }
                    }catch (Exception ignored){

                    }
                    inspectDownload(fileURL, finalI);


                });
            }
        }else{
            throw new RuntimeException("\n"+"路径"+fileURL+"没有分块数据");
        }
    }

    public void fakeDownload(String fileURL,long blockMaxSize,long length){

        //文件分块
        segFileRange(fileURL,length,blockMaxSize);
        //启动线程
        startThreads(fileURL);
        System.out.println("\n"+
                "信息："+gateway+fileURL+"\n"+
                "分块数量："+filesRange.get(fileURL).size()+"\n"+
                "线程已全部启动"
        );
    }
    /*
    思路：
    1.创建下载线程池并限制数量；
    2.创建任务表，为每一个分块下载任务分配线程；
    3.按照顺序和间隔时间启动线程；
    4.创建心跳检测线程，专门用来检测下载是否正常和是否结束；
    4.1.间隔一段时间检测一次所有线程的任务进度并记录，只保留一次数据进行比对；
    4.2.如果发现某个线程任务进度长时间没有更新，则认为该线程出现异常，关闭该线程，并重新在已下载进度处开启新的线程，替换任务表中的线程；
    5.线程池监听线程完成事件，当一个线程完成后检测线程表是否有剩余任务；
    5.1.如果有任务则检测线程池是否满了，如果没满就按照顺序添加任务；
    5.2.如果线程池满了，则无任何操作；
    5.3.如果任务全部完成，并且没有剩余正在运行的线程，则关闭线程池；
     */

    /**
     * 按照大小为文件分块，单位 字节 。
     * 分块结果会放在filesRange
     * 理想块大小==45088768
     * @param fileURL  CID
     * @param length  文件长度
     * @param blockMaxSize 最大块大小
     */
    private void segFileRange(String fileURL,long length,long blockMaxSize) {
        if(length<1){
            throw new IllegalArgumentException("文件长度不能小于1");
        }
        /*
         * 文件分块的范围
         * index:分块的序号
         * long[0]:分块的起始位置
         * long[1]:分块的终止位置
         */
        ArrayList<long[]> ranges = new ArrayList<>();
        length--;
        if (blockMaxSize<=length){
            long i = blockMaxSize-1;
            for (; i < length; i += blockMaxSize) {
                long[] range = new long[]{i-(blockMaxSize-1), i};
                ranges.add(range);
            }
            if((i-blockMaxSize)<length){
                long[] range = new long[]{i-(blockMaxSize-1),length};
                ranges.add(range);
            }
        }else{
            long[] range = new long[]{0,length};
            ranges.add(range);
        }
        filesRange.put(fileURL,new FileRanges(ranges));
    }
    public static void start(String gateway,String fileURL,int nThreads,long blockMaxSize){
        FilePreheater a = new FilePreheater(gateway);
        a.executor = Executors.newFixedThreadPool(nThreads);
        //获取文件大小
        long length=-1L;

        //限制重试次数
        int retryTimes = 10;
        for (int i=0;i<retryTimes;i++){
            try {
                length = Long.parseLong(Objects.requireNonNull(Network.getHeaders(gateway + fileURL).get("content-length")));

            }catch (Exception ignored){

            }
            if(!Objects.equals(length,-1L)){
                break;
            }else{
                if(i==retryTimes-1){
                    System.out.println("\n"+"信息："+gateway+fileURL+"\n"+ (i + 1) + "次尝试获取文件大小失败,已放弃。");
                    return;
                }else {
                    System.out.println("\n" + "信息：" + gateway + fileURL + "\n" + "获取文件大小失败(第" + (i + 1) + "次)");
                }
            }
            try {
                //重试间隔时间
                Thread.sleep(20000);
            } catch (InterruptedException ignored) {

            }
        }

        a.fakeDownload(fileURL,blockMaxSize,length);
        //每隔10秒检测一次下载进度
        long finalLength = length;
        new Thread(()->{
            while (true) {
                try {

                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ignored) {

                    }
                    long b = 0L;
//                System.out.println("当前下载进度：");
                    for (String fileURL2 : a.filesLocationsMap.keySet()) {
                        for (int i = 0; i < a.filesLocationsMap.get(fileURL2).size(); i++) {
//                        System.out.print(fileURL2+"分块"+i+"："+a.filesLocationsMap.get(fileURL2).get(i).get()+"   ");
                            b += a.filesLocationsMap.get(fileURL2).get(i).get();
                        }
                    }
                    System.out.println();
                    System.out.println(
                            "网关：" + gateway+"\n"+
                            "路径：" + fileURL+"\n"+
                            "总下载进度：" + b + "/" + finalLength + "   格式化：" + StorageFormatter.formatBytes(b) + "/" + StorageFormatter.formatBytes(finalLength)+"\n"
                    );

                    if (Objects.equals(finalLength, b)) {
                        System.out.println("下载完成");
                        a.executor.shutdownNow();
                        return;
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }
//    public static void main(String[] args){
//        long blockMaxSize = 1048576L*2;
//        int nThreads = 128;
//        //路径
//        HashMap<String,String> pathMap = new HashMap<>();
//        pathMap.put("ipfs","/ipfs/bafybeifqqmz3fxoloeaahzph36smhdayutqefu64egae4fkqp5tkl5dkl4");
//        pathMap.put("ipns","/ipns/temp.lwdjd.top");
//
//        HashMap<String,HashSet<String>> getaways = new HashMap<>();
//        getaways.put("ipfs", new HashSet<>());
//        getaways.put("ipns", new HashSet<>());
//
//        //ipns
//        getaways.get("ipns").add("https://gw.crustgw.org");
//        getaways.get("ipns").add("https://gw.crustgw.work");
//        getaways.get("ipns").add("https://gw.crust-gateway.xyz");
//        getaways.get("ipns").add("https://gw.crust-gateway.com");
//
//        //ipfs
//        getaways.get("ipfs").add("https://gw.w3ipfs.org.cn");
//        getaways.get("ipfs").add("https://gw.smallwolf.me");
//        getaways.get("ipfs").add("https://ipfs.cloud.ava.do");
//        getaways.get("ipfs").add("https://ipfs.test.bitmark.com");
//        getaways.get("ipfs").add("https://ipfs.hypha.coop");
//        getaways.get("ipfs").add("https://ipfs.oplanto.com");
//        getaways.get("ipfs").add("https://ipfs-8.yoghourt.cloud");
//        getaways.get("ipfs").add("https://ipfs-9.yoghourt.cloud");
//        getaways.get("ipfs").add("https://ipfs-10.yoghourt.cloud");
//        getaways.get("ipfs").add("https://ipfs-11.yoghourt.cloud");
//        getaways.get("ipfs").add("https://ipfs-12.yoghourt.cloud");
//
//        //启动所有项目
//        for(String Key : getaways.keySet()){
//            for(String gateway : getaways.get(Key)){
//                new Thread(() -> start(
//                        gateway,
//                        pathMap.get(Key),
//                        nThreads,
//                        blockMaxSize
//                )).start();
//            }
//        }
//
//
//
////        new Thread(() -> {
////            start(
////                    "https://gw.crustgw.org",
////                    "/ipns/temp.lwdjd.top",
////                    nThreads,
////                    blockMaxSize
////            );
////        }).start();
//
//    }
//    /**
//     * 仅测试使用
//     */
//    public static void main(String[] args){
//        String gateway = "https://gw.crustgw.org";
//        String fileURL = "/ipns/temp.lwdjd.top";
//
//        FilePreheater a = new FilePreheater(gateway);
//        a.executor = Executors.newFixedThreadPool(32);
//        long blockMaxSize = 1048576L*2;
//        //获取文件大小
//        long length;
//        try {
//            length = Long.parseLong(Objects.requireNonNull(Network.getHeaders(gateway + fileURL).get("content-length")));
//        }catch (Exception e){
//            throw new RuntimeException("获取文件大小失败",e);
//        }
//        a.fakeDownload(fileURL,blockMaxSize);
//        System.out.println("线程已全部启动");
//        //每隔10秒检测一次下载进度
//        new Thread(()->{
//            while (System.currentTimeMillis()!=-1){
//                try {
//                    Thread.sleep(10000);
//                } catch (InterruptedException ignored) {
//
//                }
//                long b = 0L;
////                System.out.println("当前下载进度：");
//                for (String fileURL2 : a.filesLocationsMap.keySet()) {
//                    for (int i = 0; i < a.filesLocationsMap.get(fileURL2).size(); i++) {
////                        System.out.print(fileURL2+"分块"+i+"："+a.filesLocationsMap.get(fileURL2).get(i).get()+"   ");
//                        b+=a.filesLocationsMap.get(fileURL2).get(i).get();
//                    }
//                }
//                System.out.println();
//                System.out.println("网关："+gateway);
//                System.out.println("路径："+fileURL);
//                System.out.println("总下载进度："+b+"/"+ length +"   格式化："+StorageFormatter.formatBytes(b)+"/"+ StorageFormatter.formatBytes(length) );
//
//                if(Objects.equals(length,b)){
//                    System.out.println("下载完成");
//                    a.executor.shutdownNow();
//                    return;
//                }
//            }
//        }).start();
//    }
//    /**
//     * 仅测试使用
//     */
//    public static void main(String[] args){
//        FilePreheater a = new FilePreheater();
//        a.segFileRange("1234567890",45088770L,45088768L);
//        System.out.println(a.filesRange.get("1234567890").toString());
//        System.out.println("分块数量："+a.filesRange.get("1234567890").size());
////        System.out.println(StorageFormatter.formatBytes(96827391943376896L));
//    }

    public static void filePreheat(){
        //读取网关
        JSONObject getaway = ConfigManager.getConfig("getaway.json")==null?new JSONObject():ConfigManager.getConfig("getaway.json");
        if(getaway.size()==0){
            System.out.println("未配置网关，请先配置网关。");
            return;
        }

        Map<String, Set<String>> getawayMap = new HashMap<>();
        for (String key : getaway.keySet()) {
            JSONArray jsonArray = getaway.getJSONArray(key);
            Set<String> set = new HashSet<>();
            for (int i = 0; i < jsonArray.size(); i++) {
                set.add(jsonArray.getString(i));
            }
            getawayMap.put(key, set);
        }
        System.out.println("已配置网关：");
        for (Map.Entry<String, Set<String>> entry : getawayMap.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }

        //获取配置
        JSONObject config =ConfigManager.getConfig("config.json")==null?new JSONObject():ConfigManager.getConfig("config.json");
        Pattern pattern = Pattern.compile("^/[^\n/]+(/[^/\n]+)+/?$");
        HashMap<String,HashSet<String>> pathMap = new HashMap<>();
        while(true){
            System.out.print("""
                            
                            提示：finish 结束输入
                            请输入下载路径："""
            );
            String path = Main.scanner.nextLine();
            if (path.equals("finish")) {
                if (pathMap.size() > 0) {
                    System.out.println("已完成添加路径，开始下载...");
                    break;
                }else {
                    System.out.println("未添加任何路径，退出。");
                    return;
                }
            } else {
                //匹配正则表达式
                if (pattern.matcher(path).matches()) {
                    String[] pathTemp = path.split("/");
                    //添加路径
                    if (Objects.equals(pathMap.get(pathTemp[0]), null)) {
                        HashSet<String> setTemp = new HashSet<>();
                        setTemp.add(path);
                        pathMap.put(pathTemp[1], setTemp);
                    } else {
                        pathMap.get(pathTemp[1]).add(path);
                    }
                    System.out.println("路径已添加：" + path);
                } else {
                    System.out.println("路径格式不正确，请重新输入!!");
                }
            }

        }

        System.out.println("记录点2："+pathMap);
        int nThreads = (Objects.equals(config.getInteger("maxPreheatThreads"),null)? IpfsApi.maxPreheatThreads:config.getInteger("maxPreheatThreads"));
        long blockMaxSize = (Objects.equals(config.getLong("maxPreheatBlockSize"),null)? IpfsApi.maxPreheatBlockSize:config.getLong("maxPreheatBlockSize"));
        //启动所有项目
        for (String Key : getawayMap.keySet()) {
            System.out.println("记录点1："+Key);
            for (String path:pathMap.get(Key)) {
                for (String gateway : getawayMap.get(Key)) {
                    new Thread(() -> start(
                            gateway,
                            path,
                            nThreads,
                            blockMaxSize
                    )).start();
                }
            }
        }



    }
    //测试使用
    public static void main(String[] args){
        ConfigManager.loadConfig("getaway.json");
        ConfigManager.loadConfig("config.json");
        filePreheat();
    }


}
class FileRanges {
    /**
     * 文件分块的范围
     * index:分块的序号
     * long[0]:分块的起始位置
     * long[1]:分块的终止位置
     */
    private final ArrayList<long[]> fileRange;
    public FileRanges(ArrayList<long[]> fileRange){
        this.fileRange = new ArrayList<>(fileRange);
    }

    public long[] getRange(int index){
        return fileRange.get(index);
    }

    public int size(){
        return fileRange.size();
    }

    public FileRanges clone(){
        return new FileRanges(fileRange);
    }
    @Override
    public String toString(){
        String temp = "[";
        for (long[] range : fileRange) {
            temp += "["+range[0]+","+range[1]+"],";
        }
        temp = temp.substring(0,temp.length()-1)+"]";

        return temp;
    }
}