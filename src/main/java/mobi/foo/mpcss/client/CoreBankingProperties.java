package mobi.foo.mpcss.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "mpcss.core-banking")
public class CoreBankingProperties {
    private String baseUrl = "http://localhost:8081/api/v1";
    private int timeoutSeconds = 30;
}

