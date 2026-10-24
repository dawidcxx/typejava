package cc.kilonode.typejava.lexer;

import cc.kilonode.typejava.testing.SnapshotAssertions;
import cc.kilonode.typejava.testing.TestFixtures;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class LexerSnapshotTest {
  private static final Path SNAPSHOT_ROOT = Path.of("src/test/resources/snapshots/lexer");

  @ParameterizedTest(name = "{0}")
  @MethodSource("fixtures")
  void matchesSnapshot(String fixture) {
    String source = TestFixtures.read(fixture);
    String actual = TokenSnapshot.render(new Lexer(source).tokenize());

    SnapshotAssertions.assertMatches(snapshotPath(fixture), actual);
  }

  static Stream<String> fixtures() {
    return TestFixtures.names(".tj").stream();
  }

  private static Path snapshotPath(String fixture) {
    String snapshot = fixture.substring(0, fixture.length() - ".tj".length()) + ".tokens";
    return SNAPSHOT_ROOT.resolve(snapshot);
  }
}
