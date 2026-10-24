package cc.kilonode.typejava.parser;

import cc.kilonode.typejava.ast.Program;
import cc.kilonode.typejava.lexer.Lexer;

final class ParserTestSupport {
  private ParserTestSupport() {}

  static Program parse(String source) {
    return new Parser(new Lexer(source).tokenize()).parse();
  }

  static ParseResult parseWithSourceMap(String source) {
    return new Parser(new Lexer(source).tokenize()).parseWithSourceMap();
  }
}
