package org.example.transacaoservice.business.model;

import lombok.Getter;
import org.example.transacaoservice.enums.TipoTransacao;

import java.time.LocalDateTime;

@Getter
public class Transacao {
    private String numeroConta;
    private Long valor;
    private TipoTransacao tipoTransacao;
    private LocalDateTime timeStamp;
    private boolean aprovada;
}
