package cc.kilonode.typejava.ast;

public sealed interface Expression
    permits AssignmentExpression,
        BinaryExpression,
        BooleanLiteral,
        CallExpression,
        Identifier,
        MemberExpression,
        NumericLiteral,
        StringLiteral,
        UnaryExpression {}
