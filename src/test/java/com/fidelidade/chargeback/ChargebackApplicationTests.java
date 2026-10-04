package com.fidelidade.chargeback;

import com.fidelidade.chargeback.config.RabbitMQConfig;
import com.fidelidade.chargeback.entity.Transacao;
import com.fidelidade.chargeback.repository.TransacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ChargebackApplicationTests {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TransacaoRepository transacaoRepository;

    private UUID transacaoId;

    @BeforeEach
    void setUp() {
        transacaoRepository.deleteAll();

        Transacao transacao = new Transacao();
        
        // ATRIBUIÇÃO MANUAL DO ID
        transacao.setId(UUID.randomUUID()); 

        transacao.setClienteId("CLIENTE_TESTE_999");
        transacao.setValorReais(new BigDecimal("2500.00"));
        transacao.setPontos(800);
        transacao.setMilhas(2000);
        transacao.setCupomCodigo("PROMO100");
        transacao.setCupomUsado(false);
        transacao.setStatus("CHARGEBACK_SOLICITADO");
        transacao.setPontosEstornados(false);
        transacao.setMilhasCanceladas(false);
        transacao.setCupomInvalidado(false);

        Transacao salva = transacaoRepository.save(transacao);
        this.transacaoId = salva.getId();
    }

    @Test
    @DisplayName("Deve processar todas as etapas em paralelo e alterar status para CHARGEBACK_CONCLUIDO")
    void deveProcessarFluxoCompletoDeChargeback() {
        String idString = transacaoId.toString();

        // 1. Envia mensagens simultaneamente para as 3 filas
        rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_PONTOS, idString);
        rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_MILHAS, idString);
        rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_CUPONS, idString);

        // 2. Aguarda até 10 segundos para o processamento assíncrono terminar
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Transacao transacaoAtualizada = transacaoRepository.findById(transacaoId).orElseThrow();

            // Verifica se todas as flags booleanas foram ativadas
            assertTrue(Boolean.TRUE.equals(transacaoAtualizada.getPontosEstornados()), "Pontos deveriam estar estornados");
            assertTrue(Boolean.TRUE.equals(transacaoAtualizada.getMilhasCanceladas()), "Milhas deveriam estar canceladas");
            assertTrue(Boolean.TRUE.equals(transacaoAtualizada.getCupomInvalidado()), "Cupom deveria estar invalidado");

            // Verifica o status final do agregador
            assertEquals("CHARGEBACK_CONCLUIDO", transacaoAtualizada.getStatus());

            // Garante que todas as etapas foram registradas no histórico textual sem sobrescrita
            String obs = transacaoAtualizada.getObservacao();
            assertNotNull(obs);
            assertTrue(obs.contains("Pontos Estornados"));
            assertTrue(obs.contains("Milhas Canceladas"));
            assertTrue(obs.contains("Cupom Invalidado"));
        });
    }

    @Test
    @DisplayName("Garante a Idempotência ao enviar a mesma mensagem duas vezes para a fila")
    void deveGarantirIdempotenciaAoReprocessarMensagens() {
        String idString = transacaoId.toString();

        // Primeira rodada de disparos
        rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_PONTOS, idString);
        rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_MILHAS, idString);
        rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_CUPONS, idString);

        // Aguarda a conclusão inicial
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Transacao t = transacaoRepository.findById(transacaoId).orElseThrow();
            assertEquals("CHARGEBACK_CONCLUIDO", t.getStatus());
        });

        // Captura o estado da observação
        Transacao estadoPosPrimeiraExecucao = transacaoRepository.findById(transacaoId).orElseThrow();
        String observacaoOriginal = estadoPosPrimeiraExecucao.getObservacao();

        // Dispara mensagens duplicadas (Segunda Rodada)
        rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_PONTOS, idString);
        rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_MILHAS, idString);
        rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_CUPONS, idString);

        // Aguarda um tempo para processamento das duplicatas
        await().pollDelay(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Transacao tFinal = transacaoRepository.findById(transacaoId).orElseThrow();

            // Observação não deve ter partes duplicadas
            assertEquals(observacaoOriginal, tFinal.getObservacao(), "A observação não deveria ser alterada no reprocessamento");
            assertEquals("CHARGEBACK_CONCLUIDO", tFinal.getStatus());
        });
    }
}