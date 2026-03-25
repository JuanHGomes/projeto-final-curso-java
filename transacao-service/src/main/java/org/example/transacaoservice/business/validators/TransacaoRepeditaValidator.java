package org.example.transacaoservice.business.validators;

import org.example.transacaoservice.business.model.Transacao;
import org.springframework.stereotype.Component;

@Component
public class TransacaoRepeditaValidator implements FraudeValidators {
    @Override
    public boolean validate(Transacao transacao) {
        return false;
    }
}
