package org.example.transacaoservice.business.validators;

import org.example.transacaoservice.business.transacao.model.Transacao;

public interface FraudeValidators {
    boolean validate(Transacao transacao);
}
