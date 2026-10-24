package cc.kilonode.typejava;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cc.kilonode.typejava.checker.CheckException;
import cc.kilonode.typejava.runtime.TjArray;
import cc.kilonode.typejava.runtime.TjMap;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TypeJavaEnvironmentTest {
  @Test
  void customIoReceivesFormattedConsoleOutput() {
    List<String> lines = new ArrayList<>();
    Environment environment =
        TypeJava.environment()
            .io(
                new Io() {
                  @Override
                  public void println(String line) {
                    lines.add(line);
                  }
                })
            .withStd()
            .build();

    TypeJava.run("console.log(1, \"x\", true);", environment);

    assertEquals(List.of("1 x true"), lines);
  }

  @Test
  void stdIsNotAvailableUnlessBound() {
    CheckException exception =
        assertThrows(
            CheckException.class,
            () -> TypeJava.run("const xs: TjArray<number> = null;", TypeJava.environment().build()));
    assertTrue(
        exception.getMessage().contains("Unknown type 'TjArray'"),
        exception::getMessage);
  }

  @Test
  void manuallyBoundStdTypesWork() {
    List<String> lines = new ArrayList<>();
    Environment environment =
        TypeJava.environment()
            .io(
                new Io() {
                  @Override
                  public void println(String line) {
                    lines.add(line);
                  }
                })
            .bind(
                NativeType.of("TjArray", TjArray.class)
                    .typeParameters("T")
                    .factory(
                        "create", "()Lcc/kilonode/typejava/runtime/TjArray;", "TjArray<T>")
                    .instanceMethod("size", "()I", "number")
                    .instanceMethod("get", "(I)Ljava/lang/Object;", "T", "number")
                    .instanceMethod("add", "(Ljava/lang/Object;)V", "void", "T"))
            .bind(
                NativeType.of("TjMap", TjMap.class)
                    .typeParameters("K", "V")
                    .factory("create", "()Lcc/kilonode/typejava/runtime/TjMap;", "TjMap<K, V>")
                    .instanceMethod("size", "()I", "number")
                    .instanceMethod("get", "(Ljava/lang/Object;)Ljava/lang/Object;", "V", "K")
                    .instanceMethod(
                        "put", "(Ljava/lang/Object;Ljava/lang/Object;)V", "void", "K", "V")
                    .instanceMethod("containsKey", "(Ljava/lang/Object;)Z", "boolean", "K"))
            .build();

    TypeJava.run(
        """
        const xs: TjArray<number> = TjArray.create();
        xs.add(2);
        xs.add(3);
        const counts: TjMap<string, number> = TjMap.create();
        counts.put("total", xs.get(0) + xs.get(1));
        console.log(counts.containsKey("total"), counts.get("total"));
        """,
        environment);

    assertEquals(List.of("true 5"), lines);
  }

  @Test
  void nativeMethodsCanReceiveTheEnvironment() {
    List<String> lines = new ArrayList<>();
    Environment environment =
        TypeJava.environment()
            .io(
                new Io() {
                  @Override
                  public void println(String line) {
                    lines.add(line);
                  }
                })
            .bind(
                NativeType.of("Host", Host.class)
                    .staticMethod(
                        "print",
                        "(Lcc/kilonode/typejava/Environment;Ljava/lang/String;)V",
                        "void",
                        "string"))
            .build();

    TypeJava.run("Host.print(\"from host\");", environment);

    assertEquals(List.of("from host"), lines);
  }

  public static final class Host {
    public static void print(Environment environment, String value) {
      environment.io().println(value);
    }
  }
}
