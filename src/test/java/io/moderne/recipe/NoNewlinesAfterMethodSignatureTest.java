package io.moderne.recipe;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class NoNewlinesAfterMethodSignatureTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NoNewlinesAfterMethodSignature());
    }

    @Test
    void removesDoubleNewlinesAfterMethodSignature() {
        //language=java
        rewriteRun(
            java("""
                    public class A {
                                    
                        public static void foo() {
                        
                            System.out.println("foo");
                        }
                                    
                    }
                    """,
                """
                    public class A {
                                    
                        public static void foo() {
                            System.out.println("foo");
                        }
                                    
                    }
                    """)
        );
    }

    @Test
    void removesMoreThanDoubleNewlinesAfterMethodSignature() {
        //language=java
        rewriteRun(
            java("""
                    public class A {
                                    
                        public static void foo() {
                        
                        
                            System.out.println("foo");
                        }
                                    
                    }
                    """,
                """
                    public class A {
                                    
                        public static void foo() {
                            System.out.println("foo");
                        }
                                    
                    }
                    """)
        );
    }

    @Test
    void doesNothingIfNoNewlines() {
        //language=java
        rewriteRun(
            java("""
                    public class A {
                                    
                        public static void foo() {
                            System.out.println("foo");
                        }
                                    
                    }
                    """)
        );
    }
}
