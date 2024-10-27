package io.github.lwdjd.ipfs.manager;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.*;

public class JsonProcess {
    public static HashMap<String, String> convertJsonToHashMap(JSONObject jsonObject) {
        // 将JSONObject转换为HashMap<String, String>
        HashMap<String, String> stringMap = new HashMap<>();
        Set<String> keys = jsonObject.keySet();
        for (String key : keys) {
            // 假设所有值都是字符串，如果不是，这里可能需要类型检查和转换
            stringMap.put(key, jsonObject.getString(key));
        }
        return stringMap;
    }
    public static List<HashMap<String,String>> convertJsonToListHashMap(JSONArray jsonArray){
        // 将JSONArray转换为List<HashMap<String, String>>
        List<HashMap<String, String>> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            HashMap<String, String> map = new HashMap<>();
            for (String key : jsonObject.keySet()) {
                map.put(key, jsonObject.getString(key));
            }
            list.add(map);
        }
        return list;
    }
}

