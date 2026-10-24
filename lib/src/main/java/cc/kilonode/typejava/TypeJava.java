package cc.kilonode.typejava;

import cc.kilonode.typejava.checker.CheckedProgram;
import cc.kilonode.typejava.checker.Checker;
import cc.kilonode.typejava.codegen.CodeGenerator;
import cc.kilonode.typejava.lexer.Lexer;
import cc.kilonode.typejava.parser.ParseResult;
import cc.kilonode.typejava.parser.Parser;
import cc.kilonode.typejava.runtime.Console;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Entry point for compiling and running TypeJava source.
 *
 * <p>Compilation is fail-fast: the first lexical, syntax, or type error aborts the run.
 */
public final class TypeJava {
  private TypeJava() {}

  /**
   * Returns the artifact name.
   *
   * @return the artifact name
   */
  public static String artifactName() {
    return "typejava";
  }

  /** Runs a TypeJava program, writing console output to {@link System#out}. */
  public static void run(String source) {
    run(source, System.out);
  }

  /**
   * Compiles and runs a TypeJava program.
   *
   * @param source TypeJava source
   * @param out destination for {@code console.log}
   * @param args arguments passed to the generated main method
   */
  public static void run(String source, PrintStream out, String... args) {
    ParseResult parsed = new Parser(new Lexer(source).tokenize()).parseWithSourceMap();
    CheckedProgram checked = new Checker(parsed.program(), parsed.sourceMap()).check();
    CodeGenerator generator = new CodeGenerator(checked);
    Map<String, byte[]> classes = generator.generate();

    GeneratedClassLoader loader =
        new GeneratedClassLoader(TypeJava.class.getClassLoader(), classes);
    PrintStream previous = Console.currentOut();
    Console.setOut(out);
    try {
      Class<?> mainClass = loader.loadClass(generator.mainClassName());
      Method main = mainClass.getMethod("main", String[].class);
      main.invoke(null, (Object) args);
    } catch (InvocationTargetException exception) {
      Throwable cause = exception.getCause();
      if (cause instanceof RuntimeException runtime) {
        throw runtime;
      }
      if (cause instanceof Error error) {
        throw error;
      }
      throw new RuntimeException(cause);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException("Could not start generated TypeJava program", exception);
    } finally {
      Console.setOut(previous);
    }
  }

  /** Reads and runs a TypeJava source file. */
  public static void runFile(Path path, PrintStream out, String... args) throws IOException {
    run(Files.readString(path), out, args);
  }

  private static final class GeneratedClassLoader extends ClassLoader {
    private final Map<String, byte[]> classes;

    private GeneratedClassLoader(ClassLoader parent, Map<String, byte[]> classes) {
      super(parent);
      this.classes = classes;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
      byte[] bytes = classes.get(name.replace('.', '/'));
      if (bytes == null) {
        throw new ClassNotFoundException(name);
      }
      return defineClass(name, bytes, 0, bytes.length);
    }
  }
}
