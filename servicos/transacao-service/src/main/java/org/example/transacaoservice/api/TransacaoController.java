package org.example.transacaoservice.api;

import lombok.RequiredArgsConstructor;
import org.example.transacaoservice.api.model.TransacaoRequest;
import org.example.transacaoservice.api.model.TransacaoResponse;
import org.example.transacaoservice.business.transacao.TransacaoService;
import org.example.transacaoservice.business.transacao.model.Transacao;
import org.example.transacaoservice.data.conta.ContaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@RestController
@RequestMapping("transacao/")
public class TransacaoController {
    private final TransacaoService transacaoService;
    private final TransacaoApiMapper mapper;
    private final ContaRepository contaRepository;

    @PostMapping("validarFundos")
    public ResponseEntity<TransacaoResponse> validarFundos(@RequestBody TransacaoRequest transacaoRequest) throws Exception {
        Transacao transacao = mapper.toTransacao(transacaoRequest);
        Transacao transcaoValidada = transacaoService.validarFundos(transacao);
        TransacaoResponse transacaoResponse = mapper.toResponse(transcaoValidada);

        return ResponseEntity.ok(transacaoResponse);
    }

    @PostMapping("validarFraude")
    public ResponseEntity<TransacaoResponse> validarFraude(@RequestBody TransacaoRequest transacaoRequest){
        Transacao transacao = mapper.toTransacao(transacaoRequest);
        Transacao transcaoValidada = transacaoService.validarFraude(transacao);
        TransacaoResponse transacaoResponse = mapper.toResponse(transcaoValidada);

        return ResponseEntity.ok(transacaoResponse);
    }

    @PostMapping("executarTransacao")
    public ResponseEntity<TransacaoResponse> executarTransacao(@RequestBody TransacaoRequest transacaoRequest) throws Exception {
        Transacao transacao = mapper.toTransacao(transacaoRequest);
        Transacao transcaoValidada = transacaoService.executarTransacao(transacao);
        TransacaoResponse transacaoResponse = mapper.toResponse(transcaoValidada);

        return ResponseEntity.ok(transacaoResponse);
    }

    @PostMapping("estornarTransacao")
    public ResponseEntity<Void> estornarTransacao(@RequestBody TransacaoRequest transacaoRequest) {
        Transacao transacao = mapper.toTransacao(transacaoRequest);
        transacaoService.estornarTransacao(transacao);
        return ResponseEntity.ok().build();
    }

    @GetMapping("saldo/{numeroConta}")
    public ResponseEntity<Long> getSaldo(@PathVariable String numeroConta) {
        Long saldo = contaRepository.getSaldoByNumeroConta(numeroConta);
        return ResponseEntity.ok(saldo);
    }

    @GetMapping("limiteCredito/{numeroConta}")
    public ResponseEntity<Long> getLimiteCredito(@PathVariable String numeroConta) {
        Long limiteCredito = contaRepository.getLimiteCreditoByNumeroConta(numeroConta);
        return ResponseEntity.ok(limiteCredito);
    }
}
