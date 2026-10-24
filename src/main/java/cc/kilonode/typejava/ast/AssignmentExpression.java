package cc.kilonode.typejava.ast;

public record AssignmentExpression(LValue target, Expression value) implements Expression {}
