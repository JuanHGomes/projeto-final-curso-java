package org.example.registrotransacaoservice.messaggin;

import org.example.registrotransacaoservice.business.RegistroTransacaoService;
import org.example.registrotransacaoservice.business.model.Transacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RegistroTransacaoConsumerConfigUnitTest {

    @Mock
    private RegistroTransacaoService registroTransacaoService;

    @InjectMocks
    private RegistroTransacaoConsumerConfig registroTransacaoConsumerConfig;

    @Test
    @DisplayName("Deve consumir mensagem e registrar transação")
    void deveConsumirMensagemERegistrarTransacao() {
        // given
        RegistroTransacaoConsumerConfig consumer = new RegistroTransacaoConsumerConfig(registroTransacaoService);

        Transacao transacao = Transacao.builder()
                .numeroConta("12345")
                .valor(10000L)
                .tipoTransacao(Transacao.TipoTransacao.DEBITO)
                .timeStamp(LocalDateTime.now())
                .estabelecimento("Supermercado")
                .historico(new LinkedHashMap<>(Map.of("processado", true)))
                .build();

        Message<Transacao> message = MessageBuilder.withPayload(transacao).build();

        // when
        consumer.registroTransacaoConsumer().accept(message);

        // then
        verify(registroTransacaoService).garantirRegistroTransacao(transacao);
    }

    @Test
    @DisplayName("Deve consumir mensagem com transação de crédito")
    void deveConsumirMensagemCredito() {
        // given
        RegistroTransacaoConsumerConfig consumer = new RegistroTransacaoConsumerConfig(registroTransacaoService);

        Transacao transacao = Transacao.builder()
                .numeroConta("67890")
                .valor(50000L)
                .tipoTransacao(Transacao.TipoTransacao.CREDITO)
                .timeStamp(LocalDateTime.now())
                .estabelecimento("Loja de Roupas")
                .historico(new LinkedHashMap<>(Map.of("processado", true)))
                .build();

        Message<Transacao> message = MessageBuilder.withPayload(transacao).build();

        // when
        consumer.registroTransacaoConsumer().accept(message);

        // then
        verify(registroTransacaoService).garantirRegistroTransacao(transacao);
    }
}
