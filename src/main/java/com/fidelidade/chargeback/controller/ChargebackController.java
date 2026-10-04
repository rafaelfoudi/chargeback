package com.fidelidade.chargeback.controller;

import com.fidelidade.chargeback.entity.Transacao;
import com.fidelidade.chargeback.repository.TransacaoRepository;
import com.fidelidade.chargeback.service.ChargebackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transacoes")
@CrossOrigin(origins = "*")
@Tag(name = "Transações & Cancelamento de compra", description = "Endpoints para simulação e consulta do fluxo de desfazer as transações")
public class ChargebackController {

    private final ChargebackService service;
    private final TransacaoRepository repository;

    public ChargebackController(ChargebackService service, TransacaoRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @GetMapping
    @Operation(
        summary = "Listar todas as transações",
        description = "Retorna o histórico de transações."
    )
    public List<Transacao> listarTodas() {
        return repository.findAll();
    }

    @PostMapping("/{id}/chargeback")
    @Operation(
        summary = "Simular aviso de cancelamento de compra",
        description = "Recebe o aviso de que o cliente pediu o dinheiro de volta e envia essa informação para processamento automático em segundo plano."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Aviso recebido e enviado para processamento com sucesso."),
        @ApiResponse(responseCode = "404", description = "Compra não encontrada para o código informado.")
    })
    public ResponseEntity<Void> simularChargeback(
            @Parameter(description = "UUID da transação a ser estornada", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
            @PathVariable UUID id) {
        
        service.solicitarChargeback(id);
        return ResponseEntity.accepted().build();
    }
    
    @PostMapping
    @Operation(
        summary = "Criar nova transação",
        description = "Insere uma nova transação no banco com o status inicial COMPRA_REALIZADA."
    )
    public ResponseEntity<Transacao> criarTransacao(@RequestBody Transacao transacao) {
        if (transacao.getId() == null) {
            transacao.setId(UUID.randomUUID());
        }
        if (transacao.getStatus() == null) {
            transacao.setStatus("COMPRA_REALIZADA");
        }
        if (transacao.getCupomUsado() == null) {
            transacao.setCupomUsado(false);
        }
        Transacao novaTransacao = repository.save(transacao);
        return ResponseEntity.ok(novaTransacao);
    }
}