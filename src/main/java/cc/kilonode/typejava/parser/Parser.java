package cc.kilonode.typejava.parser;

import cc.kilonode.typejava.ast.AssignmentExpression;
import cc.kilonode.typejava.ast.BinaryExpression;
import cc.kilonode.typejava.ast.BlockStatement;
import cc.kilonode.typejava.ast.BooleanLiteral;
import cc.kilonode.typejava.ast.CallExpression;
import cc.kilonode.typejava.ast.Expression;
import cc.kilonode.typejava.ast.ExpressionStatement;
import cc.kilonode.typejava.ast.FunctionDeclaration;
import cc.kilonode.typejava.ast.Identifier;
import cc.kilonode.typejava.ast.IfStatement;
import cc.kilonode.typejava.ast.MemberExpression;
import cc.kilonode.typejava.ast.NamedType;
import cc.kilonode.typejava.ast.NumericLiteral;
import cc.kilonode.typejava.ast.ObjectType;
import cc.kilonode.typejava.ast.Parameter;
import cc.kilonode.typejava.ast.Program;
import cc.kilonode.typejava.ast.ReturnStatement;
import cc.kilonode.typejava.ast.Statement;
import cc.kilonode.typejava.ast.StringLiteral;
import cc.kilonode.typejava.ast.TypeAliasDeclaration;
import cc.kilonode.typejava.ast.TypeField;
import cc.kilonode.typejava.ast.TypeReference;
import cc.kilonode.typejava.ast.UnaryExpression;
import cc.kilonode.typejava.ast.VariableDeclaration;
import cc.kilonode.typejava.ast.VariableKind;
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
    List<TypeAliasDeclaration> typeDeclarations = new ArrayList<>();
    List<Statement> statements = new ArrayList<>();

    while (!check(TokenType.EOF)) {
      if (check(TokenType.TYPE)) {
        typeDeclarations.add(typeAliasDeclaration());
      } else {
        statements.add(statement());
      }
    }

    return new Program(typeDeclarations, statements);
  }

  private TypeAliasDeclaration typeAliasDeclaration() {
    consume(TokenType.TYPE, "Expected 'type'");
    Token name = consume(TokenType.IDENTIFIER, "Expected type name after 'type'");
    consume(TokenType.ASSIGN, "Expected '=' after type name");
    TypeReference type = type();
    consume(
        TokenType.SEMICOLON,
        "Expected ';' after type declaration (maybe you forgot a semicolon)");
    return new TypeAliasDeclaration(name.lexeme(), type);
  }

  private Statement statement() {
    if (check(TokenType.FUNCTION)) {
      return functionDeclaration();
    }
    if (check(TokenType.LET) || check(TokenType.CONST)) {
      return variableDeclaration();
    }
    if (check(TokenType.IF)) {
      return ifStatement();
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

  private VariableDeclaration variableDeclaration() {
    Token keyword = advance();
    VariableKind kind = keyword.type() == TokenType.LET ? VariableKind.LET : VariableKind.CONST;
    Token name = consume(TokenType.IDENTIFIER, "Expected variable name after '" + keyword.lexeme() + "'");
    consume(
        TokenType.COLON,
        "Expected ':' and a type annotation for variable '" + name.lexeme() + "'");
    TypeReference type = type();
    consume(
        TokenType.ASSIGN,
        "Expected '=' and an initializer for variable '" + name.lexeme() + "'");
    Expression initializer = expression();
    consume(
        TokenType.SEMICOLON,
        "Expected ';' after variable declaration (maybe you forgot a semicolon)");

    return new VariableDeclaration(kind, name.lexeme(), type, initializer);
  }

  private IfStatement ifStatement() {
    consume(TokenType.IF, "Expected 'if'");
    consume(TokenType.LPAREN, "Expected '(' after 'if'");
    Expression condition = expression();
    consume(TokenType.RPAREN, "Expected ')' after if condition");
    BlockStatement thenBranch = block();
    Statement elseBranch = null;

    if (match(TokenType.ELSE)) {
      elseBranch = check(TokenType.IF) ? ifStatement() : block();
    }

    return new IfStatement(condition, thenBranch, elseBranch);
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
    if (match(TokenType.IDENTIFIER)) {
      return new NamedType(previous().lexeme());
    }
    if (match(TokenType.LBRACE)) {
      return objectType();
    }
    throw error(peek(), "Expected a type name or object type");
  }

  private ObjectType objectType() {
    List<TypeField> fields = new ArrayList<>();

    while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
      Token name = consume(TokenType.IDENTIFIER, "Expected field name");
      consume(
          TokenType.COLON,
          "Expected ':' and a type for field '" + name.lexeme() + "'");
      TypeReference type = type();
      consume(
          TokenType.SEMICOLON,
          "Expected ';' after field '" + name.lexeme() + "'");
      fields.add(new TypeField(name.lexeme(), type));
    }

    consume(TokenType.RBRACE, "Expected '}' after object type");
    return new ObjectType(fields);
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
    return assignment();
  }

  private Expression assignment() {
    if (check(TokenType.IDENTIFIER) && checkNext(TokenType.ASSIGN)) {
      Token name = advance();
      advance();
      Expression value = assignment();
      return new AssignmentExpression(new Identifier(name.lexeme()), value);
    }

    return logicalOr();
  }

  private Expression logicalOr() {
    Expression expression = logicalAnd();

    while (match(TokenType.OROR)) {
      String operator = previous().lexeme();
      expression = new BinaryExpression(expression, operator, logicalAnd());
    }

    return expression;
  }

  private Expression logicalAnd() {
    Expression expression = equality();

    while (match(TokenType.ANDAND)) {
      String operator = previous().lexeme();
      expression = new BinaryExpression(expression, operator, equality());
    }

    return expression;
  }

  private Expression equality() {
    Expression expression = comparison();

    while (match(TokenType.EQEQ) || match(TokenType.BANGEQ)) {
      String operator = previous().lexeme();
      expression = new BinaryExpression(expression, operator, comparison());
    }

    return expression;
  }

  private Expression comparison() {
    Expression expression = additive();

    while (match(TokenType.LT)
        || match(TokenType.LTE)
        || match(TokenType.GT)
        || match(TokenType.GTE)) {
      String operator = previous().lexeme();
      expression = new BinaryExpression(expression, operator, additive());
    }

    return expression;
  }

  private Expression additive() {
    Expression expression = unary();

    while (match(TokenType.PLUS)) {
      String operator = previous().lexeme();
      expression = new BinaryExpression(expression, operator, unary());
    }

    return expression;
  }

  private Expression unary() {
    if (match(TokenType.BANG)) {
      String operator = previous().lexeme();
      return new UnaryExpression(operator, unary());
    }

    return postfix();
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
    if (match(TokenType.TRUE)) {
      return new BooleanLiteral(true);
    }
    if (match(TokenType.FALSE)) {
      return new BooleanLiteral(false);
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

  private boolean checkNext(TokenType type) {
    return current + 1 < tokens.size() && tokens.get(current + 1).type() == type;
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
