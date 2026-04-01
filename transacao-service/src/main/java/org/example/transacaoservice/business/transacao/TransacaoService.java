package org.example.transacaoservice.business.transacao;

import lombok.RequiredArgsConstructor;
import org.example.transacaoservice.business.transacao.executors.TransacaoOperators;
import org.example.transacaoservice.business.transacao.model.Transacao;
import org.example.transacaoservice.business.validators.FraudeValidators;
import org.example.transacaoservice.data.conta.ContaRepository;
import org.example.transacaoservice.data.transacao.TransacaoRepository;
import org.example.transacaoservice.enums.TipoTransacao;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class TransacaoService {
    @Qualifier("contaRepositoryJpaImpl")
    private final ContaRepository contaRepository;
    private final TransacaoRepository transacaoRepository;
    private final List<FraudeValidators> fraudeValidatorsList;
    private final List<TransacaoOperators> transacaoOperatorsList;

    public boolean validarFundos(Transacao transacao) throws Exception {
        switch (transacao.getTipoTransacao()){
            case TipoTransacao.CREDITO -> {return validarLimiteCredito(transacao);}
            case TipoTransacao.DEBITO -> {return validarSaldo(transacao);}
            default -> throw new Exception("Tipo de transação inválida");
        }
    }

    private boolean validarSaldo(Transacao transacao) {
        Long saldo = contaRepository.getSaldoByNumeroConta(transacao.getNumeroConta());
        return saldo >= transacao.getValor();
    }

    private boolean validarLimiteCredito(Transacao transacao) {
        Long limiteCredito = contaRepository.getLimiteCreditoByNumeroConta(transacao.getNumeroConta());
        return limiteCredito >= transacao.getValor();
    }

    public boolean validarFraude(Transacao transacao){
       return fraudeValidatorsList.stream().noneMatch(
               fraudeValidator -> fraudeValidator.validate(transacao));
    }

    public boolean executarTransacao(Transacao transacao) throws Exception {
        switch (transacao.getTipoTransacao()){
            case TipoTransacao.CREDITO -> {return executarTransacaoCredito(transacao);}
            case TipoTransacao.DEBITO -> {return executarTransacaoDebito(transacao);}
            default -> throw new Exception("Tipo de transação inválida");
        }
    }

    private boolean executarTransacaoDebito(Transacao transacao) {
        Long valor = transacao.getValor();
        transacaoOperatorsList.forEach();
        return false;
    }

    private boolean executarTransacaoCredito(Transacao transacao) {
        return false;
    }
}
