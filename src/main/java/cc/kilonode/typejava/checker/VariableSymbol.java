package cc.kilonode.typejava.checker;

public final class VariableSymbol {
  private final String name;
  private final Type type;
  private final boolean mutable;
  private final boolean topLevelField;

  public VariableSymbol(String name, Type type, boolean mutable, boolean topLevelField) {
    this.name = name;
    this.type = type;
    this.mutable = mutable;
    this.topLevelField = topLevelField;
  }

  public String name() {
    return name;
  }

  public Type type() {
    return type;
  }

  public boolean mutable() {
    return mutable;
  }

  public boolean topLevelField() {
    return topLevelField;
  }
}
