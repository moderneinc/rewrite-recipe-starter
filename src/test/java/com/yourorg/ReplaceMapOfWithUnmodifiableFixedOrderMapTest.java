package com.yourorg;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class ReplaceMapOfWithUnmodifiableFixedOrderMapTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceMapOfWithUnmodifiableFixedOrderMap());
    }

    @Test
    void replaceMapOf() {
        rewriteRun(
          //language=java
          java(
            """
            import java.util.Map;
            import static com.example.Constants.*;

            public class TestClass {
                void test() {
                    Map<String, Boolean> map = Map.of("POLARIS_ROUTER_KEY", false);
                }
            }
            """,
            """
            import static com.example.Constants.*;
            import com.example.UnmodifiableFixedOrderMap;

            public class TestClass {
                void test() {
                    Map<String, Boolean> map = UnmodifiableFixedOrderMap.<String, Boolean>builder()
                        .put("POLARIS_ROUTER_KEY", false)
                        .build();
                }
            }
            """
          )
        );
    }
}
