package cc.kilonode.typejava.ast;

public record Identifier(String name) implements Expression, LValue {}
