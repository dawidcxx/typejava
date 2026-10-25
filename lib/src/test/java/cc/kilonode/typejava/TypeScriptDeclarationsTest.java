package cc.kilonode.typejava;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cc.kilonode.typejava.runtime.TjArray;
import org.junit.jupiter.api.Test;

public class TypeScriptDeclarationsTest {
  public record Payload(double incrementMe) {}

  public record Label(@MaybeNull String label) {}

  public interface LogApi {
    Payload analyzeLog(Payload payload);

    double invocationCount();
  }

  public interface MaybeApi {
    @MaybeNull
    String echo(@MaybeNull String value);
  }

  public interface ArrayApi {
    TjArray numbers();

    double size(TjArray values);
  }

  @Test
  void generatesNativeRecordAndExportDeclarations() {
    Environment environment =
        TypeJava.environment()
            .withStd()
            .record(Payload.class)
            .record(Label.class)
            .exports(LogApi.class, MaybeApi.class, ArrayApi.class)
            .build();

    assertEquals(
        """
        interface TjArray<T> {
          size(): number;
          get(arg0: number): T;
          add(arg0: T): void;
        }

        declare const TjArray: {
          create<T>(): TjArray<T>;
        };

        interface TjMap<K, V> {
          size(): number;
          get(arg0: K): V;
          put(arg0: K, arg1: V): void;
          containsKey(arg0: K): boolean;
        }

        declare const TjMap: {
          create<K, V>(): TjMap<K, V>;
        };

        interface Label {
          readonly label: string | null;
        }

        interface Payload {
          readonly incrementMe: number;
        }

        interface LogApi {
          analyzeLog(arg0: Payload): Payload;
          invocationCount(): number;
        }

        interface MaybeApi {
          echo(arg0: string | null): string | null;
        }

        interface ArrayApi {
          numbers(): TjArray<unknown>;
          size(arg0: TjArray<unknown>): number;
        }

        """,
        environment.typeScriptDeclarations());
  }

  @Test
  void emptyEnvironmentProducesNoDeclarations() {
    assertEquals("", TypeJava.environment().build().typeScriptDeclarations());
  }
}
