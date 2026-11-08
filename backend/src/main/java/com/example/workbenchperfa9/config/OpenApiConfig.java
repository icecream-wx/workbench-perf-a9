package com.example.workbenchperfa9.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI applicationOpenApi() {
        return new OpenAPI().info(new Info()
                .title("workbench-perf-a9 API")
                .version("v1")
                .description("workbench-perf-a9 application scaffold"));
    }
}
