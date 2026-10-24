package cc.kilonode.typejava.ast;

import java.util.List;

public record BlockStatement(List<Statement> statements) implements Statement {
  public BlockStatement {
    statements = List.copyOf(statements);
  }
}
