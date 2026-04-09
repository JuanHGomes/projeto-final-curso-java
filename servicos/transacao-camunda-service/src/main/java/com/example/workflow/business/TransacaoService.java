package com.example.workflow.business;

import com.example.workflow.business.model.AtualizacaoHistorico;
import com.example.workflow.business.model.Notificacao;
import com.example.workflow.business.model.Transacao;
import com.example.workflow.data.TransacaoRepository;
import com.example.workflow.messaging.TransacaoProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;

@Slf4j
@RequiredArgsConstructor
@Service
public class TransacaoService {
    private static final String TOPICO_NOTIFICACAO = "notificacaoProducer-out-0";

    private final TransacaoRepository transacaoRepository;
    private final TransacaoProducer kafkaProducer;

    public Transacao validarFundos(Transacao transacao) {
       return transacaoRepository.validarFundos(transacao);
    }

    public Transacao validarFraude(Transacao transacao) {
        return transacaoRepository.validarFraude(transacao);
    }

    public Transacao executarTransacao(Transacao transacao) {
        return transacaoRepository.executarTransacao(transacao);
    }

    public void estornarTransacao(Transacao transacao) {
        transacaoRepository.estornarTransacao(transacao);
    }

    public void enviarNotificacao(Transacao transacao){
        AtualizacaoHistorico ultimaAtualizacaoHistorico = getUltimaAtualizacaoHistorico((LinkedHashMap<String, Boolean>)transacao.getHistorico());
        String numeroConta = transacao.getNumeroConta();
        String etapa = ultimaAtualizacaoHistorico.etapa();

        Notificacao notificacao = buildNotificacao(etapa, numeroConta, transacao);

        log.info("Enviando notificaocao: {}", notificacao.toString());
        kafkaProducer.sendMessage(TOPICO_NOTIFICACAO, notificacao);
    }

    private Notificacao buildNotificacao(String etapa, String numeroConta, Transacao transacao) {
       StringBuilder mensagem = new StringBuilder();

       String estabelecimento = transacao.getEstabelecimento() != null ? transacao.getEstabelecimento() : "";
       String contexto = !estabelecimento.isEmpty() ? " em " + estabelecimento : "";

       switch (etapa){
           case "FUNDOS_SUFICIENTES" -> mensagem.append("Pagamento").append(contexto).append(" recusado: fundos insuficientes.");
           case "FRAUDE" -> mensagem.append("Pagamento").append(contexto).append(" bloqueado: possível fraude detectada.");
           case "EXECUCAO_SUCESSO" -> mensagem.append("Transação").append(contexto).append(" concluída com sucesso!");
           default -> throw  new RuntimeException("Etapa não encontrada");

       }

       log.info("Notificação gerada com sucesso: {}", mensagem.toString());

       return new Notificacao(numeroConta, mensagem.toString());
    }

    private AtualizacaoHistorico getUltimaAtualizacaoHistorico(LinkedHashMap<String, Boolean> historico) {
        var ultimaAtualizacao = historico.lastEntry();

        return new AtualizacaoHistorico(ultimaAtualizacao.getKey(), ultimaAtualizacao.getValue());
    }

    public boolean enviarTransacaoParaRegistro(Transacao transacao) {
        return transacaoRepository.enviarTransacaoParaRegistro(transacao);
    }
}
