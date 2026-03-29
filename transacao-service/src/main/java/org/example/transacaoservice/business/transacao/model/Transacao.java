package org.example.transacaoservice.business.transacao.model;

import lombok.Getter;
import lombok.Setter;
import org.example.transacaoservice.enums.TipoTransacao;

import java.time.LocalDateTime;

@Getter
@Setter
public class Transacao {
    private String numeroConta;
    private Long valor;
    private TipoTransacao tipoTransacao;
    private LocalDateTime timeStamp;
    private String estabelecimento;
    private boolean aprovada;
}
