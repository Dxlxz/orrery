package orrery.protobuf.visitors.dataset;

import static orrery.protobuf.ProtobufUtils.getMessageOptions;

import com.google.gson.Gson;
import com.google.protobuf.Descriptors;
import com.linkedin.data.template.StringMap;
import com.linkedin.dataset.DatasetProperties;
import com.linkedin.util.Pair;
import orrery.protobuf.visitors.ProtobufExtensionUtil;
import orrery.protobuf.visitors.ProtobufModelVisitor;
import orrery.protobuf.visitors.VisitContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class PropertyVisitor implements ProtobufModelVisitor<DatasetProperties> {
  private static final Gson GSON = new Gson();

  @Override
  public Stream<DatasetProperties> visitGraph(VisitContext context) {
    Map<String, String> properties = new HashMap<>();

    List<Pair<Descriptors.FieldDescriptor, Object>> propertyOptions =
        ProtobufExtensionUtil.filterByOrreryType(
            getMessageOptions(context.root().messageProto()),
            context.getGraph().getRegistry(),
            ProtobufExtensionUtil.OrreryMetadataType.PROPERTY);

    for (Pair<Descriptors.FieldDescriptor, Object> fd : propertyOptions) {
      var fieldDescriptor = fd.getKey();
      if (fieldDescriptor.getJavaType() != Descriptors.FieldDescriptor.JavaType.MESSAGE) {
        if (fieldDescriptor.isRepeated()) {
          List<String> stringValues = new ArrayList<>();
          for (Object item : (Collection<?>) fd.getValue()) {
            stringValues.add(item.toString());
          }
          properties.put(fieldDescriptor.getName(), GSON.toJson(stringValues));
        } else {
          properties.put(fieldDescriptor.getName(), fd.getValue().toString());
        }
      }
    }

    return Stream.of(new DatasetProperties().setCustomProperties(new StringMap(properties)));
  }
}
