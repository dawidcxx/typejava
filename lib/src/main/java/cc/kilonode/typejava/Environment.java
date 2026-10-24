package cc.kilonode.typejava;

import cc.kilonode.typejava.runtime.TjArray;
import cc.kilonode.typejava.runtime.TjMap;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class Environment {
  private static final Set<String> RESERVED_NAMES =
      Set.of("number", "string", "boolean", "void", "null", "console");

  private final Io io;
  private final Map<String, NativeType> types;

  private Environment(Io io, Map<String, NativeType> types) {
    this.io = io;
    this.types = types;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static Environment standard() {
    return builder().withStd().build();
  }

  public Io io() {
    return io;
  }

  public Optional<NativeType> findType(String name) {
    return Optional.ofNullable(types.get(name));
  }

  public Collection<NativeType> types() {
    return types.values();
  }

  public static final class Builder {
    private final Map<String, NativeType> types = new LinkedHashMap<>();
    private Io io;

    public Builder io(Io io) {
      this.io = Objects.requireNonNull(io, "io");
      return this;
    }

    public Builder withStd() {
      return withArray().withMap();
    }

    public Builder withArray() {
      return bind(standardArray());
    }

    public Builder withMap() {
      return bind(standardMap());
    }

    public Builder bind(NativeType type) {
      Objects.requireNonNull(type, "type");
      type.resolve();
      if (RESERVED_NAMES.contains(type.name())) {
        throw new IllegalArgumentException(
            "Cannot bind native type '" + type.name() + "': name is reserved");
      }
      NativeType existing = types.putIfAbsent(type.name(), type);
      if (existing != null) {
        throw new IllegalArgumentException(
            "Native type '" + type.name() + "' is already bound");
      }
      return this;
    }

    public Environment build() {
      return new Environment(io == null ? new DefaultIo() : io, Map.copyOf(types));
    }
  }

  private static NativeType standardArray() {
    return NativeType.of("TjArray", TjArray.class)
        .typeParameters("T")
        .factory("create", "()Lcc/kilonode/typejava/runtime/TjArray;", "TjArray<T>")
        .instanceMethod("size", "()I", "number")
        .instanceMethod("get", "(I)Ljava/lang/Object;", "T", "number")
        .instanceMethod("add", "(Ljava/lang/Object;)V", "void", "T");
  }

  private static NativeType standardMap() {
    return NativeType.of("TjMap", TjMap.class)
        .typeParameters("K", "V")
        .factory("create", "()Lcc/kilonode/typejava/runtime/TjMap;", "TjMap<K, V>")
        .instanceMethod("size", "()I", "number")
        .instanceMethod("get", "(Ljava/lang/Object;)Ljava/lang/Object;", "V", "K")
        .instanceMethod("put", "(Ljava/lang/Object;Ljava/lang/Object;)V", "void", "K", "V")
        .instanceMethod("containsKey", "(Ljava/lang/Object;)Z", "boolean", "K");
  }
}
