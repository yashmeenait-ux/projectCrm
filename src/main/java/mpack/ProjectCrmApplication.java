package mpack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(
    scanBasePackages = {
        "controllers",
        "services",
        "repositories",
        "entities",
        "mpack"
    }
)
@EnableJpaRepositories(basePackages = "repositories") // Tells Spring where to look for interfaces extending JpaRepository
@EntityScan(basePackages = "entities")                 // Tells Spring where to look for @Entity annotations
public class ProjectCrmApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjectCrmApplication.class, args);
    }
}