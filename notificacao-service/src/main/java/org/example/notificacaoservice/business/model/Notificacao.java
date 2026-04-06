package org.example.notificacaoservice.business.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
public record Notificacao(
        String numeroConta,
        String mensagem

) {
}
