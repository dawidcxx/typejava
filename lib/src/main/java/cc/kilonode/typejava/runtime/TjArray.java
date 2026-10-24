package cc.kilonode.typejava.runtime;

import java.util.ArrayList;
import java.util.List;

public final class TjArray {
  private final List<Object> values = new ArrayList<>();

  public static TjArray create() {
    return new TjArray();
  }

  public int size() {
    return values.size();
  }

  public Object get(int index) {
    return values.get(index);
  }

  public void add(Object value) {
    values.add(value);
  }
}
