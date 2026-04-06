package org.example.registrotransacaoservice.business;

import lombok.RequiredArgsConstructor;
import org.example.registrotransacaoservice.business.model.Transacao;
import org.example.registrotransacaoservice.data.RegistroTransacaoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class RegistroTransacaoService {
    private final RegistroTransacaoRepository registroTransacaoRepository;

    public void garantirRegistroTransacao(Transacao transacao) {
        registroTransacaoRepository.registrarSeNaoPresente(transacao);
    }

    public List<Transacao> getExtratoUltimosTrintaDias(String numeroConta) {
      return registroTransacaoRepository.findAllOverLastThirtyDaysByNumeroConta(numeroConta);
    }

//    public byte[] getExtratoPdf(String numeroConta) {
//
//    }
}
