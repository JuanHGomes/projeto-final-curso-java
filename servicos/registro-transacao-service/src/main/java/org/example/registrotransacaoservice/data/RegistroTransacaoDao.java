package org.example.registrotransacaoservice.data;

import org.example.registrotransacaoservice.data.model.TransacaoDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RegistroTransacaoDao extends MongoRepository<TransacaoDocument, String> {
    Optional<TransacaoDocument> findTransacaoDocumentByNumeroContaAndTimeStamp(String numeroConta, LocalDateTime timeStamp);
}
