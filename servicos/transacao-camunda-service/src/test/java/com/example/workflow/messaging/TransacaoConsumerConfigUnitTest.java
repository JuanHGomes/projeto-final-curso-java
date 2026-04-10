package com.example.workflow.messaging;

import com.example.workflow.business.model.Transacao;
import com.example.workflow.business.enums.TipoTransacao;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - TransacaoConsumerConfig")
class TransacaoConsumerConfigUnitTest {

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private MessageCorrelationBuilder correlationBuilder;

    private TransacaoConsumerConfig consumerConfig;

    @BeforeEach
    void setUp() {
        consumerConfig = new TransacaoConsumerConfig(runtimeService);
    }

    @Nested
    @DisplayName("Consumo de Mensagens Kafka")
    class ConsumoMensagemTests {

        @Test
        @DisplayName("Deve processar mensagem e iniciar correlacao do processo")
        void deveProcessarMensagemEIniciarCorrelacao() {
            // given
            Consumer<Message<Transacao>> consumer = consumerConfig.transacaoConsumer();

            LinkedHashMap<String, Boolean> historico = new LinkedHashMap<>();
            historico.put("FUNDOS_SUFICIENTES", true);

            Transacao transacao = Transacao.builder()
                    .numeroConta("123456")
                    .valor(1000L)
                    .tipoTransacao(TipoTransacao.DEBITO)
                    .timeStamp(LocalDateTime.now())
                    .historico(historico)
                    .build();

            Message<Transacao> message = MessageBuilder.withPayload(transacao).build();

            when(runtimeService.createMessageCorrelation("Nova_Transacao")).thenReturn(correlationBuilder);
            when(correlationBuilder.setVariables(anyMap())).thenReturn(correlationBuilder);

            // when
            consumer.accept(message);

            // then
            verify(runtimeService).createMessageCorrelation("Nova_Transacao");
            verify(correlationBuilder).setVariables(anyMap());
            verify(correlationBuilder).correlateStartMessage();
        }

        @Test
        @DisplayName("Deve extrair payload corretamente da mensagem")
        void deveExtrairPayloadCorretamente() {
            Consumer<Message<Transacao>> consumer = consumerConfig.transacaoConsumer();

            Transacao transacao = Transacao.builder()
                    .numeroConta("999888")
                    .valor(5000L)
                    .tipoTransacao(TipoTransacao.CREDITO)
                    .timeStamp(LocalDateTime.now())
                    .historico(new LinkedHashMap<>())
                    .build();

            Message<Transacao> message = MessageBuilder.withPayload(transacao)
                    .setHeader("customHeader", "customValue")
                    .build();

            when(runtimeService.createMessageCorrelation("Nova_Transacao")).thenReturn(correlationBuilder);
            when(correlationBuilder.setVariables(anyMap())).thenReturn(correlationBuilder);

            consumer.accept(message);

            verify(correlationBuilder).correlateStartMessage();
        }

        @Test
        @DisplayName("Deve processar multiplas mensagens em sequencia")
        void deveProcessarMultiplasMensagens() {
            Consumer<Message<Transacao>> consumer = consumerConfig.transacaoConsumer();

            when(runtimeService.createMessageCorrelation("Nova_Transacao")).thenReturn(correlationBuilder);
            when(correlationBuilder.setVariables(anyMap())).thenReturn(correlationBuilder);

            Transacao t1 = Transacao.builder().numeroConta("conta-1").valor(100L).tipoTransacao(TipoTransacao.DEBITO).timeStamp(LocalDateTime.now()).historico(new LinkedHashMap<>()).build();
            Transacao t2 = Transacao.builder().numeroConta("conta-2").valor(200L).tipoTransacao(TipoTransacao.CREDITO).timeStamp(LocalDateTime.now()).historico(new LinkedHashMap<>()).build();
            Transacao t3 = Transacao.builder().numeroConta("conta-1").valor(300L).tipoTransacao(TipoTransacao.DEBITO).timeStamp(LocalDateTime.now()).historico(new LinkedHashMap<>()).build();

            consumer.accept(MessageBuilder.withPayload(t1).build());
            consumer.accept(MessageBuilder.withPayload(t2).build());
            consumer.accept(MessageBuilder.withPayload(t3).build());

            verify(runtimeService, times(3)).createMessageCorrelation("Nova_Transacao");
            verify(correlationBuilder, times(3)).correlateStartMessage();
        }
    }

    @Nested
    @DisplayName("Configuração do Bean")
    class BeanConfigurationTests {

        @Test
        @DisplayName("Deve criar bean Consumer")
        void deveCriarBeanConsumer() {
            Consumer<Message<Transacao>> consumer = consumerConfig.transacaoConsumer();
            assertThat(consumer).isNotNull();
            assertThat(consumer).isInstanceOf(Consumer.class);
        }

        @Test
        @DisplayName("Deve criar nova instancia a cada chamada")
        void deveCriarNovaInstanciaCadaChamada() {
            Consumer<Message<Transacao>> consumer1 = consumerConfig.transacaoConsumer();
            Consumer<Message<Transacao>> consumer2 = consumerConfig.transacaoConsumer();

            assertThat(consumer1).isNotSameAs(consumer2);
        }
    }
}
