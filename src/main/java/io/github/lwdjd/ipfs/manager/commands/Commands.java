package io.github.lwdjd.ipfs.manager.commands;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.github.lwdjd.ipfs.manager.JsonProcess;
import io.github.lwdjd.ipfs.manager.Main;
import io.github.lwdjd.ipfs.manager.config.ConfigManager;
import io.github.lwdjd.ipfs.manager.crust.AutoPins;

import java.util.*;

public class Commands {
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
                    if (iid<1||iid>file.size()+1){
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
                e.printStackTrace();
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
            System.out.println("命令格式:[配置项] [修改值]");
            System.out.println("返回上一层请使用back命令"+"\n");
            System.out.print("请输入命令:");
            String command = Main.scanner.nextLine().toLowerCase();
            switch (command) {
                case "":
                    System.out.println("命令不能为空！");
                    break;
                case "back":
                    System.out.println();
                    return;
                default:
                    String[] commands = command.split(" ");
                    try {
                        switch (commands[0]){
                            case "autoretrypins":
                                if(commands.length==2){
                                    if(commands[1].equals("true")){
                                        config.put("autoRetryPins",true);
                                        ConfigManager.saveConfig("config.json",config);
                                    }else if(commands[1].equals("false")){
                                        config.put("autoRetryPins",false);
                                        ConfigManager.saveConfig("config.json",config);
                                    } else {
                                        System.out.println("正确格式(不区分大小写):autoRetryPins [布尔值]");
                                    }
                                }else {
                                    System.out.println("正确格式(不区分大小写):autoRetryPins [布尔值]");
                                }
                                break;
                            case "outputreport":
                                if(commands.length==2){
                                    if(commands[1].equals("true")){
                                        config.put("outputReport",true);
                                        ConfigManager.saveConfig("config.json",config);
                                    }else if(commands[1].equals("false")){
                                        config.put("outputReport",false);
                                        ConfigManager.saveConfig("config.json",config);
                                    } else {
                                        System.out.println("正确格式(不区分大小写):outputReport [布尔值]");
                                    }
                                }else {
                                    System.out.println("正确格式(不区分大小写):outputReport [布尔值]");
                                }
                                break;
                            default:
                                System.out.println("未知命令！");
                        }
                    }catch (Exception e){
                        System.out.println("未知命令！");
                    }

            }
        }
    }

    public static void help(){
        System.out.println("命令列表：");
        System.out.println("pins:自动批量pin文件到Crust");
        System.out.println("config:修改配置文件");
        System.out.println("help:显示帮助");
        System.out.println("exit:退出程序");
        System.out.println();
    }
}
