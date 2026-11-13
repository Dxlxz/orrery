package com.linkedin.orrery.graphql.util;

import com.linkedin.data.template.DoubleMap;
import com.linkedin.orrery.graphql.generated.SearchInsight;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

public class SearchInsightsUtil {

  public static List<SearchInsight> getInsightsFromFeatures(@Nullable final DoubleMap features) {
    // Currently no features are extracted.
    if (features == null) {
      return Collections.emptyList();
    }

    return Collections.emptyList();
  }

  private SearchInsightsUtil() {}
}
