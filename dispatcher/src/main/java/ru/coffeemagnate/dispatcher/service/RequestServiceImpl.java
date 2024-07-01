package ru.coffeemagnate.dispatcher.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.coffeemagnate.dispatcher.model.RequestRefund;
import ru.coffeemagnate.dispatcher.repository.RequestRepository;

import java.util.List;
@Service
public class RequestServiceImpl implements RequestService{
    static final Logger logger = LogManager.getLogger(RequestServiceImpl.class);
    @Autowired
    private RequestRepository requestRepository;

    @Override
    public void processTextMessage(Update update) {
    };

    private void saveRequestRefund(Update update) {
    }

    @Override
    public void save(RequestRefund request) {
        RequestRefund requestRefund = RequestRefund.builder()
                .chatId(request.getChatId())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .bankName(request.getBankName())
                .problem(request.getProblem())
                .vmNumber(request.getVmNumber())
                .sum(request.getSum())
                .pruduct(request.getPruduct())
                .location(request.getLocation())
                .cardNumber(request.getCardNumber())
                .reason(request.getReason())
                .requestPhoto(request.getRequestPhoto())
                .requestDocument(request.getRequestDocument())
                .build();
        requestRepository.save(requestRefund);
        logger.debug("Заявка для chatId = " + request.getChatId() + " сохранена");
    }

    @Override
    public List<RequestRefund> findAll() {
        return null;
    }

    @Override
    public RequestRefund findById() {
        return null;
    }

    @Override
    public void delete(Long id) {

    }
}
