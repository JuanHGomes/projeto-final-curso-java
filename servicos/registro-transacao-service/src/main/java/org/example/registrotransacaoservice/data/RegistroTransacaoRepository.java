package org.example.registrotransacaoservice.data;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.registrotransacaoservice.business.model.Transacao;
import org.example.registrotransacaoservice.data.model.TransacaoDocument;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Repository
public class RegistroTransacaoRepository {
    private final RegistroTransacaoDao dao;
    private final RegistroTransacaoMapper mapper;


    public Transacao registrarSeNaoPresente(Transacao transacao) {
        String numeroConta = transacao.getNumeroConta();
        LocalDateTime timestamp = transacao.getTimeStamp();
        return getTransacaoByNumeroContaEhTimeStamp(numeroConta, timestamp)
                .orElseGet(() -> {
                    log.info("Transacao não foi localizada, garantindo registro da transacao: {}", transacao.toString());
                    TransacaoDocument transacaoAhSerSalva = mapper.toDocument(transacao);
                    return mapper.toTransacao(dao.save(transacaoAhSerSalva));
                });
    }

    private Optional<Transacao> getTransacaoByNumeroContaEhTimeStamp(String numeroConta, LocalDateTime timestamp) {
      return dao.findTransacaoDocumentByNumeroContaAndTimeStamp(numeroConta, timestamp)
              .map(document -> {
                  log.info("Transacao localizada, foi registrada corretamente");
                  return mapper.toTransacao(document);
              });
    }
}
