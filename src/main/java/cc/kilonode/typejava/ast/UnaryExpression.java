package cc.kilonode.typejava.ast;

public record UnaryExpression(String operator, Expression operand) implements Expression {}
