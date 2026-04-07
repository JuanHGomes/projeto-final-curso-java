package org.example.transacaoservice.data.transacao;

import org.example.transacaoservice.business.transacao.model.Transacao;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TransacaoRepository {
    public Transacao save(Transacao transacao);
    public Optional<LocalDateTime> getTimeStampByNumeroConta(String numeroConta);
    public Optional<Transacao> getTransacaoByNumeroConta(String numeroConta);
}
