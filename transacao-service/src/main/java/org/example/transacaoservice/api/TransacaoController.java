package org.example.transacaoservice.api;

import lombok.RequiredArgsConstructor;
import org.example.transacaoservice.business.transacao.TransacaoService;
import org.example.transacaoservice.business.transacao.model.Transacao;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("transacao/")
public class TransacaoController {
    private final TransacaoService transacaoService;

    @PostMapping("validarFundos")
    public boolean validarFundos(@RequestBody Transacao transacao) throws Exception {
        return transacaoService.validarFundos(transacao);
    }

    @PostMapping("validarFraude")
    public boolean validarFraude(@RequestBody Transacao transacao){
        return transacaoService.validarFraude(transacao);
    }

    @PostMapping("executarTransacao")
    public void executarTransacao(@RequestBody Transacao transacao) throws Exception {
        transacaoService.executarTransacao(transacao);
    }
}
