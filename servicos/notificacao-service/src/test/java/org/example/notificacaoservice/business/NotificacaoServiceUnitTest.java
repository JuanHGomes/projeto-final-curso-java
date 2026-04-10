package org.example.notificacaoservice.business;

import org.example.notificacaoservice.business.model.Notificacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - NotificacaoService")
class NotificacaoServiceUnitTest {

    @Mock
    private NotificacaoHub hub;

    @InjectMocks
    private NotificacaoService notificacaoService;

    @Nested
    @DisplayName("Testes - receberNotificacao")
    class ReceberNotificacaoTests {

        @Test
        @DisplayName("Deve delegar ao hub.subscribe com o número da conta")
        void deveDelegarAoHubSubscribe() {
            Flux<Notificacao> expectedFlux = Flux.just(
                    Notificacao.builder().numeroConta("conta-1").mensagem("msg").build()
            );
            when(hub.subscribe("conta-1")).thenReturn(expectedFlux);

            Flux<Notificacao> result = notificacaoService.receberNotificacao("conta-1");

            assertThat(result).isNotNull();
            verify(hub, times(1)).subscribe("conta-1");
        }

        @Test
        @DisplayName("Deve retornar Flux recebido do hub")
        void deveRetornarFluxDoHub() {
            Notificacao n1 = Notificacao.builder().numeroConta("conta-2").mensagem("msg1").build();
            Notificacao n2 = Notificacao.builder().numeroConta("conta-2").mensagem("msg2").build();
            when(hub.subscribe("conta-2")).thenReturn(Flux.just(n1, n2));

            Flux<Notificacao> result = notificacaoService.receberNotificacao("conta-2");

            StepVerifier.create(result)
                    .assertNext(n -> assertThat(n.mensagem()).isEqualTo("msg1"))
                    .assertNext(n -> assertThat(n.mensagem()).isEqualTo("msg2"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Deve propagar erro do hub")
        void devePropagarErroDoHub() {
            when(hub.subscribe("conta-erro"))
                    .thenReturn(Flux.error(new RuntimeException("Erro simulado")));

            Flux<Notificacao> result = notificacaoService.receberNotificacao("conta-erro");

            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();
        }

        @Test
        @DisplayName("Deve retornar Flux vazio quando hub retornar vazio")
        void deveRetornarFluxVazio() {
            when(hub.subscribe("conta-vazia")).thenReturn(Flux.empty());

            Flux<Notificacao> result = notificacaoService.receberNotificacao("conta-vazia");

            StepVerifier.create(result)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Testes - dispararNotificacao")
    class DispararNotificacaoTests {

        @Test
        @DisplayName("Deve delegar ao hub.publish com a notificação correta")
        void deveDelegarAoHubPublish() {
            Notificacao notificacao = Notificacao.builder()
                    .numeroConta("conta-disparo")
                    .mensagem("Notificação de teste")
                    .build();

            notificacaoService.dispararNotificacao(notificacao);

            verify(hub, times(1)).publish(notificacao);
        }

        @Test
        @DisplayName("Deve disparar múltiplas notificações")
        void deveDispararMultiplasNotificacoes() {
            Notificacao n1 = Notificacao.builder().numeroConta("conta-1").mensagem("Msg 1").build();
            Notificacao n2 = Notificacao.builder().numeroConta("conta-2").mensagem("Msg 2").build();
            Notificacao n3 = Notificacao.builder().numeroConta("conta-1").mensagem("Msg 3").build();

            notificacaoService.dispararNotificacao(n1);
            notificacaoService.dispararNotificacao(n2);
            notificacaoService.dispararNotificacao(n3);

            verify(hub).publish(n1);
            verify(hub).publish(n2);
            verify(hub).publish(n3);
            verify(hub, times(3)).publish(any(Notificacao.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando hub lançar erro")
        void deveLancarExcecaoQuandoHubLancarErro() {
            Notificacao notificacao = Notificacao.builder()
                    .numeroConta("conta-error")
                    .mensagem("Msg com erro")
                    .build();

            doThrow(new RuntimeException("Erro no hub")).when(hub).publish(any(Notificacao.class));

            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () ->
                    notificacaoService.dispararNotificacao(notificacao)
            );
        }
    }
}
