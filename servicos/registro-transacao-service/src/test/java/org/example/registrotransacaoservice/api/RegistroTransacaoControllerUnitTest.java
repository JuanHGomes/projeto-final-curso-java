package org.example.registrotransacaoservice.api;

import org.example.registrotransacaoservice.business.RegistroTransacaoService;
import org.example.registrotransacaoservice.business.model.Transacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RegistroTransacaoControllerUnitTest {

    private MockMvc mockMvc;

    @Mock
    private RegistroTransacaoService registroTransacaoService;

    @InjectMocks
    private RegistroTransacaoController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Deve retornar extrato da conta")
    void deveRetornarExtrato() throws Exception {
        // given
        String numeroConta = "12345";
        Transacao t1 = Transacao.builder()
                .numeroConta(numeroConta)
                .valor(10000L)
                .tipoTransacao(Transacao.TipoTransacao.DEBITO)
                .timeStamp(LocalDateTime.now().minusDays(5))
                .estabelecimento("Supermercado")
                .build();

        Transacao t2 = Transacao.builder()
                .numeroConta(numeroConta)
                .valor(20000L)
                .tipoTransacao(Transacao.TipoTransacao.CREDITO)
                .timeStamp(LocalDateTime.now().minusDays(10))
                .estabelecimento("Farmácia")
                .build();

        when(registroTransacaoService.getExtratoUltimosTrintaDias(numeroConta))
                .thenReturn(List.of(t1, t2));

        // when & then
        mockMvc.perform(get("/registro-transacao/extrato/{numeroConta}", numeroConta))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].numeroConta").value(numeroConta))
                .andExpect(jsonPath("$[0].estabelecimento").value("Supermercado"))
                .andExpect(jsonPath("$[1].estabelecimento").value("Farmácia"));
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando sem transações")
    void deveRetornarListaVazia() throws Exception {
        // given
        String numeroConta = "99999";
        when(registroTransacaoService.getExtratoUltimosTrintaDias(numeroConta))
                .thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/registro-transacao/extrato/{numeroConta}", numeroConta))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("Deve retornar saldo da conta")
    void deveRetornarSaldo() throws Exception {
        // given
        String numeroConta = "12345";
        when(registroTransacaoService.getSaldo(numeroConta)).thenReturn(150000L);

        // when & then
        mockMvc.perform(get("/registro-transacao/saldo/{numeroConta}", numeroConta))
                .andExpect(status().isOk())
                .andExpect(content().string("150000"));
    }

    @Test
    @DisplayName("Deve retornar limite de crédito")
    void deveRetornarLimiteCredito() throws Exception {
        // given
        String numeroConta = "12345";
        when(registroTransacaoService.getLimiteCredito(numeroConta)).thenReturn(500000L);

        // when & then
        mockMvc.perform(get("/registro-transacao/limiteCredito/{numeroConta}", numeroConta))
                .andExpect(status().isOk())
                .andExpect(content().string("500000"));
    }

    @Test
    @DisplayName("Deve retornar PDF do extrato")
    void deveRetornarPdfExtrato() throws Exception {
        // given
        String numeroConta = "12345";
        byte[] pdfContent = "%PDF-1.4 fake pdf content".getBytes();
        when(registroTransacaoService.getExtratoPdf(numeroConta)).thenReturn(pdfContent);

        // when & then
        mockMvc.perform(get("/registro-transacao/extratoPdf/{numeroConta}", numeroConta))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("extrato-" + numeroConta + ".pdf")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfContent));
    }

    @Test
    @DisplayName("Deve retornar PDF da fatura")
    void deveRetornarPdfFatura() throws Exception {
        // given
        String numeroConta = "12345";
        byte[] pdfContent = "%PDF-1.4 fake fatura content".getBytes();
        when(registroTransacaoService.getFaturaPdf(numeroConta)).thenReturn(pdfContent);

        // when & then
        mockMvc.perform(get("/registro-transacao/faturaPdf/{numeroConta}", numeroConta))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("fatura-" + numeroConta + ".pdf")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfContent));
    }

    @Test
    @DisplayName("Deve enviar extrato por e-mail")
    void deveEnviarExtratoEmail() throws Exception {
        // given
        String numeroConta = "12345";

        // when & then
        mockMvc.perform(post("/registro-transacao/enviarExtratoEmail/{numeroConta}", numeroConta))
                .andExpect(status().isOk())
                .andExpect(content().string("E-mail enviado com sucesso!"));
    }
}
