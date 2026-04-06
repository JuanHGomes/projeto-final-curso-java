package org.example.registrotransacaoservice.business.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transacao {
    private String numeroConta;
    private Long valor;
    private TipoTransacao tipoTransacao;
    private LocalDateTime timeStamp;
    private String estabelecimento;
    private LinkedHashMap<String, Boolean> historico;

    public enum TipoTransacao{
        DEBITO,
        CREDITO
    }
}
