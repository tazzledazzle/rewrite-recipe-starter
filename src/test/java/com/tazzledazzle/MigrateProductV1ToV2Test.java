package com.tazzledazzle;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import static org.openrewrite.java.Assertions.java;

public class MigrateProductV1ToV2Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec recipeSpec) {
        recipeSpec.parser(JavaParser.fromJavaVersion().classpath("example-toolkit"));
        recipeSpec.recipe(new MigrateProductV1ToV2Recipe());
    }

    @Test
    public void migrate() {
        rewriteRun(
          java(
            """
            import com.example.rewritelab.toolkit.v1.model.Product;
            import java.math.BigDecimal;

            public class A {
                public void configureProduct(Product product) {
                    product.setName("A");
                    product.setPrice(new BigDecimal("10.00"));
                    product.setQuantity(2);
                }
            }
            """,
            """
            import com.example.rewritelab.toolkit.v2.model.Product;

            import java.math.BigDecimal;

            public class A {
                public void configureProduct(Product product) {
                    product.setName("A")
                    .setPrice(new BigDecimal("10.00"))
                    .setQuantity(2);
                }
            }
            """)
        );
    }
}
