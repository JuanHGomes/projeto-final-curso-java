package org.example.transacaoservice.api.model;

import org.example.transacaoservice.enums.TipoTransacao;

import java.time.LocalDateTime;
import java.util.Map;

public record TransacaoRequest(
        String numeroConta,
        Long valor,
        TipoTransacao tipoTransacao,
        LocalDateTime timeStamp,
        String estabelecimento,
        Map<String, Boolean> historico
) {
}
