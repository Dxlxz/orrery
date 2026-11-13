package orrery.protobuf.visitors.dataset;

import static orrery.protobuf.ProtobufUtils.getMessageOptions;

import com.linkedin.common.GlossaryTermAssociation;
import orrery.protobuf.visitors.ProtobufExtensionUtil;
import orrery.protobuf.visitors.ProtobufModelVisitor;
import orrery.protobuf.visitors.VisitContext;
import java.util.stream.Stream;

public class TermAssociationVisitor implements ProtobufModelVisitor<GlossaryTermAssociation> {

  @Override
  public Stream<GlossaryTermAssociation> visitGraph(VisitContext context) {
    return ProtobufExtensionUtil.extractTermAssociationsFromOptions(
        getMessageOptions(context.root().messageProto()), context.getGraph().getRegistry());
  }
}
