package com.linkedin.gms.servlet;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan(basePackages = {"com.orrery.auth.authentication", "com.orrery.authorization"})
@Configuration
public class AuthServletConfig {}
