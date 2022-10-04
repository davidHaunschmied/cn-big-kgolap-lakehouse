package at.jku.dke.bigkgolap.shared.config;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.internal.core.metadata.DefaultEndPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.aws.mcs.auth.SigV4AuthProvider;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@Profile("aws")
public class AwsConfig {

    @Value("${lakehouse.cassandra.aws-region}")
    private String cassAwsRegion;

    @Value("${spring.data.cassandra.port}")
    private int cassPort;

    @Value("${spring.data.cassandra.contact-points}")
    private List<String> cassContactPoints;

    @Value("${spring.data.cassandra.local-datacenter}")
    private String cassDataCenter;

    @Bean
    public CqlSession cqlSession() throws NoSuchAlgorithmException {
        return CqlSession.builder()
                .addContactEndPoints(cassContactPoints.stream().map(s ->
                                new DefaultEndPoint(InetSocketAddress.createUnresolved(s, cassPort)))
                        .collect(Collectors.toSet()))
                .withConfigLoader(DriverConfigLoader.programmaticBuilder().withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofMillis(60 * 1000)).build())
                .withSslContext(SSLContext.getDefault())
                .withLocalDatacenter(cassDataCenter)
                .withAuthProvider(new SigV4AuthProvider(cassAwsRegion))
                .build();
    }
}
