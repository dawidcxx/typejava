package cc.kilonode.typejava.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TjMap {
  private final Map<Object, Object> values = new LinkedHashMap<>();

  public static TjMap create() {
    return new TjMap();
  }

  public int size() {
    return values.size();
  }

  public Object get(Object key) {
    return values.get(key);
  }

  public void put(Object key, Object value) {
    values.put(key, value);
  }

  public boolean containsKey(Object key) {
    return values.containsKey(key);
  }
}
