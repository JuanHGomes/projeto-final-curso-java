package com.example.workflow.business;

import com.example.workflow.business.model.AtualizacaoHistorico;
import com.example.workflow.business.model.Notificacao;
import com.example.workflow.business.model.Transacao;
import com.example.workflow.data.TransacaoRepository;
import com.example.workflow.messaging.KafkaProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;

@RequiredArgsConstructor
@Service
public class TransacaoService {
    private static final String BASE_NOTIFICACAO_TRANSACAO_NAO_CONCLUIDA = "Transação não concluída! Veja abaixo o motivo\n";
    private static final String TOPICO_NOTIFICACAO = "notificacaoProducer-out-0";

    private final TransacaoRepository transacaoRepository;
    private final KafkaProducer kafkaProducer;

    public Transacao validarFundos(Transacao transacao) {
       return transacaoRepository.validarFundos(transacao);
    }

    public Transacao validarFraude(Transacao transacao) {
        return transacaoRepository.validarFraude(transacao);
    }

    public Transacao executarTransacao(Transacao transacao) {
        return transacaoRepository.executarTransacao(transacao);
    }

    public void enviarNotificacao(Transacao transacao){
        AtualizacaoHistorico ultimaAtualizacaoHistorico = getUltimaAtualizacaoHistorico((LinkedHashMap<String, Boolean>)transacao.getHistorico());
        String numeroConta = transacao.getNumeroConta();
        String etapa = ultimaAtualizacaoHistorico.etapa();
        boolean resultado = ultimaAtualizacaoHistorico.resultado();

        Notificacao notificacao = buildNotificacao(etapa, resultado, numeroConta);

        kafkaProducer.sendMessage(TOPICO_NOTIFICACAO, notificacao);
    }

    private Notificacao buildNotificacao(String etapa, boolean resultado, String numeroConta) {
       StringBuilder mensagem = new StringBuilder();

       switch (etapa){
           case "VALIDAR_FUNDOS" -> mensagem.append(BASE_NOTIFICACAO_TRANSACAO_NAO_CONCLUIDA + "Fundos insuficientes!");
           case "FRAUDE" -> mensagem.append(BASE_NOTIFICACAO_TRANSACAO_NAO_CONCLUIDA + "Possível fraude detectada");
           case "EXECUCAO_SUCESSO" -> mensagem.append("Transação concluída!");
           default -> throw  new RuntimeException("Etapa não encontrada");

       }

       return new Notificacao(numeroConta, mensagem.toString());
    }

    private AtualizacaoHistorico getUltimaAtualizacaoHistorico(LinkedHashMap<String, Boolean> historico) {
        var ultimaAtualizacao = historico.lastEntry();

        return new AtualizacaoHistorico(ultimaAtualizacao.getKey(), ultimaAtualizacao.getValue());
    }
}
