package om.gov.cbo.mpcss;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.artemis.mode=embedded",
        "mpcss.signature.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "mpcss.core-banking.base-url=http://localhost:9999"
})
class MpcssConsumerServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
