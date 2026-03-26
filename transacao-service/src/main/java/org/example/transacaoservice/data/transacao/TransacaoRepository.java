package org.example.transacaoservice.data.transacao;

import org.example.transacaoservice.business.model.Transacao;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransacaoRepository {
    public Transacao save(Transacao transacao);
    public Optional<Transacao> findByNumeroConta(String numeroConta);
}
