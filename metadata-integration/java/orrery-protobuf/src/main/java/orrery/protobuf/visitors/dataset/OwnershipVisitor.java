package orrery.protobuf.visitors.dataset;

import static orrery.protobuf.ProtobufUtils.getMessageOptions;

import com.linkedin.common.Owner;
import com.linkedin.common.OwnershipSource;
import com.linkedin.common.OwnershipSourceType;
import com.linkedin.common.OwnershipType;
import com.linkedin.common.urn.Urn;
import orrery.protobuf.visitors.ProtobufExtensionUtil;
import orrery.protobuf.visitors.ProtobufModelVisitor;
import orrery.protobuf.visitors.VisitContext;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class OwnershipVisitor implements ProtobufModelVisitor<Owner> {

  @Override
  public Stream<Owner> visitGraph(VisitContext context) {
    return ProtobufExtensionUtil.filterByOrreryType(
            getMessageOptions(context.root().messageProto()),
            context.getGraph().getRegistry(),
            ProtobufExtensionUtil.OrreryMetadataType.OWNER)
        .stream()
        .flatMap(
            extEntry -> {
              if (extEntry.getKey().isRepeated()) {
                return ((Collection<String>) extEntry.getValue())
                    .stream().map(v -> Map.entry(extEntry.getKey(), v));
              } else {
                return Stream.of(Map.entry(extEntry.getKey(), (String) extEntry.getValue()));
              }
            })
        .map(
            entry -> {
              try {
                OwnershipType ownershipType;
                try {
                  ownershipType = OwnershipType.valueOf(entry.getKey().getName().toUpperCase());
                } catch (IllegalArgumentException e) {
                  ownershipType = OwnershipType.TECHNICAL_OWNER;
                }

                String[] id = entry.getValue().toLowerCase().split(":", 2);
                return new Owner()
                    .setType(ownershipType)
                    .setSource(new OwnershipSource().setType(OwnershipSourceType.MANUAL))
                    .setOwner(
                        new Urn(
                            id.length > 1
                                ? id[0].replaceFirst("corpgroup", "corpGroup")
                                : "corpGroup",
                            id[id.length - 1]));
              } catch (URISyntaxException e) {
                System.err.println(e.getMessage());
                return null;
              }
            })
        .filter(Objects::nonNull);
  }
}
