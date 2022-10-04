package at.jku.dke.bigkgolap.bgprocessor;

import at.jku.dke.bigkgolap.shared.config.LakehouseConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableScheduling
@Import(LakehouseConfig.class)
public class CirculatorService {

    public static void main(String[] args) {
        SpringApplication.run(CirculatorService.class, args);
    }


}
