package ru.coffeemagnate.dispatcher.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.coffeemagnate.dispatcher.model.RequestPhoto;

public interface PhotoRepository extends JpaRepository<RequestPhoto, Long>{
}
