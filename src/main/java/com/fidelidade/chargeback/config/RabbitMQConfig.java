package com.fidelidade.chargeback.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_CHARGEBACK = "chargeback.fanout.exchange";

    public static final String QUEUE_PONTOS = "q.chargeback.pontos";
    public static final String QUEUE_MILHAS = "q.chargeback.milhas";
    public static final String QUEUE_CUPONS = "q.chargeback.cupons";
    public static final String QUEUE_DLQ = "q.chargeback.dlq";

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public ApplicationListener<ApplicationReadyEvent> initializeAdmin(RabbitAdmin rabbitAdmin) {
        return event -> rabbitAdmin.initialize();
    }

    @Bean
    public FanoutExchange chargebackExchange() {
        return new FanoutExchange(EXCHANGE_CHARGEBACK);
    }

    @Bean
    public Queue queueDlq() {
        return QueueBuilder.durable(QUEUE_DLQ).build();
    }

    @Bean
    public Queue queuePontos() {
        return QueueBuilder.durable(QUEUE_PONTOS)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_DLQ)
                .build();
    }

    @Bean
    public Queue queueMilhas() {
        return QueueBuilder.durable(QUEUE_MILHAS)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_DLQ)
                .build();
    }

    @Bean
    public Queue queueCupons() {
        return QueueBuilder.durable(QUEUE_CUPONS)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_DLQ)
                .build();
    }

    // 2. Garanta que os métodos bind recebam a interface genérica 'Exchange' para evitar erros de injeção
    @Bean
    public Binding bindPontos(Queue queuePontos, FanoutExchange chargebackExchange) {
        return BindingBuilder.bind(queuePontos).to(chargebackExchange);
    }

    @Bean
    public Binding bindMilhas(Queue queueMilhas, FanoutExchange chargebackExchange) {
        return BindingBuilder.bind(queueMilhas).to(chargebackExchange);
    }

    @Bean
    public Binding bindCupons(Queue queueCupons, FanoutExchange chargebackExchange) {
        return BindingBuilder.bind(queueCupons).to(chargebackExchange);
    }
}