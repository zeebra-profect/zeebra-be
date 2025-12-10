package com.zeebra.domain.chat.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit

public class ChatAmqpConfig {

    public static final String CHAT_DB_EXCHANGE = "chat.db.exchange";
    public static final String CHAT_DB_QUEUE = "chat.db.queue";
    public static final String CHAT_DB_ROUTING_KEY = "chat.db.message";

    @Bean
    public TopicExchange chatDbExchange() {
        return new TopicExchange(CHAT_DB_EXCHANGE);
    }

    @Bean Queue chatDbQueue() {
        return new Queue(CHAT_DB_QUEUE, true);
    }

    @Bean
    public Binding chatDbBinding() {
        return BindingBuilder
                .bind(chatDbQueue())
                .to(chatDbExchange())
                .with(CHAT_DB_ROUTING_KEY);
    }

    @Bean
    public MessageConverter chatMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

}
