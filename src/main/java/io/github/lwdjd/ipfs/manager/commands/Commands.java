package io.github.lwdjd.ipfs.manager.commands;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.github.lwdjd.ipfs.manager.JsonProcess;
import io.github.lwdjd.ipfs.manager.Main;
import io.github.lwdjd.ipfs.manager.api.ipfs.IpfsApi;
import io.github.lwdjd.ipfs.manager.config.ConfigManager;
import io.github.lwdjd.ipfs.manager.crust.AutoPins;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Commands {

    //获取某个CID下的文件(夹)列表
    private static List<HashMap<String,String>> getCidLinks(String cid) throws InterruptedException {
        TempFiles tempFileLinks = new TempFiles();
        TempFiles resultFiles = new TempFiles();


        JSONObject ipfsFileLinksJsonObj = JSONObject.parseObject(IpfsApi.getIpfsFileLinks(cid));
        JSONArray ipfsFileLinksJsonObjObjArray = ipfsFileLinksJsonObj.getJSONArray("Objects");
        JSONArray ipfsFileLinksJsonArray = ipfsFileLinksJsonObjObjArray.getJSONObject(0).getJSONArray("Links");
        List<HashMap<String,String>> ipfsFileLinks = JsonProcess.convertJsonToListHashMap(ipfsFileLinksJsonArray);

        for(HashMap<String,String> ipfsFileLink:ipfsFileLinks){
            String fileName = ipfsFileLink.get("Name");
            String fileType = ipfsFileLink.get("Type");
            if(fileType.equals("1")){
                tempFileLinks.addTempFiles(ipfsFileLink);
            }else if(fileType.equals("2")){
                if(!fileName.equals("")){
                    resultFiles.addTempFiles(ipfsFileLink);
                }
            }
        }

        //线程数
        int threadCount = 5;
        CountDownLatch latch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            int finalI = i;
            new Thread(() -> {
                try {// 执行线程任务
                    System.out.println("线程 " + finalI + " 已运行");
                    while (true) {
//                        System.out.println("记录点1");
                        HashMap<String,String> Link;
                        try {
                            Link = tempFileLinks.getAndRemoveTempFiles(0);
                        } catch (Exception e) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ex) {
                                throw new RuntimeException(ex);
                            }
                            try {
                                Link = tempFileLinks.getAndRemoveTempFiles(0);
                            }catch (Exception e2){
                                break;
                            }
                        }

                        List<HashMap<String,String>> ipfsFileLinks2;
                        try {
                            JSONObject ipfsFileLinksJsonObj2 = JSONObject.parseObject(IpfsApi.getIpfsFileLinks(Link.get("Hash")));
                            JSONArray ipfsFileLinksJsonObjObjArray2 = ipfsFileLinksJsonObj2.getJSONArray("Objects");
                            JSONArray ipfsFileLinksJsonArray2 = ipfsFileLinksJsonObjObjArray2.getJSONObject(0).getJSONArray("Links");
                            ipfsFileLinks2 = JsonProcess.convertJsonToListHashMap(ipfsFileLinksJsonArray2);
                        }catch (Exception e){
                            if(Objects.equals(Link.get("ErrorN"),null)){
                                Link.put("ErrorN","1");
                                System.out.println("此文件(夹)获取失败"+Link.get("ErrorN")+"次"+" cid:"+Link.get("Hash")+"  文件(夹)名:"+Link.get("Name"));
                                System.out.println("已添加至重试队列");
                                tempFileLinks.addTempFiles(Link);
                            }else {
                                int errorN;
                                try {
                                    errorN = (Integer.parseInt(Link.get("ErrorN"))+1);
                                }catch (Exception e2){
                                    System.out.println("此文件(夹)获取失败"+" 未知 次"+" cid:"+Link.get("Hash")+"  文件(夹)名:"+Link.get("Name"));
                                    System.out.println("已放弃");
                                    continue;
                                }
                                //设置文件最多重试3次
                                if ((errorN)>=3){
                                    Link.put("ErrorN",String.valueOf(errorN));
                                    System.out.println("此文件(夹)获取失败"+Link.get("ErrorN")+"次"+" cid:"+Link.get("Hash")+"  文件(夹)名:"+Link.get("Name"));
                                    System.out.println("已放弃");
                                }else {
                                    Link.put("ErrorN",String.valueOf(errorN));
                                    System.out.println("此文件(夹)获取失败"+Link.get("ErrorN")+"次"+" cid:"+Link.get("Hash")+"  文件(夹)名:"+Link.get("Name"));
                                    System.out.println("已添加至重试队列");
                                    tempFileLinks.addTempFiles(Link);
                                }
                            }
                            continue;
                        }
                        List<HashMap<String,String>> files = new ArrayList<>();
                        for(HashMap<String,String> ipfsFileLink:ipfsFileLinks2){
//                            System.out.println("记录点2");
                            String fileName = ipfsFileLink.get("Name");
                            String fileType = ipfsFileLink.get("Type");
                            if(fileType.equals("1")){
//                                System.out.println("记录点3");
                                tempFileLinks.addTempFiles(ipfsFileLink);
                            }else if(fileType.equals("2")){
//                                System.out.println("记录点4");
                                if(!fileName.equals("")){
//                                    System.out.println("记录点5");
                                    files.add(ipfsFileLink);
                                }
                            }
                        }
                        if (files.size()!=0){
//                            System.out.println("记录点6");
                            resultFiles.addAllTempFiles(files);
                        }
                    }
                } finally {
                    System.out.println("线程 " + finalI + " 已结束");
                    latch.countDown(); // 任务完成后计数减1
                }
            }).start();
        }

        latch.await(); // 等待所有线程执行完毕


        return resultFiles.getTempFiles();
    }

    private static List<HashMap<String,String>> getFileLinks(String path) throws Exception {
        TempFiles tempFileLinks = new TempFiles();
        TempFiles resultFiles = new TempFiles();

        //获取根目录文件(夹)Json
        String rootFilesJson = IpfsApi.getIpfsFileList("/",true,true);
        if(rootFilesJson.equals("")){
            throw new Exception("无返回数据，请检查ipfsApiUrl是否正确。");
        }
        JSONObject rootFilesJsonObj;
        JSONArray rootFilesJsonArray ;
        try {
            //处理json数据
            rootFilesJsonObj = JSONObject.parseObject(rootFilesJson);
            //获取文件(夹)列表json
            rootFilesJsonArray = rootFilesJsonObj.getJSONArray("Entries");
        }catch (Exception e){
            throw new Exception("解析根目录文件(夹)列表失败，请检查ipfsApiUrl是否正确，报错信息："+e.getMessage());
        }


        //根文件目录列表
        List<HashMap<String,String>> rootFiles = new ArrayList<>();
        for(int i=0;i<rootFilesJsonArray.size();i++){
            //获取根目录文件（夹）
            JSONObject fileJsonObj = rootFilesJsonArray.getJSONObject(i);
            //添加根目录文件(夹)（将JSONObject转为HashMap<String,String>）
            rootFiles.add(JsonProcess.convertJsonToHashMap(fileJsonObj));
        }
        String[] pathCell =path.split("/");
        if(pathCell.length==0){
            for(HashMap<String,String> rootFile:rootFiles){
                resultFiles.addAllTempFiles(getCidLinks(rootFile.get("Hash")));
            }
        }else {


            if(Objects.equals(pathCell[0], "")){
                pathCell = Arrays.copyOfRange(pathCell, 1, pathCell.length);
            }
            //遍历根文件目录列表
            for(int i=0;i<rootFiles.size();i++){
                if(pathCell.length!=0) {
                    if (rootFiles.get(i).get("Name").equals(pathCell[0])) {
                        if (rootFiles.get(i).get("Type").equals("1")) {
                            String tempCid = rootFiles.get(i).get("Hash");
                            for(int j=0;j<pathCell.length;j++){
                                if(j==pathCell.length-1){
//                                    System.out.println("记录点1:"+pathCell.length);
                                    resultFiles.addAllTempFiles(getCidLinks(tempCid));
                                }else {
                                    JSONObject jsonObject = JSONObject.parseObject(IpfsApi.getIpfsFileLinks(tempCid));
                                    JSONArray ipfsFileLinksJsonObjObjArray = jsonObject.getJSONArray("Objects");
                                    JSONArray ipfsFileLinksJsonArray = ipfsFileLinksJsonObjObjArray.getJSONObject(0).getJSONArray("Links");
                                    List<HashMap<String,String>> ipfsFileLinks = JsonProcess.convertJsonToListHashMap(ipfsFileLinksJsonArray);
                                    for (HashMap<String,String> ipfsFileLink : ipfsFileLinks){
//                                        System.out.println("记录点2");
                                        if(Objects.equals(ipfsFileLink.get("Type"), "1")){
//                                            System.out.println("记录点3:"+ Arrays.toString(pathCell)+j);
                                            if(Objects.equals(ipfsFileLink.get("Name"),pathCell[j+1])){
//                                                System.out.println("记录点4:Name"+ipfsFileLink.get("Name")+" , CID:"+ipfsFileLink.get("Hash"));
                                                tempCid = ipfsFileLink.get("Hash");
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (rootFiles.get(i).get("Type").equals("0")) {
                            if (pathCell.length == 1) {
                                List<HashMap<String, String>> file = new ArrayList<>();
                                file.add(rootFiles.get(i));
                                resultFiles.addAllTempFiles(file);
                            }
                        }
                        break;
                    }
                }else {
                    if (rootFiles.get(i).get("Type").equals("1")) {
                        resultFiles.addAllTempFiles(getCidLinks(rootFiles.get(i).get("Hash")));
                    } else if (rootFiles.get(i).get("Type").equals("0")) {
                        List<HashMap<String, String>> file = new ArrayList<>();
                        file.add(rootFiles.get(i));
                        resultFiles.addAllTempFiles(file);
                    }
                }
            }
        }
    return resultFiles.getTempFiles();
    }

    //临时测试
    public static void main(String[] args){
        ConfigManager.loadConfig("config.json");
        List<HashMap<String,String>> fileLinks = new ArrayList<>();
        try {
            fileLinks = getCidLinks("QmbbfDGyDUDo45179pyBS8YxQerto4HW6Y8ZfvSBCo9kQY");
//            fileLinks = getFileLinks("////");
        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println(fileLinks);
    }


    public static void getPinCids(){


        System.out.print("请输入需要获取的目录(以/开头):");
        String path = Main.scanner.nextLine();
        List<HashMap<String,String>> fileList;
        try {
            System.out.println("\n开始获取文件列表");
            fileList = getFileLinks(path);
        } catch (Exception e) {
            System.out.println("失败，原因："+e.getMessage()+"\n");
            return;
        }
        if(fileList.size()==0){
            System.out.println("没有找到文件，请检查路径是否正确。"+"\n");
            return;
        }
        System.out.println("获取完毕,共"+fileList.size()+"个文件\n");
        System.out.print("是否生成列表文件(y/n):");
        while (true) {
            String select = Main.scanner.nextLine();
            select = select.toLowerCase();
            if (select.equals("y") || select.equals("yes")) {
                System.out.print("请输入文件名(不含后缀):");
                String fileName = Main.scanner.nextLine();
                JSONObject fileJson = new JSONObject();
                JSONArray pins = new JSONArray();
                for(HashMap<String,String> file:fileList){
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("cid",file.get("Hash"));
                    jsonObject.put("fileName",file.get("Name"));
                    jsonObject.put("size",file.get("Size"));
                    pins.add(jsonObject);
                }
                fileJson.put("pins",pins);

                ConfigManager.saveConfig(AutoPins.pinsFile+fileName+".pins.json",fileJson);
                System.out.println("列表文件保存成功。\n");

                break;
            } else if (select.equals("n") || select.equals("no")) {
                System.out.println("已取消生成列表文件。\n");
                break;
            } else {
                System.out.print("未知命令请重新输入,是否生成列表文件(y/n):");
            }
        }
    }

    /**
     * 一键pin文件功能实现
     */
    public static void pins(){
        List<HashMap<String,String>> pins = new ArrayList<>();
        List<String> file = ConfigManager.filterPinsJsonFiles(ConfigManager.listFilesInDirectory(AutoPins.pinsFile));
        if(file.size()==0){
            System.out.println("\n"+"没有找到文件，请在pinsList文件夹添加以 .pins.json 结尾的CID列表文件。");
//            System.out.println("""
//                    格式：
//                    {
//                         "data": [
//                             {
//                                 "cid": "Qm...",
//                                 "fileName": "文件名"
//                             },
//                             {
//                                 "cid": "Qm...",
//                                 "fileName": "文件名"
//                             },
//                             {
//                                 "cid": "Qm...",
//                                 "fileName": "文件名"
//                             }
//                         ]
//                     }"""
//            );
            System.out.println("可以使用格式化工具将json格式化，方便查看。");
            return;
        }else {
            System.out.println("找到以下列表");
            for (String fileName : file) {
                System.out.println((file.indexOf(fileName) + 1) + ". " + fileName);

            }
            int iid;
            while (true) {
                System.out.print("输入编号(back返回)：");
                try {
                    String id = Main.scanner.nextLine();
                    if (id.equals("back")){
                        System.out.println();
                        return;
                    }
                    iid = Integer.parseInt(id);
                    if (iid<1||iid>file.size()){
                        System.out.print("输入有误，请重新");
                        continue;
                    }
                    break;
                } catch (Exception e) {
                    System.out.print("输入有误，请重新");
                }
            }
            try {
                ConfigManager.loadConfig(AutoPins.pinsFile+file.get(iid-1));
                JSONObject jsonObject = ConfigManager.getConfig(AutoPins.pinsFile+file.get(iid-1));
                JSONArray jsonArray = jsonObject.getJSONArray("pins");
                pins = JsonProcess.convertJsonToListHashMap(jsonArray);

            }catch (Exception e){
//                e.printStackTrace();
                System.out.println("文件加载失败,请检查文件json格式是否正确。");
                System.out.println("""
                        格式：
                        {
                             "pins": [
                                 {
                                     "cid": "Qm...",
                                     "fileName": "文件名"
                                 },
                                 {
                                     "cid": "Qm...",
                                     "fileName": "文件名"
                                 },
                                 {
                                     "cid": "Qm...",
                                     "fileName": "文件名"
                                 }
                             ]
                        }"""
                );
                return;
            }



        }

//        HashMap<String,String> temp = new HashMap<>();
//        temp.put("cid","bafybeidk7szrtw63kflrqrctjqnhhkunpsgok4dfq4dzvuavvqjtrjg7b4");
//        temp.put("fileName","测试bafybeidk7szrtw63kflrqrctj");
//        pins.add(new HashMap<>(temp));
//        temp.put("cid","QmNn1vTXNcietGe3spZKhnYNtEyC3sKGo5QkrfCrYGY5q3");
//        temp.put("fileName","测试QmNn1vTXN");
//        pins.add(new HashMap<>(temp));
//        temp.put("cid","QmV3svXGDiRsoVsNBGGmoSWLts6m3Uv43mGPis9zZrhrQs");
//        pins.add(new HashMap<>(temp));
//        temp.put("cid","QmY5LXc6s73nV6kVjwW8k9P8eBeTHX8ncmXGU8ezkkPq9C");
//        pins.add(new HashMap<>(temp));
//        temp.put("cid","QmSgz7pEfwDdjCLuvFk96oP4qK4rLN1CEoZr8NgMD6HqZT");
//        pins.add(new HashMap<>(temp));
//        temp.put("cid","bafkreicnv6jfk3tw7bg5x52hrtykn4e3bf7jmbdrjzdxeiaintas7zxtdu");
//        pins.add(new HashMap<>(temp));


        System.out.println("\n"+"已开始pins");
        AutoPins.sendPins(pins,AutoPins.publicSign);
        System.out.println("已结束");


        while ((ConfigManager.getConfig("config.json")==null?new JSONObject():ConfigManager.getConfig("config.json")).get("autoRetryPins").toString().equals("true")) {
            //重试失败的PINS
            int retryCount = AutoPins.getRetryList().size();
            if (retryCount > 0) {
                System.out.println("重试（" + retryCount + "）");
//                List<String> reCids = new ArrayList<>();
//                List<String> reFileNames = new ArrayList<>();
//                for (HashMap<String, String> retryItem : AutoPins.getAndClearRetryList()) {
//                    if (retryItem.get("cid") != null || retryItem.get("fileName") != null) {
//                        reCids.add(retryItem.get("cid"));
//                        reFileNames.add(retryItem.get("fileName"));
//                        //测试使用
////                    System.out.println("cid:" + retryItem.get("cid") + " fileName:" + retryItem.get("fileName"));
//                    }
//                }
                AutoPins.sendPins(AutoPins.getAndClearRetryList(), AutoPins.publicSign);
                System.out.println("已结束\n");

            }else {
                return;
            }
        }
    }



    public static void config(){
        while (true) {
            JSONObject config =ConfigManager.getConfig("config.json")==null?new JSONObject():ConfigManager.getConfig("config.json");
            System.out.println("\n"+"当前配置：");

            System.out.println("#是否自动重试失败项目");
            System.out.println("autoRetryPins(布尔值) = "+ Objects.equals(config.get("autoRetryPins"),true)+"\n");

//            System.out.println("autoRetryPins(布尔值) = "+config.get("autoRetryPins").equals("true")+"\n");

            System.out.println("#是否输出响应信息");
            System.out.println("outputReport(布尔值) = "+ Objects.equals(config.get("outputReport"),true)+"\n");

            System.out.println("#IPFS Api Url");
            System.out.println("ipfsApiUrl(字符串) = "+ (Objects.equals(config.get("ipfsApiUrl"),null)? IpfsApi.ifpsDefaultApiUrl:config.get("ipfsApiUrl"))+"\n");

            System.out.println("命令格式:[配置项] [修改值]");
            System.out.println("返回上一层请使用back命令"+"\n");
            System.out.print("请输入命令:");
            String command = Main.scanner.nextLine().toLowerCase();
            switch (command) {
                case "" -> System.out.println("命令不能为空！");
                case "back" -> {
                    System.out.println();
                    return;
                }
                default -> {
                    String[] commands = command.split(" ");
                    try {
                        switch (commands[0]) {
                            case "autoretrypins" -> {
                                if (commands.length == 2) {
                                    if (commands[1].equals("true")) {
                                        config.put("autoRetryPins", true);
                                        ConfigManager.saveConfig("config.json", config);
                                    } else if (commands[1].equals("false")) {
                                        config.put("autoRetryPins", false);
                                        ConfigManager.saveConfig("config.json", config);
                                    } else {
                                        System.out.println("正确格式(不区分大小写):autoRetryPins [布尔值]");
                                    }
                                } else {
                                    System.out.println("正确格式(不区分大小写):autoRetryPins [布尔值]");
                                }
                            }
                            case "outputreport" -> {
                                if (commands.length == 2) {
                                    if (commands[1].equals("true")) {
                                        config.put("outputReport", true);
                                        ConfigManager.saveConfig("config.json", config);
                                    } else if (commands[1].equals("false")) {
                                        config.put("outputReport", false);
                                        ConfigManager.saveConfig("config.json", config);
                                    } else {
                                        System.out.println("正确格式(不区分大小写):outputReport [布尔值]");
                                    }
                                } else {
                                    System.out.println("正确格式(不区分大小写):outputReport [布尔值]");
                                }
                            }
                            case "ipfsapiurl" -> {
                                if (commands.length == 2) {
                                    config.put("ipfsApiUrl", commands[1]);
                                    ConfigManager.saveConfig("config.json", config);
                                } else {
                                    System.out.println("正确格式(不区分大小写):ipfsApiUrl [URL字符串]");
                                }
                            }
                            default -> System.out.println("未知命令！");
                        }
                    } catch (Exception e) {
                        System.out.println("未知命令！");
                    }
                }
            }
        }
    }

    public static void help(){
        System.out.println("\n命令列表：");
        System.out.println("getcids:自动获取目录下所有文件CID列表(预发布版本可能会卡住；多线程？)，并生成.pins.json文件；");
        System.out.println("pins:自动批量pin文件到Crust；");
        System.out.println("config:修改配置文件；");
        System.out.println("help:显示帮助；");
        System.out.println("exit:退出程序。");
        System.out.println();
    }
}
class TempFiles{
    private final Lock tempFilesLock=new ReentrantLock(true);

    //等待被查询的列表
    private final List<HashMap<String,String>> tempFiles =new ArrayList<>();

    public void addAllTempFiles(List<HashMap<String,String>> tempFiles){
        try {
            tempFilesLock.lock();
            this.tempFiles.addAll(tempFiles);
        }finally {
            tempFilesLock.unlock();
        }
    }

    public void addTempFiles(HashMap<String,String> tempFile){

        try {
            tempFilesLock.lock();
            this.tempFiles.add(tempFile);
        }finally {
            tempFilesLock.unlock();
        }
    }

    public List<HashMap<String,String>> getTempFiles(){

        try {
            tempFilesLock.lock();
            return new ArrayList<>(tempFiles);
        }finally {
            tempFilesLock.unlock();
        }
    }
    public void clearTempFiles(){

        try {
            tempFilesLock.lock();
            tempFiles.clear();
        }finally {
            tempFilesLock.unlock();
        }
    }
    public void removeAllTempFiles(List<HashMap<String,String>> tempFiles){
        try {
            tempFilesLock.lock();
            this.tempFiles.removeAll(tempFiles);
        }finally {
            tempFilesLock.unlock();
        }
    }

    public HashMap<String,String> getAndRemoveTempFiles(int i){
        try {
            tempFilesLock.lock();
            return new HashMap<String,String>(tempFiles.get(i));
        }finally {
            try {
                tempFiles.remove(i);
            }finally {
                tempFilesLock.unlock();
            }

        }
    }

    public List<HashMap<String,String>> getAndClearTempFiles(){
        try {
            tempFilesLock.lock();
            return new ArrayList<>(tempFiles);
        }finally {
            try {
                tempFiles.clear();
            }finally {
                tempFilesLock.unlock();
            }

        }
    }

}
