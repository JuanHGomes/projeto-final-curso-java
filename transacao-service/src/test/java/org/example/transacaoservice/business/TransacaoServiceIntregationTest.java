package org.example.transacaoservice.business;

import org.example.transacaoservice.business.model.Transacao;
import org.example.transacaoservice.enums.TipoTransacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
// import org.springframework.transaction.annotation.Transactional; <--- REMOVA ESTA LINHA

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
// @Transactional <--- REMOVA AQUI TAMBÉM
class TransacaoServiceIntegrationTest {

    @Autowired
    private TransacaoService transacaoService;

    private static final String CONTA_EXISTENTE = "123";

    @Test
    @DisplayName("DÉBITO: Deve retornar FALSE (válido) quando o valor é baixo e há saldo")
    void deveValidarDebitoComSaldoSuficiente() throws Exception {
        // ... resto do código igual ...
        Transacao transacao = new Transacao();
        transacao.setNumeroConta(CONTA_EXISTENTE);
        transacao.setValor(1L);
        transacao.setTipoTransacao(TipoTransacao.DEBITO);

        boolean isSaldoInsuficiente = transacaoService.validarFundos(transacao);

        assertFalse(isSaldoInsuficiente, "Deveria retornar false, indicando que HÁ saldo suficiente");
    }

    // ... (Mantenha os outros testes iguais) ...

    @Test
    @DisplayName("EXCEPTION: Deve lançar exceção para tipo de transação desconhecido")
    void deveLancarExcecaoTipoInvalido() {
        Transacao transacao = new Transacao();
        transacao.setNumeroConta(CONTA_EXISTENTE);
        transacao.setValor(100L);
        transacao.setTipoTransacao(null);

        Exception exception = assertThrows(Exception.class, () -> {
            transacaoService.validarFundos(transacao);
        });

        assertEquals("Tipo de transação inválida", exception.getMessage());
    }
}