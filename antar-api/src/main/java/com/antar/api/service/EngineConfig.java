package com.antar.api.service;

import com.antar.engine.GapEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * GapEngine lives in a framework-free module, so it carries no Spring annotations.
 * Registering it here keeps the dependency arrow pointing one way: api -> engine.
 */
@Configuration
public class EngineConfig {

    @Bean
    public GapEngine gapEngine() {
        return new GapEngine();
    }
}
