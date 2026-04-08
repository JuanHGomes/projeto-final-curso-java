package org.example.registrotransacaoservice.data;

import org.example.registrotransacaoservice.data.model.TransacaoDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RegistroTransacaoDao extends MongoRepository<TransacaoDocument, String> {
    Optional<TransacaoDocument> findTransacaoDocumentByNumeroContaAndTimeStamp(String numeroConta, LocalDateTime timeStamp);

    Page<TransacaoDocument> findByNumeroContaAndTimeStampBetween(String numeroConta, LocalDateTime trintaDiasAtras, LocalDateTime hoje, Pageable pageable);

    java.util.List<TransacaoDocument> findByNumeroContaAndTimeStampBetween(String numeroConta, LocalDateTime trintaDiasAtras, LocalDateTime hoje);
}
