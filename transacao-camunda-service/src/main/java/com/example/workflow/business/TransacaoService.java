package com.example.workflow.business;

import com.example.workflow.business.model.Transacao;
import com.example.workflow.data.TransacaoRepository;

public class TransacaoService {
    public TransacaoService(TransacaoRepository transacaoRepository) {
        this.transacaoRepository = transacaoRepository;
    }

    private final TransacaoRepository transacaoRepository;

    public void validarFundos(Transacao transacao) {
        transacaoRepository.validarFundos(transacao);

    }
}
