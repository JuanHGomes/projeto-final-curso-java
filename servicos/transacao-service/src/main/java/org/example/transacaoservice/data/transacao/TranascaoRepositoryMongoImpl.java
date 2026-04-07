package org.example.transacaoservice.data.transacao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.transacaoservice.business.transacao.model.Transacao;
import org.example.transacaoservice.business.transacao.operators.TransacaoOperators;
import org.example.transacaoservice.data.transacao.model.TransacaoDocument;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Repository
public class TranascaoRepositoryMongoImpl implements TransacaoRepository, TransacaoOperators {
    private final MongoTemplate mongoTemplate;
    private final TransacaoMapper mapper;

    @Override
    public Transacao save(Transacao transacao) {
        TransacaoDocument transacaoDocument = mapper.toDocument(transacao);
        return mapper.toTransacao(mongoTemplate.save(transacaoDocument));
    }

    @Override
    public Optional<LocalDateTime> getTimeStampByNumeroConta(String numeroConta) {
        return Optional.empty();
    }

    @Override
    public Optional<Transacao> getTransacaoByNumeroConta(String numeroConta) {
        return Optional.empty();
    }

    @Override
    public boolean updateSaldo(Transacao transacao) {
        log.info("Iniciando save da tansação no mongo");
        return save(transacao) != null;
    }

    @Override
    public boolean updateLimiteCredito(Transacao transacao) {
        log.info("Iniciando save da tansação no mongo");
        return save(transacao) != null;
    }

    @Override
    public void confirmarTransacao(Transacao transacao) {
        log.info("Confirmação de transação no Mongo (No-op)");
    }

    @Override
    public void estornarTransacao(Transacao transacao) {
        log.info("Estorno de transação no Mongo (No-op)");
    }
}
