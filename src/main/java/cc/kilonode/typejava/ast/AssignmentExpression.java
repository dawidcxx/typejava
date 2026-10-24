package cc.kilonode.typejava.ast;

public record AssignmentExpression(Identifier target, Expression value) implements Expression {}
