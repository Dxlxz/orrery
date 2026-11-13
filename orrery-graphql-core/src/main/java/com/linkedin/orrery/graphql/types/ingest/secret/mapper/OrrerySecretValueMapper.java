package com.linkedin.orrery.graphql.types.ingest.secret.mapper;

import static com.linkedin.metadata.Constants.SECRET_VALUE_ASPECT_NAME;

import com.linkedin.common.AuditStamp;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.SetMode;
import com.linkedin.entity.EntityResponse;
import com.linkedin.secret.OrrerySecretValue;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Maps Pegasus {@link RecordTemplate} objects to objects conforming to the GQL schema.
 *
 * <p>To be replaced by auto-generated mappers implementations
 */
public class OrrerySecretValueMapper {

  public static final OrrerySecretValueMapper INSTANCE = new OrrerySecretValueMapper();

  public static OrrerySecretValue map(
      EntityResponse fromSecret,
      @Nonnull final String name,
      @Nonnull final String value,
      String description,
      AuditStamp auditStamp) {
    return INSTANCE.apply(fromSecret, name, value, description, auditStamp);
  }

  public OrrerySecretValue apply(
      EntityResponse existingSecret,
      @Nonnull final String name,
      @Nonnull final String value,
      String description,
      AuditStamp auditStamp) {
    final OrrerySecretValue result;
    if (Objects.nonNull(existingSecret)) {
      result =
          new OrrerySecretValue(
              existingSecret.getAspects().get(SECRET_VALUE_ASPECT_NAME).getValue().data());
    } else {
      result = new OrrerySecretValue();
    }

    result.setName(name);
    result.setValue(value);
    result.setDescription(description, SetMode.IGNORE_NULL);
    if (Objects.nonNull(auditStamp)) {
      result.setCreated(auditStamp);
    }

    return result;
  }
}
