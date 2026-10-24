package cc.kilonode.typejava.ast;

public sealed interface TypeReference
    permits FunctionType, NamedType, NullType, ObjectType, UnionType {}
