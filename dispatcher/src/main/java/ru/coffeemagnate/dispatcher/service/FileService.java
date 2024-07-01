package ru.coffeemagnate.dispatcher.service;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.coffeemagnate.dispatcher.model.RequestDocument;
import ru.coffeemagnate.dispatcher.model.RequestPhoto;

import java.util.List;

public interface FileService {
    RequestDocument processDoc(Message message);
    RequestPhoto processPhoto(Message message);
    void saveDocument(RequestDocument document);
    void savePhoto(RequestPhoto photo);

}
