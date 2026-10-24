package cc.kilonode.typejava.ast;

public sealed interface Expression
    permits BinaryExpression, CallExpression, Identifier, MemberExpression, NumericLiteral, StringLiteral {}
