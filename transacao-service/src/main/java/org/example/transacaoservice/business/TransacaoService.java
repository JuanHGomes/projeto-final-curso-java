package org.example.transacaoservice.business;

import lombok.RequiredArgsConstructor;
import org.example.transacaoservice.business.model.Transacao;
import org.example.transacaoservice.data.conta.ContaRepository;
import org.example.transacaoservice.data.transacao.TransacaoRepository;
import org.example.transacaoservice.enums.TipoTransacao;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class TransacaoService {
    private final TransacaoRepository transacaoRepository;
    private final ContaRepository contaRepository;


    public boolean validarFundos(Transacao transacao) throws Exception {
        switch (transacao.getTipoTransacao()){
            case TipoTransacao.CREDITO -> {return validarLimiteCredito(transacao);}
            case TipoTransacao.DEBITO -> {return validarSaldo(transacao);}
            default -> throw new Exception("Tipo de transação inválida");
        }
    }

    private boolean validarSaldo(Transacao transacao) {
        Long saldo = contaRepository.getSaldoByNumeroConta(transacao.getNumeroConta());
        return saldo < transacao.getValor();
    }

    private boolean validarLimiteCredito(Transacao transacao) {
        return false;
    }
}
