package org.example.transacaoservice.business.transacao.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.transacaoservice.enums.TipoTransacao;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Transacao {
    private String numeroConta;
    private Long valor;
    private TipoTransacao tipoTransacao;
    private LocalDateTime timeStamp;
    private String estabelecimento;

    private LinkedHashMap<String, Boolean> historico;

    @Override
    public String toString() {
        return "Transacao{" +
                "numeroConta='" + numeroConta + '\'' +
                ", valor=" + valor +
                ", tipoTransacao=" + tipoTransacao +
                ", timeStamp=" + timeStamp +
                ", estabelecimento='" + estabelecimento + '\'' +
                ", historico=" + historico +
                '}';
    }
}
