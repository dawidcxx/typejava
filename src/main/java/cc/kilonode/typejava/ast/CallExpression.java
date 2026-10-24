package cc.kilonode.typejava.ast;

import java.util.List;

public record CallExpression(Expression callee, List<Expression> arguments) implements Expression {
  public CallExpression {
    arguments = List.copyOf(arguments);
  }
}
