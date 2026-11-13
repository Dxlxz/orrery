package orrery.protobuf.visitors.dataset;

import static orrery.protobuf.ProtobufUtils.getMessageOptions;

import com.linkedin.common.urn.Urn;
import com.linkedin.util.Pair;
import orrery.protobuf.visitors.ProtobufExtensionUtil;
import orrery.protobuf.visitors.ProtobufModelVisitor;
import orrery.protobuf.visitors.VisitContext;
import java.util.stream.Stream;

public class DomainVisitor implements ProtobufModelVisitor<Urn> {

  @Override
  public Stream<Urn> visitGraph(VisitContext context) {
    return ProtobufExtensionUtil.filterByOrreryType(
            getMessageOptions(context.root().messageProto()),
            context.getGraph().getRegistry(),
            ProtobufExtensionUtil.OrreryMetadataType.DOMAIN)
        .stream()
        .map(Pair::getValue)
        .map(o -> Urn.createFromTuple("domain", ((String) o).toLowerCase()));
  }
}
