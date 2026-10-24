package cc.kilonode.typejava.lexer;

public record Token(TokenType type, String lexeme, String value, int line, int column) {}
