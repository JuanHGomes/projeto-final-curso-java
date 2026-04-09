package org.example.notificacaoservice.messaging;

import org.example.notificacaoservice.business.NotificacaoHub;
import org.example.notificacaoservice.business.NotificacaoService;
import org.example.notificacaoservice.business.model.Notificacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes de Integracao - NotificacaoConsumerConfig")
class NotificacaoConsumerConfigIntegrationTest {

    @Mock
    private NotificacaoService notificacaoService;

    @InjectMocks
    private NotificacaoConsumerConfig consumerConfig;

    private Consumer<Message<Notificacao>> consumer;

    @BeforeEach
    void setUp() {
        consumer = consumerConfig.notificacaoConsumer();
        assertThat(consumer).isNotNull();
    }

    @Nested
    @DisplayName("Testes de Consumo de Mensagens Kafka")
    class KafkaMessageConsumptionTests {

        @Test
        @DisplayName("Deve processar mensagem Kafka recebida")
        void deveProcessarMensagemKafka() {
            Notificacao notificacao = Notificacao.builder()
                    .numeroConta("conta-kafka")
                    .mensagem("Mensagem do Kafka")
                    .build();

            Message<Notificacao> message = MessageBuilder.withPayload(notificacao).build();

            consumer.accept(message);

            verify(notificacaoService, times(1)).dispararNotificacao(notificacao);
        }

        @Test
        @DisplayName("Deve extrair payload corretamente da mensagem")
        void deveExtrairPayload_Corretamente() {
            Notificacao notificacao = Notificacao.builder()
                    .numeroConta("conta-payload")
                    .mensagem("Teste payload")
                    .build();

            Message<Notificacao> message = MessageBuilder.withPayload(notificacao)
                    .setHeader("customHeader", "customValue")
                    .build();

            consumer.accept(message);

            ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
            verify(notificacaoService).dispararNotificacao(captor.capture());

            Notificacao captured = captor.getValue();
            assertThat(captured.numeroConta()).isEqualTo("conta-payload");
            assertThat(captured.mensagem()).isEqualTo("Teste payload");
        }

        @Test
        @DisplayName("Deve processar multiplas mensagens em sequencia")
        void deveProcessarMultiplasMensagens() {
            Notificacao n1 = Notificacao.builder().numeroConta("conta-1").mensagem("Msg 1").build();
            Notificacao n2 = Notificacao.builder().numeroConta("conta-2").mensagem("Msg 2").build();
            Notificacao n3 = Notificacao.builder().numeroConta("conta-1").mensagem("Msg 3").build();

            consumer.accept(MessageBuilder.withPayload(n1).build());
            consumer.accept(MessageBuilder.withPayload(n2).build());
            consumer.accept(MessageBuilder.withPayload(n3).build());

            verify(notificacaoService, times(3)).dispararNotificacao(any(Notificacao.class));
            verify(notificacaoService).dispararNotificacao(n1);
            verify(notificacaoService).dispararNotificacao(n2);
            verify(notificacaoService).dispararNotificacao(n3);
        }
    }

    @Nested
    @DisplayName("Testes de Propagacao de Erros")
    class ErrorPropagationTests {

        @Test
        @DisplayName("Deve propagar excecao do servico")
        void devePropagarExcecao_DoServico() {
            Notificacao notificacao = Notificacao.builder()
                    .numeroConta("conta-error")
                    .mensagem("Mensagem com erro")
                    .build();

            doThrow(new RuntimeException("Erro simulado"))
                    .when(notificacaoService).dispararNotificacao(any(Notificacao.class));

            Message<Notificacao> message = MessageBuilder.withPayload(notificacao).build();

            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () ->
                    consumer.accept(message)
            );
        }
    }

    @Nested
    @DisplayName("Testes de Integracao Consumer -> Hub")
    class ConsumerHubIntegrationTests {

        @Test
        @DisplayName("Ciclo completo: Consumer -> Service -> Hub")
        void cicloCompleto_ConsumerServiceHub() {
            NotificacaoHub hubReal = new NotificacaoHub();
            NotificacaoService serviceReal = new NotificacaoService(hubReal);
            NotificacaoConsumerConfig configReal = new NotificacaoConsumerConfig(serviceReal);

            Consumer<Message<Notificacao>> consumerReal = configReal.notificacaoConsumer();

            Flux<Notificacao> flux = hubReal.subscribe("conta-end2end");

            Notificacao notificacao = Notificacao.builder()
                    .numeroConta("conta-end2end")
                    .mensagem("Mensagem end-to-end")
                    .build();

            Message<Notificacao> message = MessageBuilder.withPayload(notificacao).build();

            consumerReal.accept(message);

            StepVerifier.create(flux.take(Duration.ofMillis(500)))
                    .assertNext(n -> {
                        assertThat(n.numeroConta()).isEqualTo("conta-end2end");
                        assertThat(n.mensagem()).isEqualTo("Mensagem end-to-end");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Consumer deve isolar contas diferentes")
        void consumer_deveIsolarContasDiferentes() {
            NotificacaoHub hubReal = new NotificacaoHub();
            NotificacaoService serviceReal = new NotificacaoService(hubReal);
            NotificacaoConsumerConfig configReal = new NotificacaoConsumerConfig(serviceReal);

            Consumer<Message<Notificacao>> consumerReal = configReal.notificacaoConsumer();

            Flux<Notificacao> fluxA = hubReal.subscribe("conta-A");
            Flux<Notificacao> fluxB = hubReal.subscribe("conta-B");

            consumerReal.accept(MessageBuilder.withPayload(
                    Notificacao.builder().numeroConta("conta-A").mensagem("A1").build()).build());
            consumerReal.accept(MessageBuilder.withPayload(
                    Notificacao.builder().numeroConta("conta-B").mensagem("B1").build()).build());
            consumerReal.accept(MessageBuilder.withPayload(
                    Notificacao.builder().numeroConta("conta-A").mensagem("A2").build()).build());

            StepVerifier.create(fluxA.take(Duration.ofMillis(500)))
                    .assertNext(n -> assertThat(n.mensagem()).isEqualTo("A1"))
                    .assertNext(n -> assertThat(n.mensagem()).isEqualTo("A2"))
                    .verifyComplete();

            StepVerifier.create(fluxB.take(Duration.ofMillis(500)))
                    .assertNext(n -> assertThat(n.mensagem()).isEqualTo("B1"))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Testes de Configuracao do Bean")
    class BeanConfigurationTests {

        @Test
        @DisplayName("Deve criar bean Consumer com nome notificacaoConsumer")
        void deveCriarBeanComNomeCorreto() {
            assertThat(consumer).isNotNull();
            assertThat(consumer).isInstanceOf(Consumer.class);
        }

        @Test
        @DisplayName("Deve criar nova instancia de Consumer a cada chamada")
        void deveCriarNovaInstancia_CadaChamada() {
            Consumer<Message<Notificacao>> consumer2 = consumerConfig.notificacaoConsumer();

            assertThat(consumer).isNotSameAs(consumer2);
        }
    }
}
