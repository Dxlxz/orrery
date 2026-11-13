package orrery.client.v2.exceptions;

import com.linkedin.common.urn.Urn;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Exception thrown when saving entity changes to the Orrery server fails.
 *
 * <p>This typically indicates:
 *
 * <ul>
 *   <li>Server rejected the MetadataChangeProposal (validation errors)
 *   <li>Authentication/authorization failures
 *   <li>Network failures during save operation
 *   <li>Server-side errors
 * </ul>
 *
 * <p><b>Retry strategy:</b> Sometimes retryable depending on the cause. Check the cause to
 * determine if retry is appropriate (e.g., retry network errors, don't retry validation errors).
 */
public class EntitySaveException extends OrreryClientException {

  /**
   * Constructs an EntitySaveException.
   *
   * @param entityUrn the URN of the entity that failed to save
   * @param cause the underlying exception that caused the save failure
   */
  public EntitySaveException(@Nonnull Urn entityUrn, @Nullable Throwable cause) {
    super(String.format("Failed to save entity %s", entityUrn), cause, entityUrn, null, "save");
  }

  /**
   * Constructs an EntitySaveException with a custom message.
   *
   * @param message custom error message
   * @param entityUrn the URN of the entity that failed to save
   * @param cause the underlying exception that caused the save failure
   */
  public EntitySaveException(
      @Nonnull String message, @Nonnull Urn entityUrn, @Nullable Throwable cause) {
    super(message, cause, entityUrn, null, "save");
  }
}
