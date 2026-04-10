package com.example.workflow.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - TransacaoProducer")
class TransacaoProducerUnitTest {

    @Mock
    private StreamBridge streamBridge;

    private TransacaoProducer transacaoProducer;

    @BeforeEach
    void setUp() {
        transacaoProducer = new TransacaoProducer(streamBridge);
    }

    @Nested
    @DisplayName("Envio de Mensagens")
    class EnvioMensagemTests {

        @Test
        @DisplayName("Deve enviar mensagem com sucesso")
        void deveEnviarMensagemComSucesso() {
            String binding = "testeProducer-out-0";
            Object payload = "teste payload";
            when(streamBridge.send(binding, payload)).thenReturn(true);

            boolean resultado = transacaoProducer.sendMessage(binding, payload);

            assertTrue(resultado);
            verify(streamBridge, times(1)).send(binding, payload);
        }

        @Test
        @DisplayName("Deve retornar false quando envio falhar")
        void deveRetornarFalseQuandoEnvioFalhar() {
            String binding = "testeProducer-out-0";
            Object payload = "teste payload";
            when(streamBridge.send(binding, payload)).thenReturn(false);

            boolean resultado = transacaoProducer.sendMessage(binding, payload);

            assertFalse(resultado);
        }

        @Test
        @DisplayName("Deve lancar excecao quando StreamBridge falhar")
        void deveLancarExcecaoQuandoStreamBridgeFalhar() {
            String binding = "testeProducer-out-0";
            Object payload = "teste payload";
            when(streamBridge.send(binding, payload))
                    .thenThrow(new RuntimeException("Erro no StreamBridge"));

            assertThrows(RuntimeException.class, () -> transacaoProducer.sendMessage(binding, payload));
        }

        @Test
        @DisplayName("Deve enviar mensagem com payload nulo")
        void deveEnviarMensagemComPayloadNulo() {
            String binding = "testeProducer-out-0";
            when(streamBridge.send(eq(binding), isNull())).thenReturn(true);

            boolean resultado = transacaoProducer.sendMessage(binding, null);

            assertTrue(resultado);
            verify(streamBridge, times(1)).send(binding, null);
        }

        @Test
        @DisplayName("Deve enviar mensagem com binding diferente")
        void deveEnviarMensagemComBindingDiferente() {
            String binding1 = "producer1-out-0";
            String binding2 = "producer2-out-0";
            Object payload = "teste payload";

            when(streamBridge.send(binding1, payload)).thenReturn(true);
            when(streamBridge.send(binding2, payload)).thenReturn(true);

            boolean resultado1 = transacaoProducer.sendMessage(binding1, payload);
            boolean resultado2 = transacaoProducer.sendMessage(binding2, payload);

            assertTrue(resultado1);
            assertTrue(resultado2);
            verify(streamBridge).send(binding1, payload);
            verify(streamBridge).send(binding2, payload);
        }
    }
}
