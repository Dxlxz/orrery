package com.linkedin.orrery.graphql.types.assertion;

import com.linkedin.assertion.FieldAssertionInfo;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.AssertionStdOperator;
import com.linkedin.orrery.graphql.generated.FieldAssertionType;
import com.linkedin.orrery.graphql.generated.FieldMetricType;
import com.linkedin.orrery.graphql.generated.FieldTransformType;
import com.linkedin.orrery.graphql.generated.FieldValuesFailThresholdType;
import com.linkedin.orrery.graphql.types.dataset.mappers.DatasetFilterMapper;
import javax.annotation.Nullable;

public class FieldAssertionMapper extends AssertionMapper {

  public static com.linkedin.orrery.graphql.generated.FieldAssertionInfo mapFieldAssertionInfo(
      @Nullable final QueryContext context, final FieldAssertionInfo gmsFieldAssertionInfo) {
    final com.linkedin.orrery.graphql.generated.FieldAssertionInfo result =
        new com.linkedin.orrery.graphql.generated.FieldAssertionInfo();
    result.setEntityUrn(gmsFieldAssertionInfo.getEntity().toString());
    result.setType(FieldAssertionType.valueOf(gmsFieldAssertionInfo.getType().name()));
    if (gmsFieldAssertionInfo.hasFilter()) {
      result.setFilter(DatasetFilterMapper.map(context, gmsFieldAssertionInfo.getFilter()));
    }
    if (gmsFieldAssertionInfo.hasFieldValuesAssertion()) {
      result.setFieldValuesAssertion(
          mapFieldValuesAssertion(gmsFieldAssertionInfo.getFieldValuesAssertion()));
    }
    if (gmsFieldAssertionInfo.hasFieldMetricAssertion()) {
      result.setFieldMetricAssertion(
          mapFieldMetricAssertion(gmsFieldAssertionInfo.getFieldMetricAssertion()));
    }
    return result;
  }

  private static com.linkedin.orrery.graphql.generated.FieldValuesAssertion
      mapFieldValuesAssertion(
          final com.linkedin.assertion.FieldValuesAssertion gmsFieldValuesAssertion) {
    final com.linkedin.orrery.graphql.generated.FieldValuesAssertion result =
        new com.linkedin.orrery.graphql.generated.FieldValuesAssertion();
    result.setField(mapSchemaFieldSpec(gmsFieldValuesAssertion.getField()));
    result.setOperator(AssertionStdOperator.valueOf(gmsFieldValuesAssertion.getOperator().name()));
    result.setFailThreshold(
        mapFieldValuesFailThreshold(gmsFieldValuesAssertion.getFailThreshold()));
    result.setExcludeNulls(gmsFieldValuesAssertion.isExcludeNulls());

    if (gmsFieldValuesAssertion.hasTransform()) {
      result.setTransform(mapFieldTransform(gmsFieldValuesAssertion.getTransform()));
    }

    if (gmsFieldValuesAssertion.hasParameters()) {
      result.setParameters(mapParameters(gmsFieldValuesAssertion.getParameters()));
    }
    return result;
  }

  private static com.linkedin.orrery.graphql.generated.FieldMetricAssertion
      mapFieldMetricAssertion(
          final com.linkedin.assertion.FieldMetricAssertion gmsFieldMetricAssertion) {
    final com.linkedin.orrery.graphql.generated.FieldMetricAssertion result =
        new com.linkedin.orrery.graphql.generated.FieldMetricAssertion();
    result.setField(mapSchemaFieldSpec(gmsFieldMetricAssertion.getField()));
    result.setMetric(FieldMetricType.valueOf(gmsFieldMetricAssertion.getMetric().name()));
    result.setOperator(AssertionStdOperator.valueOf(gmsFieldMetricAssertion.getOperator().name()));

    if (gmsFieldMetricAssertion.hasParameters()) {
      result.setParameters(mapParameters(gmsFieldMetricAssertion.getParameters()));
    }

    return result;
  }

  private static com.linkedin.orrery.graphql.generated.FieldTransform mapFieldTransform(
      final com.linkedin.assertion.FieldTransform gmsFieldTransform) {
    final com.linkedin.orrery.graphql.generated.FieldTransform result =
        new com.linkedin.orrery.graphql.generated.FieldTransform();
    result.setType(FieldTransformType.valueOf(gmsFieldTransform.getType().name()));
    return result;
  }

  private static com.linkedin.orrery.graphql.generated.FieldValuesFailThreshold
      mapFieldValuesFailThreshold(
          final com.linkedin.assertion.FieldValuesFailThreshold gmsFieldValuesFailThreshold) {
    final com.linkedin.orrery.graphql.generated.FieldValuesFailThreshold result =
        new com.linkedin.orrery.graphql.generated.FieldValuesFailThreshold();
    result.setType(
        FieldValuesFailThresholdType.valueOf(gmsFieldValuesFailThreshold.getType().name()));
    result.setValue(gmsFieldValuesFailThreshold.getValue());
    return result;
  }

  private FieldAssertionMapper() {}
}
