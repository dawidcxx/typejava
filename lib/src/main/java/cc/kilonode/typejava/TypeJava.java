package cc.kilonode.typejava;

import cc.kilonode.typejava.checker.CheckException;
import cc.kilonode.typejava.checker.CheckedProgram;
import cc.kilonode.typejava.checker.Checker;
import cc.kilonode.typejava.codegen.CodeGenerator;
import cc.kilonode.typejava.lexer.Lexer;
import cc.kilonode.typejava.lexer.LexerException;
import cc.kilonode.typejava.parser.ParseException;
import cc.kilonode.typejava.parser.ParseResult;
import cc.kilonode.typejava.parser.Parser;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
      Constructor<?> constructor = programClass.getConstructor();
      Method run = programClass.getMethod("run", Environment.class);
      Map<String, ExportedFunction> exports = new LinkedHashMap<>();
      for (Method method : programClass.getDeclaredMethods()) {
        if (!method.getName().startsWith("export$")) {
          continue;
        }
        String name = method.getName().substring("export$".length());
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0
            || !parameterTypes[0].equals(Environment.class)) {
          throw new IllegalStateException(
              "Generated export '" + name + "' does not take an Environment");
        }
        List<Class<?>> hostParameters = new ArrayList<>();
        for (int i = 1; i < parameterTypes.length; i++) {
          hostParameters.add(parameterTypes[i]);
        }
        ExportSignature signature =
            new ExportSignature(name, hostParameters, method.getReturnType());
        exports.put(name, new ExportedFunction(name, method, signature));
      }
      return new CompiledProgram(constructor, run, environment, exports);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException("Could not load generated TypeJava program", exception);
    }
  }

  /**
   * Checks TypeJava source without running it.
   *
   * <p>Runs the full compile pipeline and returns an empty list when the program compiles. The
   * compiler is fail-fast, so the list contains at most one diagnostic.
   *
   * @param source TypeJava source
   * @param environment environment used to check the program
   * @return diagnostics, empty when the source compiles
   */
  public static List<Diagnostic> check(String source, Environment environment) {
    try {
      compile(source, environment);
      return List.of();
    } catch (LexerException exception) {
      return List.of(
          new Diagnostic(exception.getMessage(), exception.getLine(), exception.getColumn()));
    } catch (ParseException exception) {
      return List.of(
          new Diagnostic(exception.getMessage(), exception.getLine(), exception.getColumn()));
    } catch (CheckException exception) {
      return List.of(
          new Diagnostic(exception.getMessage(), exception.getLine(), exception.getColumn()));
    }
  }

  /**
   * Reads and checks a TypeJava source file without running it.
   *
   * @param path source file
   * @param environment environment used to check the program
   * @return diagnostics, empty when the source compiles
   * @throws IOException if the file cannot be read
   */
  public static List<Diagnostic> checkFile(Path path, Environment environment) throws IOException {
    return check(Files.readString(path), environment);
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
