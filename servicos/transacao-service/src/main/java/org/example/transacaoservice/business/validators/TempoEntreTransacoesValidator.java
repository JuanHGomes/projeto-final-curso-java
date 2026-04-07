package org.example.transacaoservice.business.validators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.transacaoservice.business.transacao.model.Transacao;
import org.example.transacaoservice.data.transacao.TransacaoRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
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
        boolean fraudeDetectada = getTransacaoTimeStampByNumeroConta(numeroConta)
                .filter(timeStamp -> isIntevaloEntreTransacoesMenorOuIgualAhMinimo(timeStamp, novaTransacao.getTimeStamp()))
                .isPresent();

        if(fraudeDetectada){
            log.warn("Tentativa de transação realizada dentro de {} minutos", INTERVALO_MINITMO.toMinutes());
            return true;
        }
        return false;
    }

    private boolean isIntevaloEntreTransacoesMenorOuIgualAhMinimo(LocalDateTime timeStampTransacaoEmMemoria, LocalDateTime timeStampNovaTransacao) {
        return Duration.between(timeStampTransacaoEmMemoria, timeStampNovaTransacao).compareTo(INTERVALO_MINITMO) <= 0;
    }

    private Optional<LocalDateTime> getTransacaoTimeStampByNumeroConta(String numeroConta) {
        return transacaoRepository.getTransacaoByNumeroConta(numeroConta)
                .map(Transacao::getTimeStamp);
    }
}
