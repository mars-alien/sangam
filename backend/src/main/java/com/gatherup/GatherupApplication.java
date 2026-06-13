package com.gatherup;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@OpenAPIDefinition(
    info = @Info(
        title       = "Sangam API",
        version     = "1.0.0",
        description = "Sangam is a social meetup platform — find companions for events you're already attending. "
                    + "Supports geospatial discovery, join-request workflows with waitlist auto-promotion, "
                    + "and JWT-secured authentication with refresh-token rotation.",
        contact     = @Contact(name = "mars-alien", url = "https://github.com/mars-alien/sangam")
    )
)
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class GatherupApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatherupApplication.class, args);
    }
}
