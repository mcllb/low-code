package tcdx.uap.common.utils;

import java.util.*;

public class MapUtils {

    public static Map<String, Object> New(String key ,Object val){
        Map<String,Object> map = new HashMap<>();
        map.put(key, val);
        return map;
    }

    public static Map<String, Object> Combine(Map<String, Object>...args){
        Map<String,Object> map = new HashMap<>();
        for(Map<String, Object> map1 : args) {
            map.putAll(map1);
        }
        return map;
    }

    public static Map G(Object ...args){
        Map map = new HashMap<>();
        for(int i=0;i<args.length-1; i+=2) {
            map.put(args[i], args[i + 1]);
        }
        return map;
    }

    public static void copy(Map<String, Object> m1, Map<String, Object> m2){
        for(Map.Entry<String, Object> entry : m1.entrySet()) {
            m2.put(entry.getKey(), entry.getValue());
        }
    }

}
