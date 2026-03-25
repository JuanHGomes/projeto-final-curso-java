package org.example.transacaoservice.business.validators;

import lombok.RequiredArgsConstructor;
import org.example.transacaoservice.business.model.Transacao;
import org.example.transacaoservice.data.transacao.TransacaoRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class TempoEntreTransacoesValidator implements FraudeValidators {
    private static final Duration LIMITE_DURACAO_ENTRE_TRANSACAO = Duration.ofMinutes(1);

    private final TransacaoRepository transacaoRepository;

    @Override
    public boolean validate(Transacao transacao) {
        Optional<Transacao> transacaoOptional = transacaoRepository.getTransacaoByAccountNumber(transacao.getNumeroConta());

        transacaoOptional.ifPresent();

        if(Duration.between(transacao.getTimeStamp(), transacaoOptional.get().getTimeStamp()).compareTo(LIMITE_DURACAO_ENTRE_TRANSACAO) >= 0){
            return false;
        }

        if(!transacaoOptional.isEmpty()){
            return false;
        }

    }
}
