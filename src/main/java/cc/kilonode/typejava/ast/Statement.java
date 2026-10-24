package cc.kilonode.typejava.ast;

public sealed interface Statement
    permits BlockStatement,
        BreakStatement,
        ContinueStatement,
        ExpressionStatement,
        FunctionDeclaration,
        IfStatement,
        ReturnStatement,
        VariableDeclaration,
        WhileStatement {}
