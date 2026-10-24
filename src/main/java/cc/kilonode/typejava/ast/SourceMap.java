package cc.kilonode.typejava.ast;

import java.util.IdentityHashMap;
import java.util.Map;

public final class SourceMap {
  private final IdentityHashMap<Object, SourceSpan> spans;

  public SourceMap(Map<Object, SourceSpan> spans) {
    this.spans = new IdentityHashMap<>(spans);
  }

  public SourceSpan spanOf(Object node) {
    return spans.get(node);
  }
}
