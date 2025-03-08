package fr.umontpellier.bloomcycle.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiConfiguration() {
        var devServer = new Server()
            .url("http://localhost:9090")
            .description("Development server");

        var prodServer = new Server()
            .url("https://api-bloomcycle.axelfrache.me")
            .description("Production server");

        var info = new Info()
            .title("BloomCycle API")
            .version("1.0")
            .description("This API exposes endpoints for BloomCycle project management.");

        return new OpenAPI()
            .info(info)
            .servers(List.of(prodServer, devServer));
    }
}