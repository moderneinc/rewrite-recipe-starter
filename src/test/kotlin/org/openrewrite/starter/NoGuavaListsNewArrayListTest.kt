/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.starter

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

class NoGuavaListsNewArrayListTest: RewriteTest {

    //Note, you can define defaults for the RecipeSpec and these defaults will be used for all tests.
    //In this case, the recipe and the parser are common. See below, on how the defaults can be overriden
    //per test.
    override fun defaults(spec: RecipeSpec) {
        spec
            .recipe(NoGuavaListsNewArrayList())
            .parser(JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(true)
                .classpath("guava")
                .build())
    }

    @Test
    fun replaceWithNewArrayList() = rewriteRun(
        //There is an overloaded version or rewriteRun that allows the RecipeSpec to be customized specifically
        //for a given test. In this case, the parser for this test is configured to not log compilation warnings.
        { spec -> spec
            .parser(JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .classpath("guava")
                .build())
        },
        java(
            """
                import com.google.common.collect.*;
                
                import java.util.List;
                
                class Test {
                    List<Integer> cardinalsWorldSeries = Lists.newArrayList();
                }
            """,
            """
                import java.util.ArrayList;
                import java.util.List;
                
                class Test {
                    List<Integer> cardinalsWorldSeries = new ArrayList<>();
                }
            """
        )
    )

    @Test
    fun replaceWithNewArrayListIterable() = rewriteRun(
        java(
            """
                import com.google.common.collect.*;
                
                import java.util.Collections;
                import java.util.List;
                
                class Test {
                    List<Integer> l = Collections.emptyList();
                    List<Integer> cardinalsWorldSeries = Lists.newArrayList(l);
                }
            """,
            """
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.List;
                
                class Test {
                    List<Integer> l = Collections.emptyList();
                    List<Integer> cardinalsWorldSeries = new ArrayList<>(l);
                }
            """
        )
    )

    @Test
    fun replaceWithNewArrayListWithCapacity() = rewriteRun(
        java(
            """
                import com.google.common.collect.*;
                
                import java.util.ArrayList;
                import java.util.List;
                
                class Test {
                    List<Integer> cardinalsWorldSeries = Lists.newArrayListWithCapacity(2);
                }
            """,
            """
                import java.util.ArrayList;
                import java.util.List;
                
                class Test {
                    List<Integer> cardinalsWorldSeries = new ArrayList<>(2);
                }
            """
        )
    )
}
