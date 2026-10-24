package cc.kilonode.typejava.ast;

import java.util.List;

public record Program(List<Statement> statements) {
  public Program {
    statements = List.copyOf(statements);
  }
}
