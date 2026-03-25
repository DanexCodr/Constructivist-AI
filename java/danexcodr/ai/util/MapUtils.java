package danexcodr.ai.util;

import java.util.*;

public class MapUtils {
    
    public static <K> Integer incrementCount(Map<K, Integer> map, K key) {
        Integer count = map.get(key);
        return (count == null) ? 1 : count + 1;
    }
    
    public static <K> void mergeCountMap(Map<K, Integer> target, Map<K, Integer> source) {
        for (Map.Entry<K, Integer> entry : source.entrySet()) {
            K key = entry.getKey();
            Integer count = entry.getValue();
            Integer current = target.get(key);
            target.put(key, (current == null ? 0 : current) + count);
        }
    }
    
    public static <K, V> boolean mapContainsAny(Map<K, V> map, Collection<K> keys) {
        for (K key : keys) {
            if (map.containsKey(key)) {
                return true;
            }
        }
        return false;
    }
    
    public static <K, V> Map<K, V> createMapFromLists(List<K> keys, List<V> values) {
        if (keys.size() != values.size()) {
            throw new IllegalArgumentException("Keys and values lists must have same size");
        }
        
        Map<K, V> map = new HashMap<K, V>();
        for (int i = 0; i < keys.size(); i++) {
            map.put(keys.get(i), values.get(i));
        }
        return map;
    }
}