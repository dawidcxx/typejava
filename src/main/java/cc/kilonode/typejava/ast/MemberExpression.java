package cc.kilonode.typejava.ast;

public record MemberExpression(Expression object, String property) implements Expression, LValue {}
