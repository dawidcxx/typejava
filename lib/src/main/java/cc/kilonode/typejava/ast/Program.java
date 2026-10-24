package cc.kilonode.typejava.ast;

import java.util.List;

public record Program(List<TypeAliasDeclaration> typeDeclarations, List<Statement> statements) {
  public Program {
    typeDeclarations = List.copyOf(typeDeclarations);
    statements = List.copyOf(statements);
  }
}
