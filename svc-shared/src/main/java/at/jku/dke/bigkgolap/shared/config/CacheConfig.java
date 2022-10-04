package at.jku.dke.bigkgolap.shared.config;

import at.jku.dke.bigkgolap.shared.cache.GraphCache;
import at.jku.dke.bigkgolap.shared.cache.NoCache;
import at.jku.dke.bigkgolap.shared.cache.RedisCache;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class CacheConfig {

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public GraphCache graphCache(LakehouseConfig config) {
        if (config.isUseRedisCache()) {
            return new RedisCache(config);
        }
        return new NoCache();
    }
}
