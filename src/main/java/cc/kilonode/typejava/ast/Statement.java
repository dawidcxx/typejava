package cc.kilonode.typejava.ast;

public sealed interface Statement
    permits BlockStatement, ExpressionStatement, FunctionDeclaration, ReturnStatement {}
