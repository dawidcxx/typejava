package cc.kilonode.typejava.ast;

public record IfStatement(
    Expression condition, BlockStatement thenBranch, Statement elseBranch) implements Statement {}
