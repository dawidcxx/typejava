package cc.kilonode.typejava.ast;

public record WhileStatement(Expression condition, BlockStatement body) implements Statement {}
