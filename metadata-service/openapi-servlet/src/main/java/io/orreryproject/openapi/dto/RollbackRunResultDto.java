package io.orreryproject.openapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.orreryproject.openapi.generated.AspectRowSummary;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RollbackRunResultDto {
  List<AspectRowSummary> rowsRolledBack;
  Integer rowsDeletedFromEntityDeletion;
}
