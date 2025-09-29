package com.hidoc.api.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Ensures that the MCP server components (tools, core registry, web controller)
 * under package com.hidoc.mcp are discovered by Spring, since the main
 * application package is com.hidoc.api and does not auto-scan sibling packages.
 */
@Configuration
@ComponentScan(basePackages = {"com.hidoc.mcp"})
public class McpConfig {
}
