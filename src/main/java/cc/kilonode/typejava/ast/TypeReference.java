package cc.kilonode.typejava.ast;

public sealed interface TypeReference permits NamedType, NullType, ObjectType, UnionType {}
