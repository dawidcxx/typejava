package cc.kilonode.typejava;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cc.kilonode.typejava.checker.CheckException;
import cc.kilonode.typejava.runtime.TjArray;
import org.junit.jupiter.api.Test;

public class TypeJavaExportTest {
  public record Payload(double incrementMe) {}

  public record Labeled(double value, String label) {}

  public record MaybeLabeled(double value, @MaybeNull String label) {}

  public interface LogApi {
    Payload analyzeLog(Payload payload);

    double invocationCount();
  }

  public interface LabelApi {
    Labeled echo(Labeled value);
  }

  public interface MaybeLabelApi {
    MaybeLabeled echo(MaybeLabeled value);
  }

  public interface ArrayApi {
    TjArray numbers();

    double size(TjArray values);
  }

  @Test
  void invokesExportsWithIsolatedInstanceState() {
    Environment environment =
        TypeJava.environment()
            .record(Payload.class)
            .exports(LogApi.class)
            .withStd()
            .build();
    String source =
        """
        let invocations: number = 0;

        export function analyzeLog(payload: Payload): Payload {
          invocations = invocations + 1;
          return { incrementMe: payload.incrementMe + 1 };
        };

        export function invocationCount(): number {
          return invocations;
        };
        """;

    CompiledProgram program = TypeJava.compile(source, environment);
    ProgramInstance first = program.instantiate();
    ProgramInstance second = program.instantiate();
    LogApi firstApi = first.exports(LogApi.class);
    LogApi secondApi = second.exports(LogApi.class);

    assertEquals(1.0, firstApi.analyzeLog(new Payload(0)).incrementMe());
    assertEquals(1.0, firstApi.invocationCount());
    assertEquals(0.0, secondApi.invocationCount());

    Payload dynamic = (Payload) first.invoke("analyzeLog", new Payload(1));
    assertEquals(2.0, dynamic.incrementMe());
    assertEquals(2.0, firstApi.invocationCount());
  }

  @Test
  void acceptsIdenticalSourceRedeclarationOfRecordType() {
    Environment environment =
        TypeJava.environment().record(Payload.class).exports(LogApi.class).build();
    String source =
        """
        type Payload = {
          readonly incrementMe: number;
        };

        export function analyzeLog(payload: Payload): Payload {
          return { incrementMe: payload.incrementMe + 1 };
        };

        export function invocationCount(): number {
          return 0;
        };
        """;

    CompiledProgram program = TypeJava.compile(source, environment);
    ProgramInstance instance = program.instantiate();
    assertEquals(2.0, instance.exports(LogApi.class).analyzeLog(new Payload(1)).incrementMe());
  }

  @Test
  void rejectsDifferentSourceRedeclarationOfRecordType() {
    Environment environment = TypeJava.environment().record(Payload.class).build();
    String source =
        """
        type Payload = {
          readonly incrementMe: string;
        };
        """;

    CheckException exception =
        assertThrows(
            CheckException.class, () -> TypeJava.compile(source, environment));
    assertTrue(exception.getMessage().contains("maps it to number"), exception::getMessage);
  }

  @Test
  void checksNonNullReferenceComponentsAtTheBoundary() {
    Environment environment =
        TypeJava.environment().record(Labeled.class).exports(LabelApi.class).build();
    String source =
        """
        export function echo(value: Labeled): Labeled {
          return value;
        };
        """;

    ProgramInstance instance =
        TypeJava.compile(source, environment).instantiate();
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> instance.exports(LabelApi.class).echo(new Labeled(1, null)));
    assertTrue(exception.getMessage().contains("label"), exception::getMessage);
  }

  @Test
  void allowsNullForMaybeNullComponents() {
    Environment environment =
        TypeJava.environment().record(MaybeLabeled.class).exports(MaybeLabelApi.class).build();
    String source =
        """
        export function echo(value: MaybeLabeled): MaybeLabeled {
          return value;
        };
        """;

    ProgramInstance instance =
        TypeJava.compile(source, environment).instantiate();
    MaybeLabeled result =
        instance.exports(MaybeLabelApi.class).echo(new MaybeLabeled(1, null));
    assertEquals(null, result.label());
  }

  @Test
  void exportsNativeArrayTypes() {
    Environment environment =
        TypeJava.environment().withArray().exports(ArrayApi.class).build();
    String source =
        """
        export function numbers(): TjArray<number> {
          return TjArray.create();
        };

        export function size(values: TjArray<number>): number {
          return values.size();
        };
        """;

    ArrayApi api =
        TypeJava.compile(source, environment).instantiate().exports(ArrayApi.class);
    TjArray values = api.numbers();
    values.add(1);
    values.add(2);
    assertEquals(2.0, api.size(values));
  }

  @Test
  void rejectsExportContractsWithoutMatchingFunctions() {
    Environment environment =
        TypeJava.environment().record(Payload.class).exports(LogApi.class).build();

    CheckException exception =
        assertThrows(
            CheckException.class,
            () -> TypeJava.compile("export function other(): number { return 1; };", environment));
    assertTrue(exception.getMessage().contains("no matching exported function"), exception::getMessage);
  }
}
