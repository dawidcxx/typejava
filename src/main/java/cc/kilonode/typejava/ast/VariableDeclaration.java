package cc.kilonode.typejava.ast;

public record VariableDeclaration(
    VariableKind kind, String name, TypeReference type, Expression initializer)
    implements Statement {}
