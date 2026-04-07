package org.example.transacaoservice.business.transacao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.transacaoservice.business.transacao.operators.TransacaoOperators;
import org.example.transacaoservice.business.transacao.model.Transacao;
import org.example.transacaoservice.business.validators.FraudeValidators;
import org.example.transacaoservice.data.conta.ContaRepository;
import org.example.transacaoservice.enums.TipoTransacao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class TransacaoService {
    private static final String FUNDOS_KEY = "FUNDOS_SUFICIENTES";
    private static final String EXECUCAO_KEY = "EXECUCAO_SUCESSO";
    private static final String FRAUDE_KEY = "FRAUDE";


    private final ContaRepository contaRepository;
    private final List<FraudeValidators> fraudeValidatorsList;
    private final List<TransacaoOperators> transacaoOperatorsList;

    @Transactional
    public Transacao validarFundos(Transacao transacao) throws Exception {
        log.info("Iniciando validação e reserva de fundos, transacao: {}", transacao.toString());
        boolean isReservaSucesso = switch (transacao.getTipoTransacao()) {
            case TipoTransacao.CREDITO -> executarTransacaoCredito(transacao);
            case TipoTransacao.DEBITO -> executarTransacaoDebito(transacao);
            default -> throw new Exception("Tipo de transação inválida");
        };

        return atualizarHistorico(transacao, FUNDOS_KEY, isReservaSucesso);
    }

    private boolean validarSaldo(Transacao transacao) {
        Long saldo = contaRepository.getSaldoByNumeroConta(transacao.getNumeroConta());
        Long valor = transacao.getValor();
        return saldo >= valor;
    }

    private boolean validarLimiteCredito(Transacao transacao) {
        Long limiteCredito = contaRepository.getLimiteCreditoByNumeroConta(transacao.getNumeroConta());
        return limiteCredito >= transacao.getValor();
    }

    public Transacao validarFraude(Transacao transacao) {
       boolean isFraude = fraudeValidatorsList.stream().anyMatch(
                fraudeValidator -> fraudeValidator.validate(transacao));

       return atualizarHistorico(transacao, FRAUDE_KEY, isFraude);
    }

    @Transactional
    public Transacao executarTransacao(Transacao transacao) {
        log.info("Confirmando execução de transação: {}", transacao);
        transacaoOperatorsList.forEach(operator -> operator.confirmarTransacao(transacao));
        return atualizarHistorico(transacao, EXECUCAO_KEY, true);
    }

    @Transactional
    public void estornarTransacao(Transacao transacao) {
        log.info("Estornando transação: {}", transacao);
        transacaoOperatorsList.forEach(operator -> operator.estornarTransacao(transacao));
    }

    private boolean executarTransacaoDebito(Transacao transacao) {
        log.info("Iniciando reserva de transação: DEBITO");
      return transacaoOperatorsList.stream()
              .allMatch(operator -> operator.updateSaldo(transacao));
    }

    private boolean executarTransacaoCredito(Transacao transacao) {
        log.info("Iniciando reserva de transação: CREDITO");
        return transacaoOperatorsList.stream()
                .allMatch(operator -> operator.updateLimiteCredito(transacao));
    }

    private Transacao atualizarHistorico(Transacao transacao, String key, boolean valor) {
        transacao.getHistorico()
                .put(key, valor);
        return transacao;
    }
}
