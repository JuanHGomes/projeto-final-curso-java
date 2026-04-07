package com.example.workflow.api.model;

import com.example.workflow.business.enums.TipoTransacao;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;

public record TransacaoRequest(
         String numeroConta,
         Long valor,
         TipoTransacao tipoTransacao,
         LocalDateTime timeStamp,
         String estabelecimento,
         LinkedHashMap<String, Boolean>historico
) {
}
