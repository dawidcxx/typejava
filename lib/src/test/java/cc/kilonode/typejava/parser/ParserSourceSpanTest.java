package cc.kilonode.typejava.parser;

import static cc.kilonode.typejava.parser.ParserTestSupport.parseWithSourceMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import cc.kilonode.typejava.ast.BinaryExpression;
import cc.kilonode.typejava.ast.BlockStatement;
import cc.kilonode.typejava.ast.FunctionDeclaration;
import cc.kilonode.typejava.ast.ReturnStatement;
import cc.kilonode.typejava.ast.SourceMap;
import cc.kilonode.typejava.ast.SourceSpan;
import cc.kilonode.typejava.ast.VariableDeclaration;
import org.junit.jupiter.api.Test;

class ParserSourceSpanTest {
  @Test
  void recordsStatementAndExpressionSpans() {
    ParseResult result = parseWithSourceMap("let total: number = 1 + 2;");
    SourceMap sourceMap = result.sourceMap();

    VariableDeclaration declaration =
        (VariableDeclaration) result.program().statements().get(0);
    assertEquals(new SourceSpan(1, 1, 1, 27), sourceMap.spanOf(declaration));
    assertEquals(new SourceSpan(1, 21, 1, 26), sourceMap.spanOf(declaration.initializer()));

    BinaryExpression initializer = (BinaryExpression) declaration.initializer();
    assertEquals(new SourceSpan(1, 21, 1, 22), sourceMap.spanOf(initializer.left()));
    assertEquals(new SourceSpan(1, 25, 1, 26), sourceMap.spanOf(initializer.right()));
  }

  @Test
  void recordsMultilineSpans() {
    ParseResult result =
        parseWithSourceMap("function f(a: number): number {\n  return a * 2;\n};\n");
    SourceMap sourceMap = result.sourceMap();

    FunctionDeclaration function =
        (FunctionDeclaration) result.program().statements().get(0);
    assertEquals(new SourceSpan(1, 1, 3, 3), sourceMap.spanOf(function));

    BlockStatement body = function.body();
    ReturnStatement returnStatement = (ReturnStatement) body.statements().get(0);
    assertEquals(new SourceSpan(2, 3, 2, 16), sourceMap.spanOf(returnStatement));
    assertEquals(new SourceSpan(2, 10, 2, 15), sourceMap.spanOf(returnStatement.value()));
  }
}
