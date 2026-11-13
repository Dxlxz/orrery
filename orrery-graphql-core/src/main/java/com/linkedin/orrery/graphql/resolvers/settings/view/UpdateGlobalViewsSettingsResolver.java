package com.linkedin.orrery.graphql.resolvers.settings.view;

import static com.linkedin.orrery.graphql.resolvers.ResolverUtils.*;

import com.linkedin.common.urn.UrnUtils;
import com.linkedin.data.template.SetMode;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.authorization.AuthorizationUtils;
import com.linkedin.orrery.graphql.concurrency.GraphQLConcurrencyUtils;
import com.linkedin.orrery.graphql.exception.AuthorizationException;
import com.linkedin.orrery.graphql.generated.UpdateGlobalViewsSettingsInput;
import com.linkedin.metadata.service.SettingsService;
import com.linkedin.settings.global.GlobalSettingsInfo;
import com.linkedin.settings.global.GlobalViewsSettings;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

/**
 * Resolver responsible for updating the Global Views settings.
 *
 * <p>This capability requires the 'MANAGE_GLOBAL_VIEWS' Platform Privilege.
 */
public class UpdateGlobalViewsSettingsResolver implements DataFetcher<CompletableFuture<Boolean>> {

  private final SettingsService _settingsService;

  public UpdateGlobalViewsSettingsResolver(@Nonnull final SettingsService settingsService) {
    _settingsService = Objects.requireNonNull(settingsService, "settingsService must not be null");
  }

  @Override
  public CompletableFuture<Boolean> get(final DataFetchingEnvironment environment)
      throws Exception {
    final QueryContext context = environment.getContext();
    final UpdateGlobalViewsSettingsInput input =
        bindArgument(environment.getArgument("input"), UpdateGlobalViewsSettingsInput.class);

    return GraphQLConcurrencyUtils.supplyAsync(
        () -> {
          if (AuthorizationUtils.canManageGlobalViews(context)) {
            try {
              // First, fetch the existing global settings. This does a R-M-F.
              final GlobalSettingsInfo maybeGlobalSettings =
                  _settingsService.getGlobalSettings(context.getOperationContext());

              final GlobalSettingsInfo newGlobalSettings =
                  maybeGlobalSettings != null ? maybeGlobalSettings : new GlobalSettingsInfo();

              final GlobalViewsSettings newGlobalViewsSettings =
                  newGlobalSettings.hasViews()
                      ? newGlobalSettings.getViews()
                      : new GlobalViewsSettings();

              // Next, patch the global views settings.
              updateViewsSettings(newGlobalViewsSettings, input);
              newGlobalSettings.setViews(newGlobalViewsSettings);

              // Finally, write back to GMS.
              _settingsService.updateGlobalSettings(
                  context.getOperationContext(), newGlobalSettings);
              return true;
            } catch (Exception e) {
              throw new RuntimeException(
                  String.format("Failed to update global view settings! %s", input), e);
            }
          }
          throw new AuthorizationException(
              "Unauthorized to perform this action. Please contact your Orrery administrator.");
        },
        this.getClass().getSimpleName(),
        "get");
  }

  private static void updateViewsSettings(
      @Nonnull final com.linkedin.settings.global.GlobalViewsSettings settings,
      @Nonnull final UpdateGlobalViewsSettingsInput input) {
    settings.setDefaultView(
        input.getDefaultView() != null ? UrnUtils.getUrn(input.getDefaultView()) : null,
        SetMode.REMOVE_IF_NULL);
  }
}
