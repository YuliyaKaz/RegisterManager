package ru.coffeemagnate.dispatcher.service;

import lombok.extern.log4j.Log4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import ru.coffeemagnate.dispatcher.model.BinaryContent;
import ru.coffeemagnate.dispatcher.model.RequestDocument;
import ru.coffeemagnate.dispatcher.model.RequestPhoto;
import ru.coffeemagnate.dispatcher.repository.BinaryContentRepository;
import ru.coffeemagnate.dispatcher.repository.DocumentRepository;
import org.springframework.http.HttpHeaders;
import ru.coffeemagnate.dispatcher.repository.PhotoRepository;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileServiceImpl implements FileService{
    @Value("${bot.token}")
    private String token;
    @Value("${service.file_info.url}")
    private String fileInfoUrl;
    @Value("${service.file_storage.url}")
    private String fileStorageUrl;
    static final Logger logger = LogManager.getLogger(FileServiceImpl.class);
    private DocumentRepository documentRepository;
    private PhotoRepository photoRepository;
    private BinaryContentRepository binaryContentRepository;

    public FileServiceImpl(DocumentRepository documentRepository,
                           PhotoRepository photoRepository,
                           BinaryContentRepository binaryContentRepository) {
        this.documentRepository = documentRepository;
        this.photoRepository = photoRepository;
        this.binaryContentRepository = binaryContentRepository;
    }

    @Override
    public RequestDocument processDoc(Message message) {

        String fileId = message.getDocument().getFileId();
        ResponseEntity<String> response = getFilePath(fileId);
        if (response.getStatusCode() == HttpStatus.OK){

            BinaryContent persistantBinaryContent = getPersistantBinaryContent(response);
            Document telegramDocument = message.getDocument();
            RequestDocument transientDocument = buildTransientDocument(telegramDocument,persistantBinaryContent);
            transientDocument.setChatId(message.getChatId());

            logger.info("Документ " + telegramDocument.getFileName() +" загружен для chatId = " + message.getChatId());
            return documentRepository.save(transientDocument);
        } else {
            logger.info("Не удалось загрузить документ для chatId = " + message.getChatId());
        }
        return null;
    }
    @Override
    public RequestPhoto processPhoto(Message message) {
        //todo пока обрабатывается одно фото

        List<RequestPhoto> listPhoto = new ArrayList<>();
        PhotoSize photo = message.getPhoto().get(0);
        String fileId = photo.getFileId();
        ResponseEntity<String> response = getFilePath(fileId);
        if (response.getStatusCode() == HttpStatus.OK){
            BinaryContent persistantBinaryContent = getPersistantBinaryContent(response);
            RequestPhoto transientPhoto = buildTransientPhoto(photo,persistantBinaryContent);
            transientPhoto.setChatId(message.getChatId());
            transientPhoto.setTelegramFileId(fileId);
            listPhoto.add(transientPhoto);
            logger.info("Документ " + photo.getFileId() +" загружен для chatId = " + message.getChatId());
            return photoRepository.save(transientPhoto);
        } else {
            logger.info("Не удалось загрузить документ для chatId = " + message.getChatId());
        }
        return null;
    }
    private RequestPhoto buildTransientPhoto(PhotoSize telegramPhoto, BinaryContent persistantBinaryContext) {
        return RequestPhoto.builder()
                .telegramFileId(telegramPhoto.getFileId())
                .binaryContent(persistantBinaryContext)
                .size(telegramPhoto.getFileSize())
                .build();
    }

    private BinaryContent getPersistantBinaryContent(ResponseEntity<String> response) {
        String filePath = getFilePath(response);
        byte[] fileByte = loadFile(filePath);

        BinaryContent transientBinaryContent = BinaryContent.builder().arraysOfBytes(fileByte).build();
        BinaryContent persistantBinaryContent = binaryContentRepository.save(transientBinaryContent);
        return persistantBinaryContent;
    }


    private String getFilePath(ResponseEntity<String> response) {
        JSONObject jsonObject = new JSONObject(response.getBody());
        return String.valueOf(jsonObject.getJSONObject("result").getString("file_path"));
    }

    private RequestDocument buildTransientDocument(Document telegramDocument, BinaryContent persistantBinaryContext) {
        return RequestDocument.builder()
                .telegramFileId(telegramDocument.getFileId())
                .docname(telegramDocument.getFileName())
                .binaryContent(persistantBinaryContext)
                .mimeType(telegramDocument.getMimeType())
                .size(telegramDocument.getFileSize())
                .build();
    }

    private byte[] loadFile(String filePath) {
        String fullUrl = fileStorageUrl.replace("{bot.token}",token)
                .replace("{filePath}",filePath);
        URL urlObject = null;
        try {
            urlObject = new URL(fullUrl);
        } catch (MalformedURLException ex) {
            logger.info("Загрузка документа не удалась для " + filePath);
        }

        try (InputStream is = urlObject.openStream()) {
            return is.readAllBytes();
        }catch (IOException ex) {
            logger.info("Считывание бинарного файла не удалось для " + filePath);
        }
        return null;
    }

    private ResponseEntity<String> getFilePath(String fileId) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        HttpEntity<String> request = new HttpEntity<>(httpHeaders);
        return restTemplate.exchange(
                fileInfoUrl,
                HttpMethod.GET,
                request,
                String.class,
                token,
                fileId );
    }
    @Override
    public void saveDocument(RequestDocument document){
        documentRepository.save(document);
        logger.debug("Документ " + document.getDocname() + " успешно сохранен");
    }

    @Override
    public void savePhoto(RequestPhoto photo){
        photoRepository.save(photo);
        logger.debug("Фото " + photo.getId() + " успешно сохранен");
    }
}
