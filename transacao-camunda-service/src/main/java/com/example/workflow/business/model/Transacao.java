package com.example.workflow.business.model;

import com.example.workflow.business.enums.TipoTransacao;

import java.time.LocalDateTime;
import java.util.Map;

public class Transacao {
    private String numeroConta;
    private Long valor;
    private TipoTransacao tipoTransacao;
    private LocalDateTime timeStamp;
    private String estabelecimento;
    private Map<String, Boolean> historico;

    public Map<String, Boolean> getHistorico() {
        return historico;
    }
}
