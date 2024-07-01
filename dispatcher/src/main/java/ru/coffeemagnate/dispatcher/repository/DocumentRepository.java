package ru.coffeemagnate.dispatcher.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.coffeemagnate.dispatcher.model.RequestDocument;

public interface DocumentRepository extends JpaRepository<RequestDocument, Long> {
}
