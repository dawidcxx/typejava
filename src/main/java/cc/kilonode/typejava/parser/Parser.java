package cc.kilonode.typejava.parser;

import cc.kilonode.typejava.ast.AssignmentExpression;
import cc.kilonode.typejava.ast.BinaryExpression;
import cc.kilonode.typejava.ast.BlockStatement;
import cc.kilonode.typejava.ast.BooleanLiteral;
import cc.kilonode.typejava.ast.BreakStatement;
import cc.kilonode.typejava.ast.CallExpression;
import cc.kilonode.typejava.ast.ContinueStatement;
import cc.kilonode.typejava.ast.Expression;
import cc.kilonode.typejava.ast.ExpressionStatement;
import cc.kilonode.typejava.ast.FunctionDeclaration;
import cc.kilonode.typejava.ast.FunctionExpression;
import cc.kilonode.typejava.ast.Identifier;
import cc.kilonode.typejava.ast.IfStatement;
import cc.kilonode.typejava.ast.LValue;
import cc.kilonode.typejava.ast.MemberExpression;
import cc.kilonode.typejava.ast.MethodSignature;
import cc.kilonode.typejava.ast.NamedType;
import cc.kilonode.typejava.ast.NullLiteral;
import cc.kilonode.typejava.ast.NullType;
import cc.kilonode.typejava.ast.NumericLiteral;
import cc.kilonode.typejava.ast.ObjectField;
import cc.kilonode.typejava.ast.ObjectLiteral;
import cc.kilonode.typejava.ast.ObjectType;
import cc.kilonode.typejava.ast.Parameter;
import cc.kilonode.typejava.ast.Program;
import cc.kilonode.typejava.ast.ReturnStatement;
import cc.kilonode.typejava.ast.SourceMap;
import cc.kilonode.typejava.ast.SourceSpan;
import cc.kilonode.typejava.ast.Statement;
import cc.kilonode.typejava.ast.StringLiteral;
import cc.kilonode.typejava.ast.TypeAliasDeclaration;
import cc.kilonode.typejava.ast.TypeField;
import cc.kilonode.typejava.ast.TypeMember;
import cc.kilonode.typejava.ast.TypeReference;
import cc.kilonode.typejava.ast.UnaryExpression;
import cc.kilonode.typejava.ast.UnionType;
import cc.kilonode.typejava.ast.VariableDeclaration;
import cc.kilonode.typejava.ast.VariableKind;
import cc.kilonode.typejava.ast.WhileStatement;
import cc.kilonode.typejava.lexer.Token;
import cc.kilonode.typejava.lexer.TokenType;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

public final class Parser {
  private final List<Token> tokens;
  private final IdentityHashMap<Object, SourceSpan> spans = new IdentityHashMap<>();
  private int current;

  public Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  public Program parse() {
    current = 0;
    spans.clear();

    List<TypeAliasDeclaration> typeDeclarations = new ArrayList<>();
    List<Statement> statements = new ArrayList<>();

    while (!check(TokenType.EOF)) {
      if (check(TokenType.TYPE)) {
        typeDeclarations.add(typeAliasDeclaration());
      } else {
        statements.add(statement());
      }
    }

    Program program = new Program(typeDeclarations, statements);
    spans.put(program, span(tokens.get(0), tokens.get(tokens.size() - 1)));
    return program;
  }

  public ParseResult parseWithSourceMap() {
    Program program = parse();
    return new ParseResult(program, new SourceMap(spans));
  }

  private TypeAliasDeclaration typeAliasDeclaration() {
    Token start = peek();
    consume(TokenType.TYPE, "Expected 'type'");
    Token name = consume(TokenType.IDENTIFIER, "Expected type name after 'type'");
    List<String> typeParameters = typeParameters();
    consume(TokenType.ASSIGN, "Expected '=' after type name");
    TypeReference type = type();
    consume(
        TokenType.SEMICOLON,
        "Expected ';' after type declaration (maybe you forgot a semicolon)");
    return located(new TypeAliasDeclaration(name.lexeme(), typeParameters, type), start);
  }

  private List<String> typeParameters() {
    if (!match(TokenType.LT)) {
      return List.of();
    }

    List<String> typeParameters = new ArrayList<>();
    do {
      Token parameter = consume(TokenType.IDENTIFIER, "Expected type parameter name");
      typeParameters.add(parameter.lexeme());
    } while (match(TokenType.COMMA));
    consumeTypeClose("Expected '>' after type parameters");

    return typeParameters;
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
    if (check(TokenType.WHILE)) {
      return whileStatement();
    }
    if (check(TokenType.BREAK)) {
      return breakStatement();
    }
    if (check(TokenType.CONTINUE)) {
      return continueStatement();
    }
    if (check(TokenType.LBRACE)) {
      return block();
    }
    if (check(TokenType.RETURN)) {
      return returnStatement();
    }
    return expressionStatement();
  }

  private FunctionDeclaration functionDeclaration() {
    Token start = peek();
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

    return located(new FunctionDeclaration(name.lexeme(), parameters, returnType, body), start);
  }

  private VariableDeclaration variableDeclaration() {
    Token start = peek();
    Token keyword = advance();
    VariableKind kind = keyword.type() == TokenType.LET ? VariableKind.LET : VariableKind.CONST;
    Token name =
        consume(
            TokenType.IDENTIFIER,
            "Expected variable name after '" + keyword.lexeme() + "'");
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

    return located(new VariableDeclaration(kind, name.lexeme(), type, initializer), start);
  }

  private IfStatement ifStatement() {
    Token start = peek();
    consume(TokenType.IF, "Expected 'if'");
    consume(TokenType.LPAREN, "Expected '(' after 'if'");
    Expression condition = expression();
    consume(TokenType.RPAREN, "Expected ')' after if condition");
    BlockStatement thenBranch = block();
    Statement elseBranch = null;

    if (match(TokenType.ELSE)) {
      elseBranch = check(TokenType.IF) ? ifStatement() : block();
    }

    return located(new IfStatement(condition, thenBranch, elseBranch), start);
  }

  private WhileStatement whileStatement() {
    Token start = peek();
    consume(TokenType.WHILE, "Expected 'while'");
    consume(TokenType.LPAREN, "Expected '(' after 'while'");
    Expression condition = expression();
    consume(TokenType.RPAREN, "Expected ')' after while condition");
    BlockStatement body = block();
    return located(new WhileStatement(condition, body), start);
  }

  private BreakStatement breakStatement() {
    Token start = peek();
    consume(TokenType.BREAK, "Expected 'break'");
    consume(TokenType.SEMICOLON, "Expected ';' after break");
    return located(new BreakStatement(), start);
  }

  private ContinueStatement continueStatement() {
    Token start = peek();
    consume(TokenType.CONTINUE, "Expected 'continue'");
    consume(TokenType.SEMICOLON, "Expected ';' after continue");
    return located(new ContinueStatement(), start);
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
    Token start = peek();
    Token name = consume(TokenType.IDENTIFIER, "Expected parameter name");
    consume(
        TokenType.COLON,
        "Expected ':' and a type annotation for parameter '" + name.lexeme() + "'");
    return located(new Parameter(name.lexeme(), type()), start);
  }

  private TypeReference type() {
    Token start = peek();
    TypeReference first = typeAtom();

    if (!match(TokenType.PIPE)) {
      return first;
    }

    Token secondStart = peek();
    TypeReference second = typeAtom();
    if (!(first instanceof NullType) && !(second instanceof NullType)) {
      throw error(secondStart, "Only nullable unions are allowed; use 'T | null'");
    }
    if (check(TokenType.PIPE)) {
      throw error(peek(), "A union may contain only one 'null' member");
    }

    return located(new UnionType(List.of(first, second)), start);
  }

  private TypeReference typeAtom() {
    if (check(TokenType.IDENTIFIER)) {
      Token start = advance();
      return located(new NamedType(start.lexeme(), typeArguments()), start);
    }
    if (check(TokenType.NULL)) {
      Token start = advance();
      return located(new NullType(), start);
    }
    if (check(TokenType.LBRACE)) {
      Token start = advance();
      return located(objectType(), start);
    }
    throw error(peek(), "Expected a type name, object type, or null");
  }

  private List<TypeReference> typeArguments() {
    if (!match(TokenType.LT)) {
      return List.of();
    }

    if (check(TokenType.GT)) {
      throw error(peek(), "Expected a type argument");
    }

    List<TypeReference> arguments = new ArrayList<>();
    do {
      arguments.add(type());
    } while (match(TokenType.COMMA));
    consumeTypeClose("Expected '>' after type arguments");

    return arguments;
  }

  private ObjectType objectType() {
    List<TypeMember> members = new ArrayList<>();

    while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
      Token start = peek();
      boolean readonly = match(TokenType.READONLY);
      Token name = consume(TokenType.IDENTIFIER, "Expected field name or method name");

      if (check(TokenType.LPAREN)) {
        if (readonly) {
          throw error(name, "Methods cannot be readonly");
        }
        consume(TokenType.LPAREN, "Expected '(' after method name");
        List<Parameter> parameters = parameters();
        consume(TokenType.RPAREN, "Expected ')' after method parameters");
        consume(TokenType.COLON, "Expected ':' and a return type after method parameters");
        TypeReference returnType = type();
        consume(TokenType.SEMICOLON, "Expected ';' after method signature");
        members.add(
            located(new MethodSignature(name.lexeme(), parameters, returnType), start));
      } else {
        consume(
            TokenType.COLON,
            "Expected ':' and a type for field '" + name.lexeme() + "'");
        TypeReference type = type();
        consume(
            TokenType.SEMICOLON,
            "Expected ';' after field '" + name.lexeme() + "'");
        members.add(located(new TypeField(name.lexeme(), readonly, type), start));
      }
    }

    consume(TokenType.RBRACE, "Expected '}' after object type");
    return new ObjectType(members);
  }

  private BlockStatement block() {
    Token start = peek();
    consume(TokenType.LBRACE, "Expected '{' to start a block");
    List<Statement> statements = new ArrayList<>();

    while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
      statements.add(statement());
    }

    consume(TokenType.RBRACE, "Expected '}' after block");
    return located(new BlockStatement(statements), start);
  }

  private ReturnStatement returnStatement() {
    Token start = peek();
    consume(TokenType.RETURN, "Expected 'return'");
    boolean bare =
        check(TokenType.SEMICOLON) || check(TokenType.RBRACE) || check(TokenType.EOF);
    Expression value = bare ? null : expression();
    consume(TokenType.SEMICOLON, "Expected ';' after return (maybe you forgot a semicolon)");
    return located(new ReturnStatement(value), start);
  }

  private ExpressionStatement expressionStatement() {
    Token start = peek();
    Expression expression = expression();
    consume(TokenType.SEMICOLON, "Expected ';' after expression (maybe you forgot a semicolon)");
    return located(new ExpressionStatement(expression), start);
  }

  private Expression expression() {
    return assignment();
  }

  private Expression assignment() {
    Token start = peek();
    Expression expression = logicalOr();

    if (check(TokenType.ASSIGN)) {
      Token operator = advance();
      if (!(expression instanceof LValue target)) {
        throw error(operator, "Invalid assignment target");
      }
      Expression value = assignment();
      return located(new AssignmentExpression(target, value), start);
    }

    return expression;
  }

  private Expression logicalOr() {
    Token start = peek();
    Expression expression = logicalAnd();

    while (match(TokenType.OROR)) {
      String operator = previous().lexeme();
      expression = located(new BinaryExpression(expression, operator, logicalAnd()), start);
    }

    return expression;
  }

  private Expression logicalAnd() {
    Token start = peek();
    Expression expression = equality();

    while (match(TokenType.ANDAND)) {
      String operator = previous().lexeme();
      expression = located(new BinaryExpression(expression, operator, equality()), start);
    }

    return expression;
  }

  private Expression equality() {
    Token start = peek();
    Expression expression = comparison();

    while (match(TokenType.EQEQ) || match(TokenType.BANGEQ)) {
      String operator = previous().lexeme();
      expression = located(new BinaryExpression(expression, operator, comparison()), start);
    }

    return expression;
  }

  private Expression comparison() {
    Token start = peek();
    Expression expression = additive();

    while (match(TokenType.LT)
        || match(TokenType.LTE)
        || match(TokenType.GT)
        || match(TokenType.GTE)) {
      String operator = previous().lexeme();
      expression = located(new BinaryExpression(expression, operator, additive()), start);
    }

    return expression;
  }

  private Expression additive() {
    Token start = peek();
    Expression expression = multiplicative();

    while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
      Token operator = advance();
      rejectConsecutiveMinus(operator);
      expression =
          located(
              new BinaryExpression(expression, operator.lexeme(), multiplicative()), start);
    }

    return expression;
  }

  private Expression multiplicative() {
    Token start = peek();
    Expression expression = unary();

    while (match(TokenType.STAR) || match(TokenType.SLASH) || match(TokenType.PERCENT)) {
      String operator = previous().lexeme();
      expression = located(new BinaryExpression(expression, operator, unary()), start);
    }

    return expression;
  }

  private Expression unary() {
    Token start = peek();

    if (check(TokenType.BANG) || check(TokenType.MINUS)) {
      Token operator = advance();
      rejectConsecutiveMinus(operator);
      return located(new UnaryExpression(operator.lexeme(), unary()), start);
    }

    return postfix();
  }

  private void rejectConsecutiveMinus(Token operator) {
    if (operator.type() == TokenType.MINUS && check(TokenType.MINUS)) {
      throw error(
          peek(),
          "Consecutive '-' is not allowed; use parentheses, e.g. (-a)");
    }
  }

  private Expression postfix() {
    Token start = peek();
    Expression expression = primary();

    while (true) {
      if (match(TokenType.DOT)) {
        Token property = consume(TokenType.IDENTIFIER, "Expected a property name after '.'");
        expression = located(new MemberExpression(expression, property.lexeme()), start);
      } else if (match(TokenType.LPAREN)) {
        expression = located(new CallExpression(expression, arguments()), start);
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
    if (check(TokenType.IDENTIFIER)) {
      Token token = advance();
      return located(new Identifier(token.lexeme()), token);
    }
    if (check(TokenType.NUMBER)) {
      Token token = advance();
      return located(new NumericLiteral(token.lexeme()), token);
    }
    if (check(TokenType.STRING)) {
      Token token = advance();
      return located(new StringLiteral(token.value()), token);
    }
    if (check(TokenType.TRUE)) {
      Token token = advance();
      return located(new BooleanLiteral(true), token);
    }
    if (check(TokenType.FALSE)) {
      Token token = advance();
      return located(new BooleanLiteral(false), token);
    }
    if (check(TokenType.NULL)) {
      Token token = advance();
      return located(new NullLiteral(), token);
    }
    if (check(TokenType.LPAREN)) {
      Token start = advance();
      Expression expression = expression();
      consume(TokenType.RPAREN, "Expected ')' after expression");
      spans.put(expression, span(start, previous()));
      return expression;
    }
    if (check(TokenType.LBRACE)) {
      Token start = advance();
      return located(objectLiteral(), start);
    }

    throw error(peek(), "Expected expression");
  }

  private ObjectLiteral objectLiteral() {
    List<ObjectField> fields = new ArrayList<>();

    while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
      Token name = consume(TokenType.IDENTIFIER, "Expected field name in object literal");

      if (check(TokenType.LPAREN)) {
        FunctionExpression method = objectMethod(name);
        fields.add(located(new ObjectField(name.lexeme(), method), name));
      } else {
        consume(
            TokenType.COLON,
            "Expected ':' after object field name '" + name.lexeme() + "'");
        fields.add(located(new ObjectField(name.lexeme(), expression()), name));
      }

      if (!match(TokenType.COMMA)) {
        break;
      }
    }

    consume(TokenType.RBRACE, "Expected '}' after object literal");
    return new ObjectLiteral(fields);
  }

  private FunctionExpression objectMethod(Token start) {
    consume(TokenType.LPAREN, "Expected '(' after method name");
    List<Parameter> parameters = parameters();
    consume(TokenType.RPAREN, "Expected ')' after method parameters");
    consume(TokenType.COLON, "Expected ':' and a return type after method parameters");
    TypeReference returnType = type();
    BlockStatement body = block();
    return located(new FunctionExpression(parameters, returnType, body), start);
  }

  private <T> T located(T node, Token start) {
    return located(node, start, previous());
  }

  private <T> T located(T node, Token start, Token end) {
    spans.put(node, span(start, end));
    return node;
  }

  private SourceSpan span(Token start, Token end) {
    return new SourceSpan(
        start.line(),
        start.column(),
        end.line(),
        end.column() + end.lexeme().length());
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

  private Token consumeTypeClose(String message) {
    if (check(TokenType.GT)) {
      return advance();
    }
    if (check(TokenType.GTE)) {
      Token combined = advance();
      Token close = new Token(TokenType.GT, ">", null, combined.line(), combined.column());
      Token assign =
          new Token(TokenType.ASSIGN, "=", null, combined.line(), combined.column() + 1);
      tokens.set(current - 1, close);
      tokens.add(current, assign);
      return close;
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
