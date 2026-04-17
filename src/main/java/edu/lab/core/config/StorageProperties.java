package edu.lab.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lab.storage")
public record StorageProperties(String basePath) {
}