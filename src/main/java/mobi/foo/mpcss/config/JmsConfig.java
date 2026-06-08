package mobi.foo.mpcss.config;

import jakarta.jms.ConnectionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

/**
 * JMS Configuration for ActiveMQ Artemis.
 * Configures listener container factories and JMS templates
 * for consuming messages from PS-mpClear queues (Section 10).
 */
@Configuration
@EnableJms
public class JmsConfig {

    /**
     * Listener container factory for non-binary (XML) messages.
     * Used for payment, registration, heartbeat, enquiry queues.
     */
    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrency("1-5");
        factory.setSessionTransacted(true);
        // CLIENT_ACKNOWLEDGE for manual ack after processing
        factory.setSessionAcknowledgeMode(jakarta.jms.Session.CLIENT_ACKNOWLEDGE);
        factory.setErrorHandler(t ->
                org.slf4j.LoggerFactory.getLogger(JmsConfig.class)
                        .error("JMS listener error: {}", t.getMessage(), t));
        return factory;
    }

    /**
     * Separate factory for binary messages (reports, bulk registration).
     * These messages contain compressed (ZIP) file content.
     */
    @Bean
    @Qualifier("binaryJmsListenerContainerFactory")
    public DefaultJmsListenerContainerFactory binaryJmsListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrency("1-2");
        factory.setSessionTransacted(true);
        factory.setSessionAcknowledgeMode(jakarta.jms.Session.CLIENT_ACKNOWLEDGE);
        return factory;
    }

    /**
     * JMS Template for sending outward messages (responses/replies) to PS-mpClear.
     */
    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setDeliveryPersistent(true);
        template.setSessionTransacted(true);
        return template;
    }
}

