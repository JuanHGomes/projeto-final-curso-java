package org.example.registrotransacaoservice.business;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.example.registrotransacaoservice.business.model.Transacao;
import org.example.registrotransacaoservice.data.RegistroTransacaoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public void garantirRegistroTransacao(Transacao transacao) {
        registroTransacaoRepository.registrarSeNaoPresente(transacao);
    }

    public Page<Transacao> getExtratoUltimosTrintaDias(String numeroConta, Pageable pageable) {
        return registroTransacaoRepository.findAllOverLastThirtyDaysByNumeroConta(numeroConta, pageable);
    }

    public List<Transacao> getExtratoUltimosTrintaDias(String numeroConta) {
      return registroTransacaoRepository.findAllOverLastThirtyDaysByNumeroConta(numeroConta);
    }

    public Long getSaldo(String numeroConta) {
        return getExtratoUltimosTrintaDias(numeroConta).stream()
                .mapToLong(t -> t.getTipoTransacao() == Transacao.TipoTransacao.CREDITO ? t.getValor() : -t.getValor())
                .sum();
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
