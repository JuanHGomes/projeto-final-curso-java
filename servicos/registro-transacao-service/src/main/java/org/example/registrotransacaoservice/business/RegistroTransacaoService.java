package org.example.registrotransacaoservice.business;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.example.registrotransacaoservice.client.TransacaoServiceClient;
import org.example.registrotransacaoservice.business.model.Transacao;
import org.example.registrotransacaoservice.data.RegistroTransacaoRepository;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class RegistroTransacaoService {
    private final RegistroTransacaoRepository registroTransacaoRepository;
    private final TemplateEngine templateEngine;
    private final TransacaoServiceClient transacaoServiceClient;

    public void garantirRegistroTransacao(Transacao transacao) {
        registroTransacaoRepository.registrarSeNaoPresente(transacao);
    }

    public List<Transacao> getExtratoUltimosTrintaDias(String numeroConta) {
      return registroTransacaoRepository.findAllOverLastThirtyDaysByNumeroConta(numeroConta);
    }

    public Long getSaldo(String numeroConta) {
        return transacaoServiceClient.getSaldo(numeroConta);
    }

    public byte[] getExtratoPdf(String numeroConta) {
        List<Transacao> transacoes = getExtratoUltimosTrintaDias(numeroConta);
        
        Context context = new Context();
        context.setVariables(Map.of(
            "numeroConta", numeroConta,
            "transacoes", transacoes,
            "dataInicio", LocalDateTime.now().minusDays(30),
            "dataFim", LocalDateTime.now()
        ));

        String html = templateEngine.process("extrato-bancario.html", context);
        return generatePdfFromHtml(html);
    }

    public byte[] getFaturaPdf(String numeroConta) {
        List<Transacao> transacoesCredito = getExtratoUltimosTrintaDias(numeroConta).stream()
                .filter(t -> t.getTipoTransacao() == Transacao.TipoTransacao.CREDITO)
                .toList();

        Context context = new Context();
        context.setVariables(Map.of(
                "numeroConta", numeroConta,
                "transacoes", transacoesCredito,
                "cartaoFinal", "**** 1234",
                "vencimento", LocalDateTime.now().plusDays(10),
                "limiteTotal", 500000L
        ));

        String html = templateEngine.process("extrato-cartao.html", context);
        return generatePdfFromHtml(html);
    }

    private byte[] generatePdfFromHtml(String html) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, "/");
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar PDF", e);
        }
    }
}
