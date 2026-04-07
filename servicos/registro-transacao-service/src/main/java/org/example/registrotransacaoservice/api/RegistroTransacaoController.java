package org.example.registrotransacaoservice.api;

import lombok.RequiredArgsConstructor;
import org.example.registrotransacaoservice.business.RegistroTransacaoService;
import org.example.registrotransacaoservice.business.model.Transacao;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@RestController
@RequestMapping("registro-transacao/")
public class RegistroTransacaoController {
    private final RegistroTransacaoService registroTransacaoService;

    @GetMapping("extrato/{numeroConta}")
    public ResponseEntity<List<Transacao>> exibirExtrato(@PathVariable String numeroConta){
        List<Transacao> extrato = registroTransacaoService.getExtratoUltimosTrintaDias(numeroConta);

        return ResponseEntity.ok(extrato);
    }

//    @GetMapping("extratoPdf/{numeroConta})")
//    public ResponseEntity<byte[]> downloadExtratoPdf(@PathVariable String numeroConta){
//       byte[] pdf = registroTransacaoService.getExtratoPdf(numeroConta);
//    }
}
