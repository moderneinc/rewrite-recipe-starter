package com.yourorg;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AddBaseClassTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
          new AddBaseClass("a.b.C"));
    }

    @Test
    void addExtends() {
        rewriteRun(
          //language=java
          java(
            """
              package com.yourorg;
              
              public class MyBean {
              }
              """,
            """
            package com.yourorg;
            
            public class MyBean extends a.b.C {
            }
            """
          )
        );
    }
}