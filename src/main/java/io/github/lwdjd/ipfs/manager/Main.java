package io.github.lwdjd.ipfs.manager;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.github.lwdjd.ipfs.manager.commands.Commands;
import io.github.lwdjd.ipfs.manager.config.ConfigManager;
import io.github.lwdjd.ipfs.manager.crust.AutoPins;

import java.util.HashMap;
import java.util.Objects;
import java.util.Scanner;


public class Main {
    public static Scanner scanner = new Scanner(System.in);

    /**
     * 初始化
     */
    private static void init(){
        ConfigManager.loadConfig("config.json");
        scanner.useDelimiter("\n");

        //添加PINS监听器
        AutoPins.addPinsListener((int total,int pinned,String cid,String fileName,String report)->{
            //处理PINS结果
//            new Thread(()->{
                try {

                    JSONObject reportJsonObject = JSON.parseObject(report);
                    JSONArray pin = reportJsonObject.getJSONArray("Pin");
                    if(reportJsonObject.get("requestid")==null||reportJsonObject.get("requestid")==""){
                        throw new Exception("未成功固定");
                    }
                    System.out.print("成功");
                } catch (Exception e) {
                    HashMap<String,String> item = new HashMap<>();
                    item.put("cid",cid);
                    item.put("fileName",fileName);
                    AutoPins.retryListAdd(item);
                    System.out.print("失败");
                }finally {
                    System.out.println("（"+pinned+"/"+total+"）");
                    if (Objects.equals((ConfigManager.getConfig("config.json")==null?new JSONObject():ConfigManager.getConfig("config.json")).get("outputReport"),true)) {
                        System.out.println("Report：" + "\n" + report);
                    }
                }
//            }).start();
            }
        );
    }
    public static void main(String[] args) {
        //初始化
        init();
        while (true) {
            //欢迎信息
            System.out.println("欢迎使用IPFS Manager");
            System.out.println("help查看帮助");
            System.out.print("请输入命令:");
            String command = scanner.nextLine().toLowerCase();
            switch (command) {
                case "getcids" -> Commands.getPinCids();
                case "pins" -> Commands.pins();
                case "config" -> Commands.config();
                case "packaging" -> Commands.packaging();
                case "exit" -> {
                    return;
                }
                default -> Commands.help();
            }
        }
    }
}
