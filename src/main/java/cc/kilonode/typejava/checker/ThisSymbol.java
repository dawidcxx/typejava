package cc.kilonode.typejava.checker;

public final class ThisSymbol {
  private final Type type;
  private final Object identity = new Object();

  public ThisSymbol(Type type) {
    this.type = type;
  }

  public Type type() {
    return type;
  }

  public Object identity() {
    return identity;
  }
}
