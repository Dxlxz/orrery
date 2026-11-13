package com.linkedin.gms.servlet;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan(
    basePackages = {
      "io.orreryproject.openapi.config",
      "io.orreryproject.openapi.health",
      "io.orreryproject.openapi.openlineage",
      "io.orreryproject.openapi.operations",
      "io.orreryproject.openapi.platform",
      "io.orreryproject.openapi.relationships",
      "io.orreryproject.openapi.timeline",
      "io.orreryproject.openapi.entities",
      "io.orreryproject.openapi.v1",
      "io.orreryproject.openapi.v2",
      "io.orreryproject.openapi.v3",
      "io.orreryproject.openapi.analytics",
      "com.linkedin.gms.factory.timeline",
      "com.linkedin.gms.factory.event",
      "com.linkedin.gms.factory.s3",
      "org.springdoc",
      "com.linkedin.gms.factory.system_info"
    })
@Configuration
public class OpenAPIServletConfig {}
