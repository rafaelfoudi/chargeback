package com.fidelidade.chargeback.consumer;

import com.fidelidade.chargeback.config.RabbitMQConfig;
import com.fidelidade.chargeback.entity.Transacao;
import com.fidelidade.chargeback.repository.TransacaoRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChargebackConsumers {

    private final TransacaoRepository repository;

    private final Set<String> transacoesComFalhaSimulada = ConcurrentHashMap.newKeySet();

    public ChargebackConsumers(TransacaoRepository repository) {
        this.repository = repository;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PONTOS)
    @Transactional
    public void processarEstornoPontos(String transacaoId) {
        Transacao transacao = buscarTransacaoComLock(transacaoId);

        // Garantir que não processe novamente
        if (Boolean.TRUE.equals(transacao.getPontosEstornados())) {
            System.out.println("[SERVIÇO PONTOS] Transação " + transacaoId + " já teve os pontos estornados. Ignorando reprocessamento.");
            return;
        }

        System.out.println("--> [SERVIÇO PONTOS] Estornando " + transacao.getPontos() + " pontos para: " + transacao.getClienteId());

        // Marca como processado
        transacao.setPontosEstornados(true);
        atualizarEstado(transacao, "[Pontos Estornados: " + transacao.getPontos() + "]");
    }

    // MÓDULO DE MILHAS COM SIMULAÇÃO DE ERRO E RETENTATIVA
    @RabbitListener(queues = RabbitMQConfig.QUEUE_MILHAS)
    @Transactional
    public void processarCancelamentoMilhas(String transacaoId) {
        Transacao transacao = buscarTransacaoComLock(transacaoId);

        // Garantir que não processe novamente
        if (Boolean.TRUE.equals(transacao.getMilhasCanceladas())) {
            System.out.println("[SERVIÇO MILHAS] Transação " + transacaoId + " já teve as milhas canceladas. Ignorando reprocessamento.");
            return;
        }

        // Simulação de erro de API apenas na primeira execução
        if (!transacoesComFalhaSimulada.contains(transacaoId)) {
            transacoesComFalhaSimulada.add(transacaoId);
            System.err.println("[SERVIÇO MILHAS] API da Companhia Aérea fora do ar! Lançando exceção para ativar a retentativa");
            throw new RuntimeException("API Parceiro Aéreo indisponível no momento (Simulação de Erro)");
        }

        System.out.println("[SERVIÇO MILHAS - RETENTATIVA] Conexão reestabelecida! Cancelando " + transacao.getMilhas() + " milhas na API da Companhia Aérea...");

        transacoesComFalhaSimulada.remove(transacaoId);

        // Marca como processado
        transacao.setMilhasCanceladas(true);
        atualizarEstado(transacao, "[Milhas Canceladas: " + transacao.getMilhas() + "]");
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_CUPONS)
    @Transactional
    public void processarInvalidacaoCupom(String transacaoId) {
        Transacao transacao = buscarTransacaoComLock(transacaoId);

        // Garantir que não processe novamente
        if (Boolean.TRUE.equals(transacao.getCupomInvalidado())) {
            System.out.println("[SERVIÇO CUPONS] Transação " + transacaoId + " já teve o cupom invalidado. Ignorando reprocessamento.");
            return;
        }

        String etapa;
        if (Boolean.TRUE.equals(transacao.getCupomUsado())) {
            System.out.println("--> [SERVIÇO CUPONS] Cupom " + transacao.getCupomCodigo() + " já utilizado! Gerando débito...");
            etapa = "[Cupom Usado - Lançado Débito R$ 10]";
        } else {
            System.out.println("--> [SERVIÇO CUPONS] Invalidando cupom " + transacao.getCupomCodigo());
            etapa = "[Cupom Invalidado: " + transacao.getCupomCodigo() + "]";
        }

        // Marca como processado
        transacao.setCupomInvalidado(true);
        atualizarEstado(transacao, etapa);
    }

    private Transacao buscarTransacaoComLock(String transacaoId) {
        return repository.findByIdForUpdate(UUID.fromString(transacaoId))
                .orElseThrow(() -> new RuntimeException("Transação não encontrada: " + transacaoId));
    }

    private void atualizarEstado(Transacao transacao, String novaEtapa) {
        String obsAtual = transacao.getObservacao() != null ? transacao.getObservacao() : "";
        if (!obsAtual.contains(novaEtapa)) {
            transacao.setObservacao((obsAtual + " " + novaEtapa).trim());
        }

        boolean todosConcluidos = Boolean.TRUE.equals(transacao.getPontosEstornados()) &&
                                  Boolean.TRUE.equals(transacao.getMilhasCanceladas()) &&
                                  Boolean.TRUE.equals(transacao.getCupomInvalidado());

        if (todosConcluidos) {
            transacao.setStatus("CHARGEBACK_CONCLUIDO");
        }

        repository.save(transacao);
    }
}