package com.fidelidade.chargeback.service;

import com.fidelidade.chargeback.config.RabbitMQConfig;
import com.fidelidade.chargeback.entity.Transacao;
import com.fidelidade.chargeback.repository.TransacaoRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ChargebackService {

    private final TransacaoRepository repository;
    private final RabbitTemplate rabbitTemplate;

    public ChargebackService(TransacaoRepository repository, RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public void solicitarChargeback(UUID transacaoId) {
        Transacao transacao = repository.findById(transacaoId)
                .orElseThrow(() -> new RuntimeException("Transação não encontrada"));

        transacao.setStatus("CHARGEBACK_PROCESSANDO");
        repository.save(transacao);

        // Envia uma única vez para o Fanout Exchange (entrega para TODAS as filas)
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_CHARGEBACK, "", transacaoId.toString());
    }
}