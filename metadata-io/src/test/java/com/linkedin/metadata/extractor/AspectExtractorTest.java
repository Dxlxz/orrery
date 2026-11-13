package com.linkedin.metadata.extractor;

import static org.testng.Assert.assertEquals;

import com.orrery.test.TestEntityAspect;
import com.orrery.test.TestEntityAspectArray;
import com.orrery.test.TestEntityInfo;
import com.orrery.test.TestEntityKey;
import com.orrery.test.TestEntitySnapshot;
import com.linkedin.common.urn.Urn;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.metadata.TestEntityUtil;
import com.linkedin.metadata.models.extractor.AspectExtractor;
import java.util.Map;
import org.testng.annotations.Test;

public class AspectExtractorTest {
  @Test
  public void testExtractor() {
    TestEntitySnapshot snapshot = new TestEntitySnapshot();
    Urn urn = TestEntityUtil.getTestEntityUrn();
    TestEntityKey testEntityKey = TestEntityUtil.getTestEntityKey(urn);
    TestEntityInfo testEntityInfo = TestEntityUtil.getTestEntityInfo(urn);
    snapshot.setAspects(
        new TestEntityAspectArray(
            TestEntityAspect.create(testEntityKey), TestEntityAspect.create(testEntityInfo)));
    Map<String, RecordTemplate> result = AspectExtractor.extractAspectRecords(snapshot);
    assertEquals(result.size(), 2);
    assertEquals(result.get("testEntityKey"), testEntityKey);
    assertEquals(result.get("testEntityInfo"), testEntityInfo);
  }
}
