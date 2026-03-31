package org.example.notificacaoservice.business.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacao {
    private String numeroConta;
    private String mensagem;

    public String getNumeroConta() {
        return numeroConta;
    }

    public String getMensagem() {
        return mensagem;
    }
}
