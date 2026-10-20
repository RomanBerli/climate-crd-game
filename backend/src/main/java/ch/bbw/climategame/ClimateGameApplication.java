package ch.bbw.climategame;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ClimateGameApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClimateGameApplication.class, args);
    }
}
