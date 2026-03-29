package org.example.transacaoservice.business.validators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.transacaoservice.business.transacao.model.Transacao;
import org.example.transacaoservice.data.transacao.TransacaoRepository;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class TransacaoRepeditaValidator implements FraudeValidators {
    private final TransacaoRepository transacaoRepository;

    @Override
    public boolean validate(Transacao novaTransacao) {

        String numeroConta = novaTransacao.getNumeroConta();

        boolean isFraude = transacaoRepository.getTransacaoByNumeroConta(numeroConta)
                .filter(transacaoAnterior -> isValorEhEstabelecimentoIdenticos(transacaoAnterior, novaTransacao))
                .isPresent();

        if(isFraude){
            log.warn("Tentativa de transacao identica a transação anterior.");
            return true;
        }

        return false;
    }

    private boolean isValorEhEstabelecimentoIdenticos(Transacao transacaoAnterior, Transacao novaTransacao) {

        Long valorTransacaoAnterior = transacaoAnterior.getValor();
        Long valorNovaTransacao = novaTransacao.getValor();

        String estabelecimentoTransacaoAnterior = transacaoAnterior.getEstabelecimento();
        String estabelecimentoNovaTransacao = novaTransacao.getEstabelecimento();

        if(valorTransacaoAnterior.equals(valorNovaTransacao) && estabelecimentoTransacaoAnterior.equals(estabelecimentoNovaTransacao)){
            return true;
        }

        return false;
    }
}
