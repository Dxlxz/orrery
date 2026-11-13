package orrery.protobuf.visitors.field;

import com.linkedin.schema.SchemaField;
import com.linkedin.util.Pair;
import orrery.protobuf.model.ProtobufField;
import orrery.protobuf.visitors.ProtobufModelVisitor;
import orrery.protobuf.visitors.VisitContext;
import java.util.stream.Stream;

public class SchemaFieldVisitor implements ProtobufModelVisitor<Pair<SchemaField, Double>> {

  @Override
  public Stream<Pair<SchemaField, Double>> visitField(ProtobufField field, VisitContext context) {
    return context
        .streamAllPaths(field)
        .map(
            path ->
                Pair.of(
                    new SchemaField()
                        .setFieldPath(context.getFieldPath(path))
                        .setNullable(true)
                        .setDescription(field.comment())
                        .setNativeDataType(field.nativeType())
                        .setType(field.schemaFieldDataType()),
                    context.calculateSortOrder(path, field)));
  }
}
