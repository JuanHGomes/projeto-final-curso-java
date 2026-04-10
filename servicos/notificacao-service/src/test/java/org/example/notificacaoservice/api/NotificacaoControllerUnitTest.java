package org.example.notificacaoservice.api;

import org.example.notificacaoservice.business.NotificacaoService;
import org.example.notificacaoservice.business.model.Notificacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - NotificacaoController")
class NotificacaoControllerUnitTest {

    @Mock
    private NotificacaoService notificacaoService;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        NotificacaoController controller = new NotificacaoController(notificacaoService, null);
        webTestClient = WebTestClient.bindToController(controller).build();
    }

    @Test
    @DisplayName("Deve retornar health check")
    void deveRetornarHealthCheck() {
        webTestClient.get()
                .uri("/")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Notificacao Service is running");
    }

    @Test
    @DisplayName("Deve enviar notificação de teste via POST")
    void deveEnviarNotificacaoTeste() {
        webTestClient.post()
                .uri("/test?numeroConta=12345")
                .exchange()
                .expectStatus().isOk();

        verify(notificacaoService, times(1)).dispararNotificacao(any(Notificacao.class));
    }

    @Test
    @DisplayName("Deve usar conta padrão 123 quando não informada no POST")
    void deveUsarContaPadrao() {
        webTestClient.post()
                .uri("/test")
                .exchange()
                .expectStatus().isOk();

        verify(notificacaoService).dispararNotificacao(argThat(n ->
                n.numeroConta().equals("123")
        ));
    }

    @Test
    @DisplayName("Deve conectar SSE e enviar mensagem de boas-vindas")
    void deveConectarSseEEnviarWelcome() {
        when(notificacaoService.receberNotificacao(eq("conta-1")))
                .thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/notificacao?numeroConta=conta-1")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .returnResult(Object.class)
                .getResponseBody()
                .take(Duration.ofSeconds(1))
                .count()
                .block();

        verify(notificacaoService).receberNotificacao("conta-1");
    }

    @Test
    @DisplayName("Deve receber notificações via SSE")
    void deveReceberNotificacoesViaSse() {
        Notificacao n1 = Notificacao.builder()
                .numeroConta("conta-sse")
                .mensagem("Notificação 1")
                .build();

        when(notificacaoService.receberNotificacao("conta-sse"))
                .thenReturn(Flux.just(n1));

        webTestClient.get()
                .uri("/notificacao?numeroConta=conta-sse")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .returnResult(Object.class)
                .getResponseBody()
                .take(Duration.ofSeconds(1))
                .count()
                .block();

        verify(notificacaoService).receberNotificacao("conta-sse");
    }
}
