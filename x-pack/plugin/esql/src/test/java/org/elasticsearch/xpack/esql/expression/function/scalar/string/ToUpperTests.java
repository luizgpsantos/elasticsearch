/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.esql.expression.function.scalar.string;

import com.carrotsearch.randomizedtesting.annotations.Name;
import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;

import org.apache.lucene.util.BytesRef;
import org.elasticsearch.common.lucene.BytesRefs;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.esql.EsqlTestUtils;
import org.elasticsearch.xpack.esql.core.expression.Expression;
import org.elasticsearch.xpack.esql.core.expression.FoldContext;
import org.elasticsearch.xpack.esql.core.expression.Literal;
import org.elasticsearch.xpack.esql.core.tree.Source;
import org.elasticsearch.xpack.esql.core.type.DataType;
import org.elasticsearch.xpack.esql.core.util.DateUtils;
import org.elasticsearch.xpack.esql.expression.function.TestCaseSupplier;
import org.elasticsearch.xpack.esql.expression.function.scalar.AbstractConfigurationFunctionTestCase;
import org.elasticsearch.xpack.esql.plugin.EsqlPlugin;
import org.elasticsearch.xpack.esql.plugin.QueryPragmas;
import org.elasticsearch.xpack.esql.session.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.hamcrest.Matchers.equalTo;

public class ToUpperTests extends AbstractConfigurationFunctionTestCase {
    public ToUpperTests(@Name("TestCase") Supplier<TestCaseSupplier.TestCase> testCaseSupplier) {
        this.testCase = testCaseSupplier.get();
    }

    @ParametersFactory
    public static Iterable<Object[]> parameters() {
        List<TestCaseSupplier> suppliers = new ArrayList<>();

        suppliers.add(supplier("keyword ascii", DataType.KEYWORD, () -> randomAlphaOfLengthBetween(1, 10)));
        suppliers.add(supplier("keyword unicode", DataType.KEYWORD, () -> randomUnicodeOfLengthBetween(1, 10)));
        suppliers.add(supplier("text ascii", DataType.TEXT, () -> randomAlphaOfLengthBetween(1, 10)));
        suppliers.add(supplier("text unicode", DataType.TEXT, () -> randomUnicodeOfLengthBetween(1, 10)));
        suppliers.add(supplier("semantic_text ascii", DataType.SEMANTIC_TEXT, () -> randomAlphaOfLengthBetween(1, 10)));
        suppliers.add(supplier("semantic_text unicode", DataType.SEMANTIC_TEXT, () -> randomUnicodeOfLengthBetween(1, 10)));
        return parameterSuppliersFromTypedDataWithDefaultChecksNoErrors(true, suppliers);
    }

    public void testRandomLocale() {
        String testString = randomAlphaOfLength(10);
        Configuration cfg = randomLocaleConfig();
        ToUpper func = new ToUpper(Source.EMPTY, new Literal(Source.EMPTY, testString, DataType.KEYWORD), cfg);
        assertThat(BytesRefs.toBytesRef(testString.toUpperCase(cfg.locale())), equalTo(func.fold(FoldContext.small())));
    }

    private Configuration randomLocaleConfig() {
        return new Configuration(
            DateUtils.UTC,
            randomLocale(random()),
            null,
            null,
            new QueryPragmas(Settings.EMPTY),
            EsqlPlugin.QUERY_RESULT_TRUNCATION_MAX_SIZE.getDefault(Settings.EMPTY),
            EsqlPlugin.QUERY_RESULT_TRUNCATION_DEFAULT_SIZE.getDefault(Settings.EMPTY),
            "",
            false,
            Map.of(),
            System.nanoTime(),
            randomBoolean()
        );
    }

    @Override
    protected Expression buildWithConfiguration(Source source, List<Expression> args, Configuration configuration) {
        return new ToUpper(source, args.get(0), configuration);
    }

    private static TestCaseSupplier supplier(String name, DataType type, Supplier<String> valueSupplier) {
        return new TestCaseSupplier(name, List.of(type), () -> {
            List<TestCaseSupplier.TypedData> values = new ArrayList<>();
            String expectedToString = "ChangeCaseEvaluator[val=Attribute[channel=0], locale=en_US, caseType=UPPER]";

            String value = valueSupplier.get();
            values.add(new TestCaseSupplier.TypedData(new BytesRef(value), type, "0"));

            String expectedValue = value.toUpperCase(EsqlTestUtils.TEST_CFG.locale());
            return new TestCaseSupplier.TestCase(values, expectedToString, type, equalTo(new BytesRef(expectedValue)));
        });
    }
}
