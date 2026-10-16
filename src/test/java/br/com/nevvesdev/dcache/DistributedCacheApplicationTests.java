package br.com.nevvesdev.dcache;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DistributedCacheApplicationTests {

    @Test
    void contextLoads() {
    }
}
