package at.jku.dke.bigkgolap.surface;

import at.jku.dke.bigkgolap.shared.config.LakehouseConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Slf4j
@SpringBootApplication
@Import(LakehouseConfig.class)
public class SurfaceService {

    public static void main(String[] args) {
        SpringApplication.run(SurfaceService.class, args);
    }
}
