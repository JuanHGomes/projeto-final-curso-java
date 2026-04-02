package com.example.workflow.business.model;

import com.example.workflow.business.enums.TipoTransacao;

import java.time.LocalDateTime;

public class Transacao {
    private String numeroConta;
    private Long valor;
    private TipoTransacao tipoTransacao;
    private LocalDateTime timeStamp;
    private String estabelecimento;
    private boolean aprovada;
}
