package orrery.protobuf.visitors;

import orrery.protobuf.model.ProtobufElement;
import orrery.protobuf.model.ProtobufField;
import orrery.protobuf.model.ProtobufMessage;
import java.util.stream.Stream;

public interface ProtobufModelVisitor<T> {
  default Stream<T> visitField(ProtobufField field, VisitContext context) {
    return visitElement(field, context);
  }

  default Stream<T> visitMessage(ProtobufMessage message, VisitContext context) {
    return visitElement(message, context);
  }

  default Stream<T> visitElement(ProtobufElement element, VisitContext context) {
    return Stream.of();
  }

  default Stream<T> visitGraph(VisitContext context) {
    return Stream.of();
  }
}
