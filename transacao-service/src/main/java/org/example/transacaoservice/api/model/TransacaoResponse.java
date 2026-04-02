package org.example.transacaoservice.api.model;

import lombok.Builder;
import org.example.transacaoservice.enums.TipoTransacao;

import java.time.LocalDateTime;
import java.util.Map;

@Builder
public record TransacaoResponse(
        String numeroConta,
        Long valor,
        TipoTransacao tipoTransacao,
        LocalDateTime timeStamp,
        String estabelecimento,
        Map<String, Boolean> historico
) {

}
