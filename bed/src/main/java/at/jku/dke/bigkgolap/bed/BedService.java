package at.jku.dke.bigkgolap.bed;

import at.jku.dke.bigkgolap.shared.config.LakehouseConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

@Slf4j
@SpringBootApplication
@EnableAsync
@Import(LakehouseConfig.class)
public class BedService {

    public static void main(String[] args) {
        SpringApplication.run(BedService.class, args);
    }
}
