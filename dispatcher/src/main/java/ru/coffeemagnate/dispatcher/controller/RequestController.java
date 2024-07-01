package ru.coffeemagnate.dispatcher.controller;

import org.springframework.beans.factory.annotation.Autowired;
import ru.coffeemagnate.dispatcher.model.RequestRefund;
import ru.coffeemagnate.dispatcher.service.RequestService;
public class RequestController {
    @Autowired
    RequestService requestService;
    public void save(RequestRefund request){
    };

}
