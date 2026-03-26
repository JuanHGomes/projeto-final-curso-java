package org.example.transacaoservice.business.validators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.transacaoservice.business.model.Transacao;
import org.example.transacaoservice.data.transacao.TransacaoRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class TempoEntreTransacoesValidator implements FraudeValidators {
    private static final Duration INTERVALO_MINITMO = Duration.ofMinutes(1);

    private final TransacaoRepository transacaoRepository;

    @Override
    public boolean validate(Transacao novaTransacao) {
        String numeroConta = novaTransacao.getNumeroConta();
        boolean fraudeDetectada = findTransacaoByNumeroConta(numeroConta)
                .filter(transacao -> isIntevaloEntreTransacoesMenorQueMinimo(transacao.getTimeStamp(), novaTransacao.getTimeStamp()))
                .isPresent();

        return !fraudeDetectada;
    }

    private boolean isIntevaloEntreTransacoesMenorQueMinimo(LocalDateTime timeStampTransacaoEmMemoria, LocalDateTime timeStampNovaTransacao) {
        return Duration.between(timeStampTransacaoEmMemoria, timeStampNovaTransacao).compareTo(INTERVALO_MINITMO) <= 0;
    }

    private Optional<Transacao> findTransacaoByNumeroConta(String numeroConta) {
        return transacaoRepository.findByNumeroConta(numeroConta);
    }
}
