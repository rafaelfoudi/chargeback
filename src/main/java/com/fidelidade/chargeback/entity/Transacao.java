package com.fidelidade.chargeback.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "transacao")
public class Transacao {

    @Id
    private UUID id;
    private String clienteId;
    private Integer pontos;
    private BigDecimal valorReais;
    private Integer milhas;
    private String cupomCodigo;
    private Boolean cupomUsado;
    private String status;
    private String observacao;
    
    @Column(name = "pontos_estornados")
    private Boolean pontosEstornados = false;

    @Column(name = "milhas_canceladas")
    private Boolean milhasCanceladas = false;

    @Column(name = "cupom_invalidado")
    private Boolean cupomInvalidado = false;

    // Getters e Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }
    public Integer getPontos() { return pontos; }
    public void setPontos(Integer pontos) { this.pontos = pontos; }
    public BigDecimal getValorReais() { return valorReais; }
    public void setValorReais(BigDecimal valorReais) { this.valorReais = valorReais; }
    public Integer getMilhas() { return milhas; }
    public void setMilhas(Integer milhas) { this.milhas = milhas; }
    public String getCupomCodigo() { return cupomCodigo; }
    public void setCupomCodigo(String cupomCodigo) { this.cupomCodigo = cupomCodigo; }
    public Boolean getCupomUsado() { return cupomUsado; }
    public void setCupomUsado(Boolean cupomUsado) { this.cupomUsado = cupomUsado; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
	public Boolean getPontosEstornados() {
		return pontosEstornados;
	}
	public void setPontosEstornados(Boolean pontosEstornados) {
		this.pontosEstornados = pontosEstornados;
	}
	public Boolean getMilhasCanceladas() {
		return milhasCanceladas;
	}
	public void setMilhasCanceladas(Boolean milhasCanceladas) {
		this.milhasCanceladas = milhasCanceladas;
	}
	public Boolean getCupomInvalidado() {
		return cupomInvalidado;
	}
	public void setCupomInvalidado(Boolean cupomInvalidado) {
		this.cupomInvalidado = cupomInvalidado;
	}
    
}