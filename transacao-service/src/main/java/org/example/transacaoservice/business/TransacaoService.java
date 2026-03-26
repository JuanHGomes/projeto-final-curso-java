package org.example.transacaoservice.business;

import lombok.RequiredArgsConstructor;
import org.example.transacaoservice.business.model.Transacao;
import org.example.transacaoservice.business.validators.FraudeValidators;
import org.example.transacaoservice.data.conta.ContaRepository;
import org.example.transacaoservice.enums.TipoTransacao;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class TransacaoService {
    private final ContaRepository contaRepository;
    private final List<FraudeValidators> fraudeValidatorsList;


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
}
