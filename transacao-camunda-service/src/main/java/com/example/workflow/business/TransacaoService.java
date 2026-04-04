package com.example.workflow.business;

import com.example.workflow.business.model.Transacao;
import com.example.workflow.data.TransacaoRepository;
import org.springframework.stereotype.Service;

@Service
public class TransacaoService {
    private final TransacaoRepository transacaoRepository;

    public TransacaoService(TransacaoRepository transacaoRepository) {
        this.transacaoRepository = transacaoRepository;
    }

    public Transacao validarFundos(Transacao transacao) {
       return transacaoRepository.validarFundos(transacao);
    }
}
