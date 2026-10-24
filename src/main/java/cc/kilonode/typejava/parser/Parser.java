package cc.kilonode.typejava.parser;

import cc.kilonode.typejava.ast.CallExpression;
import cc.kilonode.typejava.ast.Expression;
import cc.kilonode.typejava.ast.ExpressionStatement;
import cc.kilonode.typejava.ast.Identifier;
import cc.kilonode.typejava.ast.MemberExpression;
import cc.kilonode.typejava.ast.Program;
import cc.kilonode.typejava.ast.Statement;
import cc.kilonode.typejava.ast.StringLiteral;
import cc.kilonode.typejava.lexer.Token;
import cc.kilonode.typejava.lexer.TokenType;
import java.util.ArrayList;
import java.util.List;

public final class Parser {
  private final List<Token> tokens;
  private int current;

  public Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  public Program parse() {
    List<Statement> statements = new ArrayList<>();

    while (!check(TokenType.EOF)) {
      statements.add(statement());
    }

    return new Program(statements);
  }

  private Statement statement() {
    Expression expression = expression();
    consume(TokenType.SEMICOLON, "Expected ';' after expression (maybe you forgot a semicolon)");
    return new ExpressionStatement(expression);
  }

  private Expression expression() {
    Expression expression = primary();

    while (true) {
      if (match(TokenType.DOT)) {
        Token property = consume(TokenType.IDENTIFIER, "Expected a property name after '.'");
        expression = new MemberExpression(expression, property.lexeme());
      } else if (match(TokenType.LPAREN)) {
        expression = new CallExpression(expression, arguments());
      } else {
        break;
      }
    }

    return expression;
  }

  private List<Expression> arguments() {
    List<Expression> arguments = new ArrayList<>();

    if (!check(TokenType.RPAREN)) {
      do {
        arguments.add(expression());
      } while (match(TokenType.COMMA));
    }

    consume(TokenType.RPAREN, "Expected ')' after arguments");
    return arguments;
  }

  private Expression primary() {
    if (match(TokenType.IDENTIFIER)) {
      return new Identifier(previous().lexeme());
    }
    if (match(TokenType.STRING)) {
      return new StringLiteral(previous().value());
    }

    throw error(peek(), "Expected expression");
  }

  private boolean match(TokenType type) {
    if (check(type)) {
      advance();
      return true;
    }
    return false;
  }

  private Token consume(TokenType type, String message) {
    if (check(type)) {
      return advance();
    }
    throw error(peek(), message);
  }

  private boolean check(TokenType type) {
    return peek().type() == type;
  }

  private Token advance() {
    if (!isAtEnd()) {
      current++;
    }
    return previous();
  }

  private boolean isAtEnd() {
    return peek().type() == TokenType.EOF;
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token previous() {
    return tokens.get(current - 1);
  }

  private ParseException error(Token token, String message) {
    return new ParseException(message, token.line(), token.column());
  }
}
