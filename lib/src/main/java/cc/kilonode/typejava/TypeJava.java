package cc.kilonode.typejava;

import cc.kilonode.typejava.checker.CheckedProgram;
import cc.kilonode.typejava.checker.Checker;
import cc.kilonode.typejava.codegen.CodeGenerator;
import cc.kilonode.typejava.lexer.Lexer;
import cc.kilonode.typejava.parser.ParseResult;
import cc.kilonode.typejava.parser.Parser;
import java.io.IOException;
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

  /** Starts a new environment configuration. */
  public static Environment.Builder environment() {
    return Environment.builder();
  }

  /**
   * Compiles and runs a TypeJava program.
   *
   * @param source TypeJava source
   * @param environment environment used to compile and run the program
   */
  public static void run(String source, Environment environment) {
    compile(source, environment).run();
  }

  /**
   * Compiles TypeJava source into a reusable program.
   *
   * <p>Compilation is fail-fast: the first lexical, syntax, or type error aborts the compile. Use
   * this when the same source is executed more than once.
   *
   * @param source TypeJava source
   * @param environment environment used to compile the program
   * @return the compiled program
   */
  public static CompiledProgram compile(String source, Environment environment) {
    ParseResult parsed = new Parser(new Lexer(source).tokenize()).parseWithSourceMap();
    CheckedProgram checked =
        new Checker(parsed.program(), parsed.sourceMap(), environment).check();
    CodeGenerator generator = new CodeGenerator(checked, environment);
    Map<String, byte[]> classes = generator.generate();

    GeneratedClassLoader loader =
        new GeneratedClassLoader(TypeJava.class.getClassLoader(), classes);
    try {
      Class<?> programClass = loader.loadClass(generator.mainClassName());
      Method run = programClass.getMethod("run", Environment.class);
      return new CompiledProgram(run, environment);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException("Could not load generated TypeJava program", exception);
    }
  }

  /**
   * Reads and runs a TypeJava source file.
   *
   * @param path source file
   * @param environment environment used to compile and run the program
   * @throws IOException if the file cannot be read
   */
  public static void runFile(Path path, Environment environment) throws IOException {
    run(Files.readString(path), environment);
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
