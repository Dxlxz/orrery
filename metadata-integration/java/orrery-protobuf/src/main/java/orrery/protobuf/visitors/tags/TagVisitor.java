package orrery.protobuf.visitors.tags;

import static orrery.protobuf.ProtobufUtils.getFieldOptions;
import static orrery.protobuf.ProtobufUtils.getMessageOptions;

import com.linkedin.common.urn.TagUrn;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.events.metadata.ChangeType;
import com.linkedin.tag.TagProperties;
import orrery.event.MetadataChangeProposalWrapper;
import orrery.protobuf.model.ProtobufField;
import orrery.protobuf.visitors.ProtobufExtensionUtil;
import orrery.protobuf.visitors.ProtobufModelVisitor;
import orrery.protobuf.visitors.VisitContext;
import java.util.stream.Stream;

public class TagVisitor
    implements ProtobufModelVisitor<MetadataChangeProposalWrapper<? extends RecordTemplate>> {
  private static final String TAG_PROPERTIES_ASPECT = "tagProperties";

  @Override
  public Stream<MetadataChangeProposalWrapper<? extends RecordTemplate>> visitGraph(
      VisitContext context) {
    return ProtobufExtensionUtil.extractTagPropertiesFromOptions(
            getMessageOptions(context.root().messageProto()), context.getGraph().getRegistry())
        .map(TagVisitor::wrapTagProperty);
  }

  @Override
  public Stream<MetadataChangeProposalWrapper<? extends RecordTemplate>> visitField(
      ProtobufField field, VisitContext context) {
    return ProtobufExtensionUtil.extractTagPropertiesFromOptions(
            getFieldOptions(field.getFieldProto()), context.getGraph().getRegistry())
        .map(TagVisitor::wrapTagProperty);
  }

  private static MetadataChangeProposalWrapper<TagProperties> wrapTagProperty(
      TagProperties tagProperty) {
    return new MetadataChangeProposalWrapper<>(
        "tag",
        new TagUrn(tagProperty.getName()).toString(),
        ChangeType.UPSERT,
        tagProperty,
        TAG_PROPERTIES_ASPECT);
  }
}
