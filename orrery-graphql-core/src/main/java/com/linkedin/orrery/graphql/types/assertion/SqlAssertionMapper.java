package com.linkedin.orrery.graphql.types.assertion;

import com.linkedin.assertion.SqlAssertionInfo;
import com.linkedin.orrery.graphql.generated.AssertionStdOperator;
import com.linkedin.orrery.graphql.generated.AssertionValueChangeType;
import com.linkedin.orrery.graphql.generated.SqlAssertionType;

public class SqlAssertionMapper extends AssertionMapper {

  public static com.linkedin.orrery.graphql.generated.SqlAssertionInfo mapSqlAssertionInfo(
      final SqlAssertionInfo gmsSqlAssertionInfo) {
    final com.linkedin.orrery.graphql.generated.SqlAssertionInfo result =
        new com.linkedin.orrery.graphql.generated.SqlAssertionInfo();
    result.setEntityUrn(gmsSqlAssertionInfo.getEntity().toString());
    result.setType(SqlAssertionType.valueOf(gmsSqlAssertionInfo.getType().name()));
    result.setStatement(gmsSqlAssertionInfo.getStatement());
    result.setOperator(AssertionStdOperator.valueOf(gmsSqlAssertionInfo.getOperator().name()));
    result.setParameters(mapParameters(gmsSqlAssertionInfo.getParameters()));
    if (gmsSqlAssertionInfo.hasChangeType()) {
      result.setChangeType(
          AssertionValueChangeType.valueOf(gmsSqlAssertionInfo.getChangeType().name()));
    }
    return result;
  }

  private SqlAssertionMapper() {}
}
