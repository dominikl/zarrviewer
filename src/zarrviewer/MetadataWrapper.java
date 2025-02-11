package zarrviewer;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class MetadataWrapper {

    private final Map<String, Object> attributes;

    public MetadataWrapper(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Object find(String... keys) {
        Queue<String> keysQueue = new LinkedList<>(Arrays.asList(keys));
        return search(this.attributes, keysQueue);
    }

    private Object search(Object item, Queue<String> keys) {
        if(keys.isEmpty())
            return item;
        if (item instanceof Map) {
            String key = keys.poll();
            Object subObject = ((Map)item).get(key);
            return search(subObject, keys);
        }
        else if (item instanceof Collection) {
            for(Object o : (Collection)item) {
                Object res = search(o, keys);
                if (res != null)
                    return res;
            }
        }
        return null;
    }
}
