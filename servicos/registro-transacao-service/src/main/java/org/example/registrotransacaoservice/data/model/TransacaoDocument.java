package org.example.registrotransacaoservice.data.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.registrotransacaoservice.business.model.Transacao;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Document(collection = "transacoes")
public class TransacaoDocument {
    @Id
    private String id;
    private String numeroConta;
    private Long valor;
    private Transacao.TipoTransacao tipoTransacao;
    private LocalDateTime timeStamp;
    private String estabelecimento;
    private LinkedHashMap<String, Boolean> historico;

    public Map<String, Boolean> getHistorico() {
        return historico;
    }

    public enum TipoTransacao{
        DEBITO,
        CREDITO
    }
}
