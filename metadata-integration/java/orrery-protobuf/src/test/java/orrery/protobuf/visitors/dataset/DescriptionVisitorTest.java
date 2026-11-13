package orrery.protobuf.visitors.dataset;

import static orrery.protobuf.TestFixtures.*;
import static org.testng.Assert.assertEquals;

import orrery.protobuf.model.ProtobufGraph;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.testng.annotations.Test;

public class DescriptionVisitorTest {

  @Test
  public void visitorTest() throws IOException {
    ProtobufGraph graph = getTestProtobufGraph("protobuf", "messageC2", "protobuf.MessageC2");

    DescriptionVisitor test = new DescriptionVisitor();

    assertEquals(
        Set.of("This contains nested type\n\nDescription for MessageC2"),
        graph
            .accept(getVisitContextBuilder("protobuf.MessageC2"), List.of(test))
            .collect(Collectors.toSet()));
  }
}
