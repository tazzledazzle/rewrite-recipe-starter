package com.tazzledazzle;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MigrateProductV1ToV2Recipe extends Recipe {

    private static final String V1_PRODUCT = "com.example.rewritelab.toolkit.v1.model.Product";
    private static final String V2_PRODUCT = "com.example.rewritelab.toolkit.v2.model.Product";
    private static final MethodMatcher PRODUCT_SETTER = new MethodMatcher(V2_PRODUCT + " set*(..)");

    @Override
    public String getDisplayName() {
        return "Migrate Product v1 to v2";
    }

    @Override
    public String getDescription() {
        return "Migrates com.example.rewritelab.toolkit.v1.model.Product to v2 and chains fluent setters.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return Arrays.asList(
                new ChangeType(V1_PRODUCT, V2_PRODUCT, null),
                new ChainProductSetters()
        );
    }

    private static class ChainProductSetters extends Recipe {

        @Override
        public String getDisplayName() {
            return "Chain Product v2 setters";
        }

        @Override
        public String getDescription() {
            return "Chain consecutive Product setter calls into a fluent call chain.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return Preconditions.check(
                    new UsesMethod<>(PRODUCT_SETTER),
//                    new ChainProductSettersVisitor());
                    new JavaIsoVisitor<ExecutionContext>() {
                        @Override
                        public J.Block visitBlock(J.Block block, ExecutionContext executionContext) {
                            J.Block b = super.visitBlock(block, executionContext);
                            List<Statement> statements = b.getStatements();
                            List<Statement> newStatements = new ArrayList<>();

                            for (int i = 0; i < statements.size();) {
                                int chainLength = 1;
                                J.MethodInvocation first = null;
                                Statement firstStatement = statements.get(i);

                                if (firstStatement instanceof J.MethodInvocation) {
                                    first = (J.MethodInvocation) firstStatement;
                                }
                                if (first != null && PRODUCT_SETTER.matches(first)) {
                                    
                                }
                            }
                        };
                    });
        }
    }

    static class ChainProductSettersVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
            // get block details
            J.Block b = super.visitBlock(block, ctx);
            // get statements from block details
            List<Statement> statements = b.getStatements();
            // create list of same size as statements
            List<Statement> newStatements = new ArrayList<>(statements.size());

            // for all the statements, internally iterate
            for (int i = 0; i < statements.size(); ) {
                int chainLength = 1;
                J.MethodInvocation first = null;
                Statement statement2 = statements.get(i);

                // find first J.MethodInvocation
                if (statement2 instanceof J.MethodInvocation) {
                    first = (J.MethodInvocation) statement2;
                }

                if (first != null && PRODUCT_SETTER.matches(first)) {
                    // check the first MethodInvocation's select expression
                    Expression select1 = first.getSelect();
                    // null or the first methodInvocation's select Expression's trimmed cursor coordinates
                    String receiver = select1 == null ? null : select1.printTrimmed(getCursor());
                    // methodInvocation Select Expression Cursor coordinates
                    if (receiver != null) {
                        int length = 1;
                        // from i + 1 to size
                        for (int i2 = i + 1; i2 < statements.size(); i2++) {
                            // find the next one
                            J.MethodInvocation next1 = null;
                            Statement statement = statements.get(i2);
                            if (statement instanceof J.MethodInvocation) {
                                next1 = (J.MethodInvocation) statement;
                            }
                            Expression select = next1.getSelect();
                            if (next1 == null || !PRODUCT_SETTER.matches(next1) || !receiver.equals(select == null ? null : select.printTrimmed(getCursor()))) {
                                break;
                            }
                            // update chain length
                            length++;
                        }
                        // persist chain length
                        chainLength = length;
                    }
                }
                // if there's a chain
                if (chainLength > 1) {
                    J.MethodInvocation chained = null;
                    Statement statement1 = statements.get(i);
                    // start at the beginning
                    if (statement1 instanceof J.MethodInvocation) {
                        chained = (J.MethodInvocation) statement1;
                    }

                    Space firstPrefix = statements.get(i).getPrefix();
                    String continuationPrefix = firstPrefix.getWhitespace().replaceFirst("^\\R", "");
                    chained = chained.withPrefix(Space.EMPTY);

                    // i + 1 to chainLength + i by 1
                    for (int i1 = i + 1; i1 < i + chainLength; i1++) {
                        J.MethodInvocation next = null;
                        Statement statement = statements.get(i1);
                        if (statement instanceof J.MethodInvocation) {
                            next = (J.MethodInvocation) statement;
                        }
                        // construct the methodInvocation
                        chained = next.getPadding().withSelect(
                                JRightPadded.build((Expression) chained)
                                        .withAfter(Space.format("\n" + continuationPrefix))
                        ).withPrefix(Space.EMPTY);
                    }
                    // add it to our new statement list
                    newStatements.add(chained.withPrefix(firstPrefix));
                    // increment counter by chainLength
                    i += chainLength;
                } else {  // chainLength < 1
                    // add single chain
                    newStatements.add(statements.get(i));
                    // increment by one
                    i++;
                }
            }

            // if stmts == n_stmts ?: stmts else stmts.withStatements(n_stmts)
            return newStatements.equals(statements) ? b : b.withStatements(newStatements);
        }

    }
}
