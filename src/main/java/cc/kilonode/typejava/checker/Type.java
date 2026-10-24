package cc.kilonode.typejava.checker;

import java.util.List;

public sealed interface Type {
  enum PrimitiveKind {
    NUMBER,
    STRING,
    BOOLEAN,
    VOID
  }

  Primitive NUMBER = new Primitive(PrimitiveKind.NUMBER);
  Primitive STRING = new Primitive(PrimitiveKind.STRING);
  Primitive BOOLEAN = new Primitive(PrimitiveKind.BOOLEAN);
  Primitive VOID = new Primitive(PrimitiveKind.VOID);
  Null NULL = new Null();

  record Primitive(PrimitiveKind kind) implements Type {}

  record Null() implements Type {}

  record Nominal(String name, List<Type> typeArguments) implements Type {
    public Nominal {
      typeArguments = List.copyOf(typeArguments);
    }

    public Nominal(String name) {
      this(name, List.of());
    }
  }

  record Function(Type receiver, List<Type> parameters, Type returnType) implements Type {
    public Function {
      parameters = List.copyOf(parameters);
    }
  }

  record Nullable(Type inner) implements Type {}

  record Variable(String name) implements Type {}

  static Nominal nominal(String name, Type... arguments) {
    return new Nominal(name, List.of(arguments));
  }

  static Nullable nullable(Type inner) {
    return new Nullable(inner);
  }
}
