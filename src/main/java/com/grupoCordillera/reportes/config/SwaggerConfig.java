package com.grupoCordillera.reportes.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Reportes - Grupo Cordillera")
                        .version("1.0.0")
                        .description("Documentación oficial del microservicio encargado de generar, consolidar y enviar reportes PDF de cumplimiento.")
                        .contact(new Contact()
                                .name("Equipo de Desarrollo TI")
                                .email("soporte@cordillera.cl")));
    }
}