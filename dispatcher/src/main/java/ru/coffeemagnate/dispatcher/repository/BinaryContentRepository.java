package ru.coffeemagnate.dispatcher.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.coffeemagnate.dispatcher.model.BinaryContent;

public interface BinaryContentRepository extends JpaRepository<BinaryContent, Long> {
}
