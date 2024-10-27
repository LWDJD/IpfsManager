package io.github.lwdjd.ipfs.manager.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    private static final Map<String, JSONObject> configFiles = new HashMap<>();
    // 路径前缀，指向类路径中的资源文件夹
    private static final String CLASSPATH_PREFIX = "/io/github/lwdjd/ipfs/manager/defaultConfig/";


    public static boolean loadConfig(String relativePath) {
        // 首先尝试从文件系统加载配置文件
        try {
            Path configFilePath = Paths.get(relativePath);
            if (Files.exists(configFilePath)) {
                String fileContent = Files.readString(configFilePath);
                JSONObject config = JSON.parseObject(fileContent);
                configFiles.put(relativePath, config);
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            // 文件系统读取失败，继续尝试从类路径加载
        }

        // 文件系统读取失败或文件不存在，尝试从类路径加载默认配置
        try (InputStream is = ConfigManager.class.getResourceAsStream(CLASSPATH_PREFIX+relativePath)) {
            if (is != null) {
                String defaultContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                JSONObject defaultConfig = JSON.parseObject(defaultContent);
                configFiles.put(relativePath, defaultConfig);
                saveDefaultConfig(relativePath);
                return true;
            } else {
                // 类路径中也未找到配置文件
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 保存配置文件的方法，接收配置的相对路径和JSONObject对象
    public static boolean saveConfig(String relativePath, JSONObject config) {
        try {
            // 获取配置文件的路径和父目录路径
            Path path = Paths.get(relativePath);
            Path dirPath = path.getParent();

            // 确保配置文件所在的目录存在
            if (dirPath != null && !Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            // 将JSONObject转换为JSON字符串并写入文件
            String configJson = JSON.toJSONString(config);
            Files.writeString(path, configJson);

            // 更新内存中的配置缓存
            configFiles.put(relativePath, config);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 获取配置文件
    public static JSONObject getConfig(String relativePath) {
        return configFiles.get(relativePath);
    }

    // 保存默认配置文件到指定路径
    public static boolean saveDefaultConfig(String relativePath) {
        // 尝试从类路径加载默认配置文件
        String defaultConfigPath = CLASSPATH_PREFIX + relativePath;
        try (InputStream is = ConfigManager.class.getResourceAsStream(defaultConfigPath)) {
            if (is != null) {
                // 如果找到了默认配置文件，读取内容
                String defaultContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                JSONObject defaultConfig = JSON.parseObject(defaultContent);
                return saveConfig(relativePath, defaultConfig);
            }
            // 如果没有找到，返回false
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    public static boolean updataConfig(String relativePath, JSONObject newConfig) {
        try {
            // 覆盖私有变量中的配置文件
            configFiles.put(relativePath, newConfig);
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

        // （可选）保存到文件系统，如果需要持久化
        // saveConfig(relativePath, newConfig);
        return true;
    }
//    public static void main(String[] args) {
//        InputStream is = ConfigManager.class.getResourceAsStream("/io/github/lwdjd/chain/message/defaultConfig/config.json");
//        if (is != null) {
//            try {
//                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
//                System.out.println("Successfully loaded default config: " + content);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        } else {
//            System.out.println("Default config not found in classpath.");
//        }
//    }
    public static List<String> listFilesInDirectory(String path) {
        // 创建一个ArrayList来存储文件名
        List<String> fileNames = new ArrayList<>();

        // 将路径字符串转换为Path对象
        Path directoryPath = Path.of(path).toAbsolutePath().normalize();

        // 检查路径是否存在，如果不存在则创建包含父目录的路径
        try {
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }
        } catch (IOException e) {
            System.err.println("Failed to create directory: " + e.getMessage());
            return fileNames; // 返回空列表
        }

        // 读取目录中的文件
        try {
            Files.list(directoryPath)
                    .filter(Files::isRegularFile) // 过滤出普通文件
                    .forEach(file -> fileNames.add(file.getFileName().toString())); // 添加文件名到列表
        } catch (IOException e) {
            System.err.println("Error reading directory: " + e.getMessage());
        }

        return fileNames; // 返回包含文件名的列表
    }

    public static List<String> filterPinsJsonFiles(List<String> fileNames) {
        // 创建一个新的ArrayList来存储以.pins.json结尾的文件名
        List<String> pinsJsonFiles = new ArrayList<>();

        // 遍历传入的文件名列表
        for (String fileName : fileNames) {
            // 检查文件名是否以.pins.json结尾
            if (fileName.endsWith(".pins.json")) {
                pinsJsonFiles.add(fileName);
            }
        }

        return pinsJsonFiles; // 返回筛选后的文件名列表
    }

    public static void main(String[] args){
        List<String> fileNames = filterPinsJsonFiles(ConfigManager.listFilesInDirectory("pinsList/"));
        for (String fileName : fileNames){
            System.out.println(fileName);
        }
    }
}