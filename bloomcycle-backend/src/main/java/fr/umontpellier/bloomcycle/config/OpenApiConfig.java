package fr.umontpellier.bloomcycle.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI myOpenAPI() {
        Server devServer = new Server();
        devServer.setUrl("http://localhost:9090");
        devServer.setDescription("Development server");

        Contact contact = new Contact();
        contact.setName("BloomCycle Team");
        contact.setEmail("contact@bloomcycle.fr");

        Info info = new Info()
                .title("BloomCycle API")
                .version("1.0")
                .contact(contact)
                .description("This API exposes endpoints for BloomCycle project management.");

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer));
    }
} 