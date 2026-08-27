/*
 * Copyright 2025 the original author or authors.
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
package com.yourorg;

import com.yourorg.table.SensitiveDataInLogsReport;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.Validated;
import org.openrewrite.analysis.InvocationMatcher;
import org.openrewrite.analysis.dataflow.DataFlowNode;
import org.openrewrite.analysis.dataflow.Dataflow;
import org.openrewrite.analysis.dataflow.TaintFlowSpec;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.marker.SearchResult;

/**
 * Demonstrates taint tracking with {@link TaintFlowSpec}. A plain {@link MethodMatcher} can only tell you that a
 * sensitive value is logged <em>directly</em>; taint tracking additionally follows the value as it is copied into
 * local variables and derived into new values, so {@code log.info("user " + account.getPassword())} is reported
 * just as reliably as {@code log.info(account.getPassword())}.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class FindSensitiveDataInLogs extends Recipe {

    private static final String DEFAULT_LOGGING_METHOD_PATTERN = "org.slf4j.Logger *(..)";

    transient SensitiveDataInLogsReport report = new SensitiveDataInLogsReport(this);

    String displayName = "Find sensitive data reaching log statements";

    String description = "Uses taint tracking to find values returned by a sensitive method that reach a logging " +
                         "call, whether they are logged directly or first assigned to a local variable and combined " +
                         "with other text. Every expression along the flow is a taint step, so the value does not " +
                         "have to arrive at the log call unchanged.";

    @Option(displayName = "Sensitive method pattern",
            description = "A [method pattern](https://docs.openrewrite.org/reference/method-patterns) matching calls " +
                          "that return sensitive data. These are the sources of the taint analysis.",
            example = "com.yourorg.Account getPassword()")
    String sensitiveMethodPattern;

    @Option(displayName = "Logging method pattern",
            description = "A [method pattern](https://docs.openrewrite.org/reference/method-patterns) matching " +
                          "logging calls. Arguments to these calls are the sinks of the taint analysis. " +
                          "Defaults to `" + DEFAULT_LOGGING_METHOD_PATTERN + "`.",
            example = "org.apache.logging.log4j.Logger *(..)",
            required = false)
    @Nullable
    String loggingMethodPattern;

    @Override
    public Validated<Object> validate() {
        return super.validate()
                .and(MethodMatcher.validate(sensitiveMethodPattern))
                .and(MethodMatcher.validate(loggingMethodPattern));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        String loggingPattern = loggingMethodPattern == null ? DEFAULT_LOGGING_METHOD_PATTERN : loggingMethodPattern;
        InvocationMatcher sensitive = InvocationMatcher.fromMethodMatcher(new MethodMatcher(sensitiveMethodPattern, true));
        InvocationMatcher logging = InvocationMatcher.fromMethodMatcher(new MethodMatcher(loggingPattern, true));

        // A TaintFlowSpec, unlike a plain DataFlowSpec, treats values *derived* from the source as still sensitive.
        TaintFlowSpec spec = new TaintFlowSpec() {
            @Override
            public boolean isSource(DataFlowNode srcNode) {
                Object value = srcNode.getCursor().getValue();
                return value instanceof Expression && sensitive.matches((Expression) value);
            }

            @Override
            public boolean isSink(DataFlowNode sinkNode) {
                return logging.advanced().isAnyArgument(sinkNode.getCursor());
            }
        };

        return Preconditions.check(new UsesMethod<>(loggingPattern, true), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public Expression visitExpression(Expression expression, ExecutionContext ctx) {
                Expression e = super.visitExpression(expression, ctx);
                Cursor cursor = getCursor();
                // findSinks() only returns a summary when this expression is a source that actually reaches a sink,
                // so no separate "is this a sensitive call" check is needed here.
                return Dataflow.startingAt(cursor).findSinks(spec).map(summary -> {
                    String sourcePath = cursor.firstEnclosingOrThrow(JavaSourceFile.class).getSourcePath().toString();
                    for (Cursor sinkCursor : summary.getSinkCursors()) {
                        report.insertRow(ctx, new SensitiveDataInLogsReport.Row(
                                sourcePath,
                                e.printTrimmed(cursor.getParentOrThrow()),
                                sinkCursor.<Expression>getValue().printTrimmed(sinkCursor.getParentOrThrow())));
                    }
                    return SearchResult.found(e);
                }).orSome(e);
            }
        });
    }
}
