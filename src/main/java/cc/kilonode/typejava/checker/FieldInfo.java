package cc.kilonode.typejava.checker;

public record FieldInfo(String name, boolean readonly, Type type) {
  public boolean isMethod() {
    return type instanceof Type.Function;
  }
}
