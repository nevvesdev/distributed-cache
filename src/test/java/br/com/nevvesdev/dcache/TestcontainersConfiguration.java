package br.com.nevvesdev.dcache;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration(proxyBeanMethods = false)
@Import({PostgresContainerConfiguration.class, RedisContainerConfiguration.class})
public class TestcontainersConfiguration {
}
