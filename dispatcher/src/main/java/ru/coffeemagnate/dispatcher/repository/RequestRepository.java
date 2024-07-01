package ru.coffeemagnate.dispatcher.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.coffeemagnate.dispatcher.model.RequestRefund;

import java.util.List;

public interface RequestRepository extends JpaRepository<RequestRefund, Long> {
    List<RequestRefund> findById(long id);
    List<RequestRefund> findByChatId(long chatId);
}
