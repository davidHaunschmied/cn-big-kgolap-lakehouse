package at.jku.dke.bigkgolap.shared.config;

import at.jku.dke.bigkgolap.api.engines.Engine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;

@Configuration
@Slf4j
public class EngineConfig {

    @Bean
    public Map<String, Engine> engines() {
        ServiceLoader<Engine> loadedEngines = ServiceLoader.load(Engine.class);

        Map<String, Engine> engines = new HashMap<>();
        for (Engine loadedEngine : loadedEngines) {
            engines.put(loadedEngine.getUniqueId().toUpperCase(Locale.ROOT), loadedEngine);
            log.info("Successfully loaded engine {} [UID: {}]!", loadedEngine.getClass().getSimpleName(), loadedEngine.getUniqueId());
        }

        if (engines.size() < 1) {
            throw new IllegalStateException("No engine provided. Stopping service!");
        }

        return Map.copyOf(engines);
    }

}
