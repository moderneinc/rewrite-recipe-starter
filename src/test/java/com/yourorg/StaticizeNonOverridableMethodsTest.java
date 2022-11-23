package com.yourorg;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;

public class StaticizeNonOverridableMethodsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new StaticizeNonOverridableMethods());
    }

    @Test
    void instanceVariableAccessed() {
        rewriteRun(
            java("""
                        class A {
                          private int a;
                        
                          private int func1() {
                            return a;
                          }
                          
                          final int func2() {
                            return a * 2;
                          }
                          
                          private final int func3() {
                            return a * 3;
                          }
                        }
                    """
            )
        );
    }

    @Test
    void instanceVariableNotAccessed() {
        rewriteRun(
            java("""
                        class A {
                          private int a;
                          
                          private int func1(int b) {
                            int c = 2;
                            return c + b;
                          }
                          
                          final int func2(int b) {
                            int c = 2;
                            return c - b;
                          }
                          
                          private final int func3(int b) {
                            int c = 2;
                            return c * b;
                          }
                        }
                    """,
                """
                        class A {
                          private int a;
                          
                          private static int func1(int b) {
                            int c = 2;
                            return c + b;
                          }
                          
                          static final int func2(int b) {
                            int c = 2;
                            return c - b;
                          }
                          
                          private static final int func3(int b) {
                            int c = 2;
                            return c * b;
                          }
                        }
                    """
            )
        );
    }

    @Test
    void overridableMethodsIgnored() {
        rewriteRun(
            java("""
                        class A {
                          public int func1(int a, int b) {
                            return a + b;
                          }
                        
                          protected int func2(int a, int b) {
                            return a - b;
                          }
                        
                          int func3(int a, int b) {
                            return a * b;
                          }
                        }
                    """
            )
        );
    }

    @Test
    void instanceMethodAccessed() {
        rewriteRun(
            java("""
                        class A {
                          int a = 1;
                        
                          private int fun1() {
                            return a;
                          }
                          
                          private int fun2(int b) {
                            return fun1() + b;
                          }
                          
                          final int func3(int b) {
                            return fun1() - b;
                          }
                          
                          private final int func4(int b) {
                            return fun1() * b;
                          }
                        }
                    """
            )
        );
    }

    @Test
    void classVariableAccessed() {
        rewriteRun(
            java("""
                        class A {
                          private static int a = 1;
                        
                          private int func1() {
                            return a;
                          }
                          
                          final int func2() {
                            return a + 1;
                          }
                          
                          private final int func3() {
                            return a + 2;
                          }
                        }
                    """,
                """
                        class A {
                          private static int a = 1;
                        
                          private static int func1() {
                            return a;
                          }
                          
                          static final int func2() {
                            return a + 1;
                          }
                          
                          private static final int func3() {
                            return a + 2;
                          }
                        }
                    """
            )
        );
    }

    @Test
    void classMethodAccessed() {
        rewriteRun(
            java("""
                        class A {
                          private static int a = 1;
                        
                          private static int func1() {
                            return a;
                          }
                        
                          private int func2(int b) {
                            return func1() + b;
                          }
                        }
                    """,
                """
                        class A {
                          private static int a = 1;
                                            
                          private static int func1() {
                            return a;
                          }
                        
                          private static int func2(int b) {
                            return func1() + b;
                          }
                        }
                    """
            )
        );
    }

    @Test
    void sameNameInstanceVariableInScope() {
        rewriteRun(
            java("""
                        class A {
                          int a = 1;
                        
                          private int func(int b) {
                            int c = 2;
                            for (int i = 0; i < b ;i++) {
                              int a = 2;
                              c += a;
                            }
                            return c + 1;
                          }
                        }
                    """
            )
        );
    }

    @Disabled("The same name local variable could make compile errors if without a scope check.")
    @Test
    void sameNameInstanceVariable() {
        rewriteRun(
            java("""
                        class A {
                          int a = 1;
                        
                          private int func(int b) {
                            int a = 2;
                            return a + b;
                          }
                        }
                    """,
                """
                        class A {
                          int a = 1;
                        
                          private static int func(int b) {
                            int a = 2;
                            return a + b;
                          }
                        }
                    """
            )
        );
    }

    @Test
    void sameNameClassVariableInScope() {
        rewriteRun(
            java("""
                        class A {
                          static int a = 1;
                        
                          private int func(int b) {
                            int c = 2;
                            for (int i = 0; i < b ;i++) {
                              int a = 2;
                              c += a;
                            }
                            return c + a;
                          }
                        }
                    """,
                """
                        class A {
                          static int a = 1;
                        
                          private static int func(int b) {
                            int c = 2;
                            for (int i = 0; i < b ;i++) {
                              int a = 2;
                              c += a;
                            }
                            return c + a;
                          }
                        }
                    """
            )
        );
    }

    // todo, kunli, for this case, current recipe staticize `func1` only. going to support this case,
    @Disabled
    @Test
    void nestedClassMethods() {
        rewriteRun(
            java("""
                        class A {
                          private static int a = 1;
                          private static int b = 2;
                        
                          private int func1() {
                            return a * 2;
                          }
                        
                          private int func2() {
                            int x = func1();
                            return x + b;
                          }
                        }
                    """,
                """
                        class A {
                          private static int a = 1;
                          private static int b = 2;
                        
                          private static int func1() {
                            return a * 2;
                          }
                        
                          private static int func2() {
                            int x = func1();
                            return x + b;
                          }
                        }
                    """
            )
        );
    }

    @Test
    void staticClassIgnored() {
        rewriteRun(
            java("""
                        class A {
                          int a = 1;
                        
                          private int func1() {
                            B b = new B();
                            return a + 1;
                          }
                        
                          private static class B {
                            int b = 0;
                            int func2(int x) {
                              return x + 1;
                            }
                          }
                        }
                    """
            )
        );
    }

    @Test
    void methodNameIsSameWithInstanceVariable() {
        rewriteRun(
            java("""
                    class A {
                      int count = 1;
                    
                      private int count(int x) {
                        return count + x;
                      }
                    }
                    """
            )
        );
    }
}
