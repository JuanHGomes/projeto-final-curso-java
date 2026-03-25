package org.example.transacaoservice.data.transacao;

import org.example.transacaoservice.business.model.Transacao;
import org.springframework.stereotype.Repository;

@Repository
public interface TransacaoRepository {
    public Transacao save(Transacao transacao);
}
