package om.gov.cbo.mpcss;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.artemis.mode=embedded",
        "mpcss.signature.enabled=false"
})
class MpcssConsumerServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}

