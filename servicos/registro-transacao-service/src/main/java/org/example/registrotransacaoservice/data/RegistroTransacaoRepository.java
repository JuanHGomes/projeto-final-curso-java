package org.example.registrotransacaoservice.data;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.registrotransacaoservice.business.model.Transacao;
import org.example.registrotransacaoservice.data.model.TransacaoDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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

    public Page<Transacao> findAllOverLastThirtyDaysByNumeroConta(String numeroConta, Pageable pageable) {
        LocalDateTime hoje = LocalDate.now().atTime(23, 59);
        LocalDateTime trintaDiasAtras = hoje.minusDays(30);
        return dao.findByNumeroContaAndTimeStampBetween(numeroConta, trintaDiasAtras, hoje, pageable)
                .map(mapper::toTransacao);
    }

    public List<Transacao> findAllOverLastThirtyDaysByNumeroConta(String numeroConta) {
        LocalDateTime hoje = LocalDate.now().atTime(23, 59);
        LocalDateTime trintaDiasAtras = hoje.minusDays(30);
        return dao.findByNumeroContaAndTimeStampBetween(numeroConta, trintaDiasAtras, hoje)
                .stream()
                .map(mapper::toTransacao)
                .toList();
    }
}
