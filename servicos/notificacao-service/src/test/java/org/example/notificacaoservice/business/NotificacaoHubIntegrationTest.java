package org.example.notificacaoservice.business;

import org.example.notificacaoservice.business.model.Notificacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testes de Integracao - NotificacaoHub")
class NotificacaoHubIntegrationTest {

    private NotificacaoHub hub;

    @BeforeEach
    void setUp() {
        hub = new NotificacaoHub();
    }

    @Nested
    @DisplayName("Testes de Subscribe")
    class SubscribeTests {

        @Test
        @DisplayName("Deve criar um Flux quando subscrever para uma conta")
        void deveCriarFlux_QuandoSubscrever() {
            Flux<Notificacao> flux = hub.subscribe("conta-1");

            assertThat(flux).isNotNull();
        }

        @Test
        @DisplayName("Deve aplicar timeout no subscriber")
        void deveAplicarTimeout_NoSubscriber() {
            Flux<Notificacao> flux = hub.subscribe("conta-timeout");

            StepVerifier.create(flux.timeout(Duration.ofSeconds(1)))
                    .expectError(java.util.concurrent.TimeoutException.class)
                    .verify(Duration.ofSeconds(2));
        }

        @Test
        @DisplayName("Deve recuperar de erro com Flux vazio")
        void deveRecuperarDeErro_ComFluxVazio() {
            Flux<Notificacao> flux = hub.subscribe("conta-error");

            StepVerifier.create(flux
                    .concatWith(Flux.error(new RuntimeException("Erro forcado")))
                    .onErrorResume(e -> Flux.empty()))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Testes de Publish")
    class PublishTests {

        @Test
        @DisplayName("Deve ignorar publicacao para conta sem subscriber")
        void deveIgnorarPublicacao_ContaSemSubscriber() {
            Notificacao notificacao = Notificacao.builder()
                    .numeroConta("conta-sem-subscriber")
                    .mensagem("Msg perdida")
                    .build();

            hub.publish(notificacao);

            Flux<Notificacao> flux = hub.subscribe("conta-sem-subscriber");

            StepVerifier.create(flux.take(Duration.ofMillis(500)))
                    .then(() -> {
                        Notificacao novaNotificacao = Notificacao.builder()
                                .numeroConta("conta-sem-subscriber")
                                .mensagem("Nova msg")
                                .build();
                        hub.publish(novaNotificacao);
                    })
                    .assertNext(n -> assertThat(n.mensagem()).isEqualTo("Nova msg"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Deve ignorar publicacao para conta inexistente")
        void deveIgnorarPublicacao_ContaInexistente() {
            Notificacao notificacao = Notificacao.builder()
                    .numeroConta("conta-inexistente")
                    .mensagem("Msg inexistente")
                    .build();

            hub.publish(notificacao);

            Flux<Notificacao> flux = hub.subscribe("conta-inexistente");

            StepVerifier.create(flux.take(Duration.ofMillis(500)))
                    .then(() -> {
                        hub.publish(Notificacao.builder()
                                .numeroConta("conta-inexistente")
                                .mensagem("Msg valida")
                                .build());
                    })
                    .assertNext(n -> assertThat(n.mensagem()).isEqualTo("Msg valida"))
                    .verifyComplete();
        }
    }
}
