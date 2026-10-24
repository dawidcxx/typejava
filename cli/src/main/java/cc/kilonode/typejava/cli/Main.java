package cc.kilonode.typejava.cli;

import cc.kilonode.typejava.TypeJava;
import cc.kilonode.typejava.checker.CheckException;
import cc.kilonode.typejava.lexer.LexerException;
import cc.kilonode.typejava.parser.ParseException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

public final class Main {
  private Main() {}

  public static void main(String[] args) {
    System.exit(execute(args, System.out, System.err));
  }

  public static int execute(String[] args, PrintStream out, PrintStream err) {
    if (args.length != 1) {
      err.println("Usage: typejava <script.tj>");
      return 2;
    }

    try {
      TypeJava.runFile(Path.of(args[0]), out);
      return 0;
    } catch (ParseException | CheckException | LexerException | IOException exception) {
      err.println("error: " + exception.getMessage());
      return 1;
    }
  }
}
