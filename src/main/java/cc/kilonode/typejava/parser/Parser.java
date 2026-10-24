package cc.kilonode.typejava.parser;

import cc.kilonode.typejava.ast.BinaryExpression;
import cc.kilonode.typejava.ast.BlockStatement;
import cc.kilonode.typejava.ast.CallExpression;
import cc.kilonode.typejava.ast.Expression;
import cc.kilonode.typejava.ast.ExpressionStatement;
import cc.kilonode.typejava.ast.FunctionDeclaration;
import cc.kilonode.typejava.ast.Identifier;
import cc.kilonode.typejava.ast.MemberExpression;
import cc.kilonode.typejava.ast.NumericLiteral;
import cc.kilonode.typejava.ast.Parameter;
import cc.kilonode.typejava.ast.Program;
import cc.kilonode.typejava.ast.ReturnStatement;
import cc.kilonode.typejava.ast.Statement;
import cc.kilonode.typejava.ast.StringLiteral;
import cc.kilonode.typejava.ast.TypeReference;
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
    if (check(TokenType.FUNCTION)) {
      return functionDeclaration();
    }
    if (check(TokenType.LBRACE)) {
      return block();
    }
    if (match(TokenType.RETURN)) {
      return returnStatement();
    }
    return expressionStatement();
  }

  private FunctionDeclaration functionDeclaration() {
    consume(TokenType.FUNCTION, "Expected 'function'");
    Token name = consume(TokenType.IDENTIFIER, "Expected function name after 'function'");
    consume(TokenType.LPAREN, "Expected '(' after function name");
    List<Parameter> parameters = parameters();
    consume(TokenType.RPAREN, "Expected ')' after parameters");
    consume(TokenType.COLON, "Expected ':' and a return type after parameter list");
    TypeReference returnType = type();
    BlockStatement body = block();
    consume(
        TokenType.SEMICOLON,
        "Expected ';' after function declaration (maybe you forgot a semicolon)");

    return new FunctionDeclaration(name.lexeme(), parameters, returnType, body);
  }

  private List<Parameter> parameters() {
    List<Parameter> parameters = new ArrayList<>();

    if (!check(TokenType.RPAREN)) {
      do {
        parameters.add(parameter());
      } while (match(TokenType.COMMA));
    }

    return parameters;
  }

  private Parameter parameter() {
    Token name = consume(TokenType.IDENTIFIER, "Expected parameter name");
    consume(
        TokenType.COLON,
        "Expected ':' and a type annotation for parameter '" + name.lexeme() + "'");
    return new Parameter(name.lexeme(), type());
  }

  private TypeReference type() {
    Token first = consume(TokenType.IDENTIFIER, "Expected a type name");
    StringBuilder name = new StringBuilder(first.lexeme());

    while (match(TokenType.DOT)) {
      Token part = consume(TokenType.IDENTIFIER, "Expected a type name after '.'");
      name.append('.').append(part.lexeme());
    }

    return new TypeReference(name.toString());
  }

  private BlockStatement block() {
    consume(TokenType.LBRACE, "Expected '{' to start a block");
    List<Statement> statements = new ArrayList<>();

    while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
      statements.add(statement());
    }

    consume(TokenType.RBRACE, "Expected '}' after block");
    return new BlockStatement(statements);
  }

  private ReturnStatement returnStatement() {
    Expression value = expression();
    consume(TokenType.SEMICOLON, "Expected ';' after return value (maybe you forgot a semicolon)");
    return new ReturnStatement(value);
  }

  private ExpressionStatement expressionStatement() {
    Expression expression = expression();
    consume(TokenType.SEMICOLON, "Expected ';' after expression (maybe you forgot a semicolon)");
    return new ExpressionStatement(expression);
  }

  private Expression expression() {
    return additive();
  }

  private Expression additive() {
    Expression expression = postfix();

    while (match(TokenType.PLUS)) {
      String operator = previous().lexeme();
      expression = new BinaryExpression(expression, operator, postfix());
    }

    return expression;
  }

  private Expression postfix() {
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
    if (match(TokenType.NUMBER)) {
      return new NumericLiteral(previous().lexeme());
    }
    if (match(TokenType.STRING)) {
      return new StringLiteral(previous().value());
    }
    if (match(TokenType.LPAREN)) {
      Expression expression = expression();
      consume(TokenType.RPAREN, "Expected ')' after expression");
      return expression;
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
