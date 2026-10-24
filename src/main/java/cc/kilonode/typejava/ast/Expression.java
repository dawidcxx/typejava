package cc.kilonode.typejava.ast;

public sealed interface Expression permits Identifier, StringLiteral, MemberExpression, CallExpression {}
