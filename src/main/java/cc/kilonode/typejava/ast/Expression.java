package cc.kilonode.typejava.ast;

public sealed interface Expression
    permits AssignmentExpression,
        BinaryExpression,
        BooleanLiteral,
        CallExpression,
        FunctionExpression,
        Identifier,
        MemberExpression,
        NullLiteral,
        NumericLiteral,
        ObjectLiteral,
        StringLiteral,
        UnaryExpression {}
