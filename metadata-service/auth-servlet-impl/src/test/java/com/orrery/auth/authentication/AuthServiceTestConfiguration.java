package com.orrery.auth.authentication;

import com.orrery.authentication.Actor;
import com.orrery.authentication.ActorType;
import com.orrery.authentication.Authentication;
import com.orrery.authentication.invite.InviteTokenService;
import com.orrery.authentication.session.UserSessionEligibilityChecker;
import com.orrery.authentication.token.StatelessTokenService;
import com.orrery.authentication.user.NativeUserService;
import com.orrery.authorization.AuthorizerChain;
import com.orrery.telemetry.TrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedin.gms.factory.config.ConfigurationProvider;
import com.linkedin.metadata.entity.EntityService;
import com.linkedin.metadata.utils.metrics.MetricUtils;
import io.orreryproject.metadata.context.ObjectMapperContext;
import io.orreryproject.metadata.context.OperationContext;
import io.orreryproject.metadata.context.SystemTelemetryContext;
import io.orreryproject.metadata.services.SecretService;
import io.orreryproject.test.metadata.context.TestOperationContexts;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

// All dependencies are provided as @Bean methods rather than @MockitoBean fields because
// this is a minimal @SpringBootTest(classes = {DispatcherServlet.class}) context with no
// factory beans — @MockitoBean requires an existing bean definition to override, but these
// services have none in this context (or are registered under different names/subtypes).
@TestConfiguration
public class AuthServiceTestConfiguration {

  public static String SYSTEM_CLIENT_ID = "systemClientId";

  // StatelessTokenService has no bean definition (production uses StatefulTokenService)
  @Bean
  @Primary
  public StatelessTokenService statelessTokenService() {
    return Mockito.mock(StatelessTokenService.class);
  }

  @Bean(name = "configurationProvider")
  public ConfigurationProvider configProvider() {
    return Mockito.mock(ConfigurationProvider.class);
  }

  @Bean
  @Primary
  public NativeUserService nativeUserService() {
    return Mockito.mock(NativeUserService.class);
  }

  @Bean
  @Primary
  public UserSessionEligibilityChecker userSessionEligibilityChecker() {
    return Mockito.mock(UserSessionEligibilityChecker.class);
  }

  @Bean
  @Primary
  @SuppressWarnings("unchecked")
  public EntityService<?> entityService() {
    return Mockito.mock(EntityService.class);
  }

  @Bean
  @Primary
  public SecretService secretService() {
    return Mockito.mock(SecretService.class);
  }

  @Bean
  @Primary
  public InviteTokenService inviteTokenService() {
    return Mockito.mock(InviteTokenService.class);
  }

  @Bean
  @Primary
  public TrackingService trackingService() {
    return Mockito.mock(TrackingService.class);
  }

  @Bean
  @Primary
  public AuthorizerChain authorizerChain() {
    return Mockito.mock(AuthorizerChain.class);
  }

  @Bean
  @Primary
  public SpanContext mockSpanContext() {
    return Mockito.mock(SpanContext.class);
  }

  @Bean
  @Primary
  public Tracer noopTestTracer() {
    return OpenTelemetry.noop().getTracer("auth-servlet-impl-test");
  }

  @Bean
  @Primary
  public MetricUtils metricUtils() {
    return Mockito.mock(MetricUtils.class);
  }

  @Bean(name = "systemOperationContext")
  public OperationContext systemOperationContext(
      ObjectMapper objectMapper, Tracer noopTestTracer, MetricUtils metricUtils) {
    SystemTelemetryContext mockSystemTelemetryContext =
        SystemTelemetryContext.builder().tracer(noopTestTracer).metricUtils(metricUtils).build();
    return TestOperationContexts.systemContextTraceNoSearchAuthorization(
        () -> ObjectMapperContext.builder().objectMapper(objectMapper).build(),
        () -> mockSystemTelemetryContext);
  }

  @Bean
  public Authentication systemAuthentication() {
    final Actor systemActor = new Actor(ActorType.USER, SYSTEM_CLIENT_ID);
    return new Authentication(
        systemActor, String.format("Basic %s:%s", SYSTEM_CLIENT_ID, "systemSecret"));
  }

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }
}
