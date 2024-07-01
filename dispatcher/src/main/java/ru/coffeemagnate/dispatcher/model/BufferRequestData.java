package ru.coffeemagnate.dispatcher.model;
import jakarta.persistence.PostPersist;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.telegram.telegrambots.meta.api.objects.Location;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import ru.coffeemagnate.dispatcher.controller.RequestController;
import ru.coffeemagnate.dispatcher.repository.RequestRepository;
import ru.coffeemagnate.dispatcher.service.RequestService;
//import ru.coffeemagnate.storage.model.RequestRefund;
//import ru.coffeemagnate.storage.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
@Setter
@Getter
//@EnableJpaRepositories
public class BufferRequestData {
    @Autowired
    RequestController requestController;

    private LocalDateTime datatime;
    private long chatid;
    private String firstName; //имя
    private String lastName; //фамилия
    private String phoneNumber; //номер телефона клиента
    private String vendingNumber;//номер автомата
    private final Map<Long, String> oneWordFromChat = new LinkedHashMap<>();
    private String bank;//наименование банка получателя
    private String cardNumber; //номер карты для перевода
    private double money; //количество денег, которые потратил клиент
    private String problem; //описание проблемы
    private String suggestion; //предложение
    private Location location;
    private String locationMessage; //локация словами
    private String product;
    private List<PhotoSize> photo;
    @Autowired
    private RequestRepository requestRepository;
    private RequestService requestService;
    public BufferRequestData() {};
}
