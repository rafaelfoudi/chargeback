package com.fidelidade.chargeback.repository;

import com.fidelidade.chargeback.entity.Transacao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, UUID> {

    /**
     * Busca a transação aplicando Lock Pessimista (for update)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transacao t WHERE t.id = :id")
    Optional<Transacao> findByIdForUpdate(@Param("id") UUID id);
}