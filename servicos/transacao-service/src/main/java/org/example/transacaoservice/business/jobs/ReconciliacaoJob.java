package org.example.transacaoservice.business.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.transacaoservice.business.transacao.TransacaoService;
import org.example.transacaoservice.business.transacao.model.Transacao;
import org.example.transacaoservice.data.transacao.TransacaoRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliacaoJob {

    private final RedisTemplate<String, Object> redisTemplate;
    private final TransacaoService transacaoService;
    private final TransacaoRepository transacaoRepository;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 60000) // Executa a cada 1 minuto para testes
    public void reconciliarTransacoesOrfas() {
        log.info("Iniciando job de reconciliação de transações órfãs...");

        Set<String> keys = redisTemplate.keys("transacao:*");

        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (String key : keys) {
            try {
                String json = (String) redisTemplate.opsForValue().get(key);
                Transacao transacao = objectMapper.readValue(json, Transacao.class);

                // Se a transação NÃO existe no MongoDB, significa que ela ficou "presa" na reserva
                if (transacaoRepository.getTransacaoByNumeroConta(transacao.getNumeroConta()).isEmpty()) {
                    log.warn("Transação órfã detectada para a conta {}. Realizando estorno automático.", transacao.getNumeroConta());
                    transacaoService.estornarTransacao(transacao);
                } else {
                    // Se existe no Mongo mas a chave ainda está no Redis, apenas limpamos a chave (confirmação que falhou em limpar)
                    log.info("Transação para a conta {} já processada. Limpando reserva residual.", transacao.getNumeroConta());
                    redisTemplate.delete(key);
                }

            } catch (Exception e) {
                log.error("Erro ao processar reconciliação para a chave: {}", key, e);
            }
        }
    }
}
