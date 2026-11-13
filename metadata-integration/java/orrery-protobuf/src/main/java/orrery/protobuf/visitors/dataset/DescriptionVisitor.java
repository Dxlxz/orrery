package orrery.protobuf.visitors.dataset;

import orrery.protobuf.visitors.ProtobufModelVisitor;
import orrery.protobuf.visitors.VisitContext;
import java.util.stream.Stream;

public class DescriptionVisitor implements ProtobufModelVisitor<String> {

  @Override
  public Stream<String> visitGraph(VisitContext context) {
    return Stream.of(context.root().comment());
  }
}
