package ru.coffeemagnate.dispatcher.service;

import org.telegram.telegrambots.meta.api.objects.Update;
import ru.coffeemagnate.dispatcher.model.RequestRefund;

import java.util.List;

public interface RequestService {
    void save(RequestRefund requestRefund);
    List<RequestRefund> findAll();
    RequestRefund findById();
    void delete(Long id);
    void processTextMessage(Update update);
}
