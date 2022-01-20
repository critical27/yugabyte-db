package com.yugabyte.yw.common.logging;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import com.yugabyte.yw.commissioner.HealthChecker;
import com.yugabyte.yw.common.alerts.AlertConfigurationWriter;
import com.yugabyte.yw.common.alerts.AlertsGarbageCollector;
import com.yugabyte.yw.common.alerts.QueryAlerts;
import com.yugabyte.yw.common.config.DummyRuntimeConfigFactoryImpl;
import com.yugabyte.yw.common.config.RuntimeConfigFactory;
import kamon.instrumentation.play.GuiceModule;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.modules.swagger.SwaggerModule;
import play.test.Helpers;
import play.test.WithApplication;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.inject.Bindings.bind;

public abstract class MDCPropagatingDispatcherTestBase extends WithApplication {
  private final boolean isCloud;
  private Config mockConfig;
  private HealthChecker mockHealthChecker;
  private QueryAlerts mockQueryAlerts;
  private AlertsGarbageCollector mockAlertsGarbageCollector;
  private AlertConfigurationWriter mockAlertConfigurationWriter;

  protected MDCPropagatingDispatcherTestBase(boolean isCloud) {
    this.isCloud = isCloud;
  }

  @Override
  protected Application provideApplication() {
    mockConfig = mock(Config.class);
    mockHealthChecker = mock(HealthChecker.class);
    mockQueryAlerts = mock(QueryAlerts.class);
    mockAlertConfigurationWriter = mock(AlertConfigurationWriter.class);
    mockAlertsGarbageCollector = mock(AlertsGarbageCollector.class);
    when(mockConfig.getString(anyString())).thenReturn("");

    Config config =
        ConfigFactory.parseMap(Helpers.inMemoryDatabase())
            .withValue("yb.cloud.enabled", ConfigValueFactory.fromAnyRef(isCloud))
            .withValue(
                "akka.actor.default-dispatcher.type",
                ConfigValueFactory.fromAnyRef(
                    "com.yugabyte.yw.common.logging.MDCPropagatingDispatcherConfigurator"));
    return new GuiceApplicationBuilder()
        .disable(SwaggerModule.class)
        .disable(GuiceModule.class)
        .configure(config)
        .overrides(
            bind(RuntimeConfigFactory.class)
                .toInstance(new DummyRuntimeConfigFactoryImpl(mockConfig)))
        .overrides(bind(HealthChecker.class).toInstance(mockHealthChecker))
        .overrides(bind(QueryAlerts.class).toInstance(mockQueryAlerts))
        .overrides(bind(AlertConfigurationWriter.class).toInstance(mockAlertConfigurationWriter))
        .overrides(bind(AlertsGarbageCollector.class).toInstance(mockAlertsGarbageCollector))
        .build();
  }
}