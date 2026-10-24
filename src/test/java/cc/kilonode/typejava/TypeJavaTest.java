package cc.kilonode.typejava;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TypeJavaTest {
  @Test
  void exposesArtifactName() {
    assertEquals("typejava", TypeJava.artifactName());
  }
}
