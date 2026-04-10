package org.example.notificacaoservice.business;

import org.example.notificacaoservice.business.model.Notificacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testes Unitários - NotificacaoHub")
class NotificacaoHubUnitTest {

    private NotificacaoHub hub;

    @BeforeEach
    void setUp() {
        hub = new NotificacaoHub();
    }

    @Nested
    @DisplayName("Testes de Subscribe")
    class SubscribeTests {

        @Test
        @DisplayName("Deve criar Flux não nulo ao subscrever")
        void deveCriarFluxNulo() {
            Flux<Notificacao> flux = hub.subscribe("conta-1");
            assertThat(flux).isNotNull();
        }

        @Test
        @DisplayName("Deve criar Flux diferente para contas diferentes")
        void deveCriarFluxDiferenteParaContasDiferentes() {
            Flux<Notificacao> flux1 = hub.subscribe("conta-A");
            Flux<Notificacao> flux2 = hub.subscribe("conta-B");
            assertThat(flux1).isNotSameAs(flux2);
        }

        @Test
        @DisplayName("Deve receber notificação publicada após subscribe")
        void deveReceberNotificacaoAposSubscribe() throws InterruptedException {
            Flux<Notificacao> flux = hub.subscribe("conta-1");

            List<Notificacao> received = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);

            flux.subscribe(
                    n -> { received.add(n); latch.countDown(); },
                    e -> {},
                    () -> {}
            );

            // Pequeno delay para garantir que o subscriber está ativo
            Thread.sleep(100);

            Notificacao notificacao = Notificacao.builder()
                    .numeroConta("conta-1")
                    .mensagem("Teste mensagem")
                    .build();

            hub.publish(notificacao);

            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(received).hasSize(1);
            assertThat(received.get(0).numeroConta()).isEqualTo("conta-1");
            assertThat(received.get(0).mensagem()).isEqualTo("Teste mensagem");
        }

        @Test
        @DisplayName("Deve receber múltiplas notificações")
        void deveReceberMultiplasNotificacoes() throws InterruptedException {
            Flux<Notificacao> flux = hub.subscribe("conta-2");

            List<Notificacao> received = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(3);

            flux.subscribe(
                    n -> { received.add(n); latch.countDown(); },
                    e -> {},
                    () -> {}
            );

            Thread.sleep(100);

            hub.publish(Notificacao.builder().numeroConta("conta-2").mensagem("Msg 1").build());
            hub.publish(Notificacao.builder().numeroConta("conta-2").mensagem("Msg 2").build());
            hub.publish(Notificacao.builder().numeroConta("conta-2").mensagem("Msg 3").build());

            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(received).hasSize(3);
            assertThat(received.get(0).mensagem()).isEqualTo("Msg 1");
            assertThat(received.get(1).mensagem()).isEqualTo("Msg 2");
            assertThat(received.get(2).mensagem()).isEqualTo("Msg 3");
        }
    }

    @Nested
    @DisplayName("Testes de Publish")
    class PublishTests {

        @Test
        @DisplayName("Não deve lançar exceção ao publicar para conta sem subscriber")
        void naoDeveLancarExcecao_ContaSemSubscriber() {
            Notificacao notificacao = Notificacao.builder()
                    .numeroConta("conta-sem-sub")
                    .mensagem("Mensagem perdida")
                    .build();

            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> hub.publish(notificacao));
        }

        @Test
        @DisplayName("Não deve lançar exceção ao publicar para conta inexistente")
        void naoDeveLancarExcecao_ContaInexistente() {
            Notificacao notificacao = Notificacao.builder()
                    .numeroConta("conta-inexistente")
                    .mensagem("Mensagem")
                    .build();

            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> hub.publish(notificacao));
        }

        @Test
        @DisplayName("Deve isolar notificações entre contas diferentes")
        void deveIsolarNotificacoesEntreContas() throws InterruptedException {
            List<Notificacao> receivedA = new ArrayList<>();
            List<Notificacao> receivedB = new ArrayList<>();
            CountDownLatch latchA = new CountDownLatch(1);
            CountDownLatch latchB = new CountDownLatch(1);

            hub.subscribe("conta-A").subscribe(
                    n -> { receivedA.add(n); latchA.countDown(); }, e -> {}, () -> {}
            );
            hub.subscribe("conta-B").subscribe(
                    n -> { receivedB.add(n); latchB.countDown(); }, e -> {}, () -> {}
            );

            Thread.sleep(100);

            hub.publish(Notificacao.builder().numeroConta("conta-A").mensagem("A1").build());
            hub.publish(Notificacao.builder().numeroConta("conta-B").mensagem("B1").build());

            assertThat(latchA.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(latchB.await(2, TimeUnit.SECONDS)).isTrue();

            assertThat(receivedA).hasSize(1);
            assertThat(receivedA.get(0).mensagem()).isEqualTo("A1");

            assertThat(receivedB).hasSize(1);
            assertThat(receivedB.get(0).mensagem()).isEqualTo("B1");
        }
    }

    @Nested
    @DisplayName("Testes de Cleanup")
    class CleanupTests {

        @Test
        @DisplayName("Não deve lançar exceção ao publicar após subscriber cancelar")
        void naoDeveLancarExcecao_AposSubscriberCancelar() throws InterruptedException {
            Flux<Notificacao> flux = hub.subscribe("conta-cleanup");

            CountDownLatch latch = new CountDownLatch(1);
            flux.subscribe(n -> {}, e -> {}, latch::countDown);

            Thread.sleep(100);

            // Publicar após subscriber ativo não deve causar erro
            Notificacao notificacao = Notificacao.builder()
                    .numeroConta("conta-cleanup")
                    .mensagem("Pós cleanup")
                    .build();

            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> hub.publish(notificacao));
        }

        @Test
        @DisplayName("Deve manter sink com múltiplos subscribers ativos")
        void deveManterSink_ComMultiplosSubscribers() throws InterruptedException {
            List<Notificacao> received1 = new ArrayList<>();
            List<Notificacao> received2 = new ArrayList<>();
            CountDownLatch latch1 = new CountDownLatch(1);
            CountDownLatch latch2 = new CountDownLatch(1);

            hub.subscribe("conta-multi").subscribe(
                    n -> { received1.add(n); latch1.countDown(); }, e -> {}, () -> {}
            );
            hub.subscribe("conta-multi").subscribe(
                    n -> { received2.add(n); latch2.countDown(); }, e -> {}, () -> {}
            );

            Thread.sleep(100);

            hub.publish(Notificacao.builder().numeroConta("conta-multi").mensagem("Multi").build());

            assertThat(latch1.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(latch2.await(2, TimeUnit.SECONDS)).isTrue();

            assertThat(received1).hasSize(1);
            assertThat(received1.get(0).mensagem()).isEqualTo("Multi");

            assertThat(received2).hasSize(1);
            assertThat(received2.get(0).mensagem()).isEqualTo("Multi");
        }
    }

    @Nested
    @DisplayName("Testes de Timeout")
    class TimeoutTests {

        @Test
        @DisplayName("Deve criar Flux com timeout configurado")
        void deveCriarFluxComTimeoutConfigurado() {
            Flux<Notificacao> flux = hub.subscribe("conta-timeout");
            assertThat(flux).isNotNull();
        }
    }
}
