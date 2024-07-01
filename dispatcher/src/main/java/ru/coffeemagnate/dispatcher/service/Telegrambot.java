package ru.coffeemagnate.dispatcher.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.coffeemagnate.dispatcher.model.RequestDocument;
import ru.coffeemagnate.dispatcher.model.RequestPhoto;
import ru.coffeemagnate.dispatcher.model.RequestRefund;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static ru.coffeemagnate.dispatcher.service.UserState.SEND_PROBLEM;

@Service
@Component
public class Telegrambot extends TelegramLongPollingBot{
    static final Logger logger = LogManager.getLogger(Telegrambot.class);
    @Value("${bot.name}")
    private String botName;
    @Value("${bot.token}")
    private String botToken;
    @Value("${service.location.url}")
    private String locationUrl;
    @Value("${yandex.token}")
    private String yandexToken;

    private final Map<Long, String> oneWordFromChat = new LinkedHashMap<>();
    private final Map<Long, UserState> userStates = new LinkedHashMap<>();
    private RequestRefund request;
    @Autowired
    private RequestService requestService;
    @Autowired
    private FileServiceImpl fileService;
    private String getUserFirstName(Message msg) {
        User user = msg.getFrom();
        String userName = user.getUserName();
        return (userName != null) ? userName : user.getFirstName();
    }
    private String getUserLastName(Message msg) {
        User user = msg.getFrom();
        String userName = user.getUserName();
        return (userName != null) ? userName : user.getLastName();
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText(); //
            long chatId = update.getMessage().getChatId(); // уникальный id гостя

            // Принудительный выход
            List<String> flugegeheimen = List.of("стоп", "/стоп", "stop", "/stop");

            if (flugegeheimen.contains(messageText.toLowerCase())) {
                userStates.remove(chatId); // Сбрасываем состояние бота
                sendAnswerMessage(sendMessage(chatId, "Действие отменено"));
                logger.info("Действие отменено для " + chatId + " ");

            } else {
                /* Основные команды */
                switch (messageText) {
                    default -> undefinedKeyboard(chatId, messageText, update);
                }
            }
        }
        else if (update.hasMessage() && update.getMessage().hasDocument()) {
            long chatId = update.getMessage().getChatId();
            logger.debug("Добавлен документ для chatId = " + chatId);
            String messageFromUser = "";
            if (update.hasMessage() && update.getMessage().hasText()) {
                messageFromUser = update.getMessage().getText();
            }
            appendDocument(chatId, update, messageFromUser);

        }
        else if (update.hasMessage() && update.getMessage().hasContact()) {
            long chatId = update.getMessage().getChatId();
            logger.debug("Добавлен номер телефона для chatId = " + chatId);
            setMobilePhone(chatId, update);
        }
        else if (update.hasMessage() && update.getMessage().hasPhoto()) {
            long chatId = update.getMessage().getChatId();
            logger.debug("Добавлено фото для chatId = " + chatId);
            appendPhoto(chatId, update, update.getMessage().getText());

        } else if (update.hasMessage() && update.getMessage().hasLocation()) {
            long chatId = update.getMessage().getChatId();
            logger.debug("Добавлена локация для chatId = " + chatId);
            addLocation(chatId,update.getMessage().getText(), update);

            describeProblem(chatId, update.getMessage().getText());
        }
        else if (update.hasCallbackQuery()) {
            logger.debug("Нажата кнопка");
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getFrom().getId();

            switch (callbackData) {
                case "BUTTON_PROBLEM" -> addProblem(chatId, "Problem button is pressed");
                case "BUTTON_SUGGESTION" -> addSuggestion(chatId, "Suggestion button is pressed");
                case "BUTTON_NOT_NUMBER" -> getLocationAutomatically(chatId, "get location automatically");
                case "BUTTON_MOBILE" -> addMobilePayment(chatId, "Payment to a mobile phone number",update);
                case "BUTTON_BANK_CARD" -> addBankCard(chatId,"payment to a bank card by mobile phone number");
                case "BUTTON_CARD" -> addCard(chatId, "payment to a bank card");
                case "BUTTON_NOTHING" -> sendFeedback(chatId, "send feedback");
                case "BUTTON_ADVICE_HELPED" -> thankForContacting(chatId, "Thank you for contacting us");
                case "BUTTON_ADVICE_NOT_HELPED" -> describeProblem(chatId,"advice not helped");
            }
        } else {
            long chatId = update.getMessage().getChatId();
            logger.info("Такое сообщение обработать не можем для chatId = " + chatId);
            sendAnswerMessage(sendMessage(chatId,"Формат сообщения не предусмотрен, попробуйте еще раз"));
        }
    }
    private void appendPhoto(long chatId, Update update, String word) {
        UserState lastState = getLastState(chatId);
        logger.debug("Попытка загрузки фотографии для chatId = " + chatId);

        switch (lastState) {
            case SEND_FEEDBACK -> { request.setReason("Suggestion button is pressed"); request.setProblem("Add photo");}
            default -> request.setProblem(word);
        }
        try {
            RequestPhoto photo = fileService.processPhoto(update.getMessage());
            request.setRequestPhoto(photo);
            sendAnswerMessage(sendMessage(chatId,"Фотография загружена"));
        } catch (Exception ex) {
            logger.info("Загрузка фото не удалась для chatId = " + chatId);
            sendAnswerMessage(sendMessage(chatId,"К сожалению, загрузка фото не удалась. Повторите попытку позже"));
        }
        switch (lastState) {
            case SEND_FEEDBACK -> sendFeedback(chatId, word);
        }
    }

    private void appendDocument(long chatId, Update update, String word) {
        UserState lastState = getLastState(chatId);
        switch (lastState) {
            case SEND_FEEDBACK -> {request.setReason("Suggestion button is pressed"); request.setProblem("Add photo");}
            default -> request.setProblem(word);
        }
        logger.debug("Попытка загрузки документа для chatId = ");

        try {
            RequestDocument document = fileService.processDoc(update.getMessage());
            request.setRequestDocument(document);
            fileService.saveDocument(document);
            sendAnswerMessage(sendMessage(chatId,"Документ загружен"));
        } catch (Exception ex) {
            logger.info("Попытка загрузки файла не удалась для chatId =" + chatId);
            sendAnswerMessage(sendMessage(chatId,"К сожалению, загрузка файла не удалась. Повторите попытку позже"));
        }
        switch (lastState) {
            case SEND_FEEDBACK -> sendFeedback(chatId, word);
        }
    }

    private void thankForContacting(long chatId, String word) {
        oneWordFromChat.put(chatId, word);
        userStates.put(chatId, UserState.THANKS);
        sendAnswerMessage(sendMessage(chatId,"Спасибо за обращение, мы рады, что смогли вам оперативно помочь"));
        logger.debug("Спасибо за обращение для chatId = " + chatId);
        finishDialog(chatId);
    }
    private void addCard(long chatId, String word) {
        oneWordFromChat.put(chatId, word);
        userStates.put(chatId, UserState.ADD_CARD);
        request.setReason(word);
        logger.debug("Попытка добавить карту для chatId = " + chatId);
        sendAnswerMessage(sendMessage(chatId,"Напишите номер карты 16 цифр "));
    }
    private void addBankCard(long chatId, String word) {
        oneWordFromChat.put(chatId, word);
        userStates.put(chatId, UserState.ADD_BANK_NAME);
        request.setReason(word);
        request.setReason(word);
        logger.debug("Решение для chatId = " + chatId + " word");
        sendAnswerMessage(sendMessage(chatId,"Напишите банк получателя"));
    }

    private void setMobilePhone(long chatId, Update update) {
        String mobilePhone = "";
        try {
            mobilePhone = update.getMessage().getContact().getPhoneNumber();
            logger.debug("Номер мобильного телефона для chatId = " + chatId + " опререлен: " + mobilePhone);
        } catch (Exception e) {
            sendAnswerMessage(sendMessage(chatId, "Не удалось определить номер мобильного телефона"));
            logger.info("Не удалось определить номер мобильного телефона для для chatId = " + chatId);
        }
        request.setPhoneNumber(mobilePhone);
        userStates.put(chatId,UserState.MOBILE_PHONE);
        requestCheckAutomate(chatId);
    }

    private void requestCheckAutomate(long chatId) {
        userStates.put(chatId,UserState.REQUEST_STATE);
        logger.debug("Спасибо за обращение для chatId = " + chatId);
        sendAnswerMessage(sendMessage(chatId,"Спасибо за обращение, проверка автомата и перевод денег производится в течение суток"));
        finishDialog(chatId);
    }
    private void getMobile(long chatId, String word) {
        var response = new SendMessage();
        response.setChatId(chatId);
        response.setText("Let's started");
//        request.setReason(word);
        request.setBankName(word);

        // Создаем клавиуатуру
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        response.setReplyMarkup(replyKeyboardMarkup);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);

        // Создаем список строк клавиатуры
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Первая строчка клавиатуры
        KeyboardRow keyboardFirstRow = new KeyboardRow();
        // Добавляем кнопки в первую строчку клавиатуры
        KeyboardButton keyboardButton = new KeyboardButton();

        keyboardButton.setText("Share your number >");
        keyboardButton.setRequestContact(true);
        keyboardFirstRow.add(keyboardButton);

        // Добавляем все строчки клавиатуры в список
        keyboard.add(keyboardFirstRow);
        // и устанваливаем этот список нашей клавиатуре
        replyKeyboardMarkup.setKeyboard(keyboard);
        sendAnswerMessage(sendMessage(chatId, "Поделитесь номером телефона", replyKeyboardMarkup));
    }

    private UserState getLastState(long chatId) {
        if (userStates.size() == 0) {
            return UserState.START;
        }
        return userStates.get(chatId);
    }

    private void undefinedKeyboard(long chatId, String word, Update update) {
        oneWordFromChat.put(chatId, word);
        UserState lastState = getLastState(chatId);

        switch (lastState) {
            case START -> startDialog(chatId, update.getMessage());
            case SEND_FEEDBACK -> sendFeedback(chatId, word);
            case FEEDBACK_SENDED -> finishDialog(chatId);
            case SEND_PROBLEM -> checkAutomatNumber(chatId, word);
            case CHECK_AUTOMAT_NUMBER -> setVendingAutomatNumber(chatId, word);
            case GET_LOCATION_AUTOMATICALLY -> addLocation(chatId,word,update);
            case GET_LOCATION -> getDescriptionProblem(chatId, word);
            case DESCRIBE_PROBLEM -> getDescriptionProblem(chatId, word);
            case GET_DESCRIPTION_PROBLEM -> getDescriptionProduct(chatId,word);
            case DESCRIBE_PRODUCT -> getAmount(chatId, word);
            case GET_AMOUNT -> getReturnMoney(chatId, word);
            case ADD_BANK_NAME -> getMobile(chatId, word);
            case ADD_CARD -> getCard(chatId,word);
            case VENDING_NUMBER -> checkAutomatNumber(chatId, word);
        }
    }

    private void checkAutomatNumber(long chatId, String word) {
        oneWordFromChat.put(chatId,word);
        int automatNumber = 0;
        try {
            automatNumber = Integer.parseInt(word.trim());
            logger.debug("Определен номер телефона для chatId = " + chatId);
        } catch (Exception e) {
            logger.info("Не удалось определить номер телефона для chatId = " + chatId);
            sendAnswerMessage(sendMessage(chatId,"Что то не так с номером. Попробуйте еще раз"));
        }
        request.setVmNumber(String.valueOf(automatNumber));
        userStates.put(chatId,UserState.CHECK_AUTOMAT_NUMBER);
        if (automatNumber >= 200 && automatNumber < 300 ) {
            describeProblem(chatId, word);
        }else if (automatNumber >= 300 && automatNumber <400) {
            pressAgain(chatId, word);
        } else if (automatNumber >= 400 && automatNumber < 500) {
            describeProblem(chatId, word);
        } else {
            logger.debug("Введен неверный номер автомата " + automatNumber + "для chatId = " + chatId );
            sendAnswerMessage(sendMessage(chatId,"Неверный номер автомата, попробуйте еще раз"));
            userStates.put(chatId,UserState.VENDING_NUMBER);
        }
    }
    private void pressAgain(long chatId, String word) {
        oneWordFromChat.put(chatId,word);
        userStates.put(chatId,UserState.PRESS_AGAIN);
        InlineKeyboardButton inlineKeyboardButton1 = new InlineKeyboardButton();
        InlineKeyboardButton inlineKeyboardButton2 = new InlineKeyboardButton();
        inlineKeyboardButton1.setText("Совет помог");
        inlineKeyboardButton1.setCallbackData("BUTTON_ADVICE_HELPED");
        inlineKeyboardButton2.setText("Совет не помог");
        inlineKeyboardButton2.setCallbackData("BUTTON_ADVICE_NOT_HELPED");
        List<InlineKeyboardButton> keyboardButtonsRow = new ArrayList<>();
        keyboardButtonsRow.add(inlineKeyboardButton1);
        keyboardButtonsRow.add(inlineKeyboardButton2);

        sendAnswerMessage(sendMessage(chatId, "Нажмите, пожалуйста, еще раз на кнопку напитка",
                InlineKeyboardMarkup.builder().keyboardRow(keyboardButtonsRow).build()));//   maker(floor1, floor2));)

    }
    private void getCard(long chatId, String word) {

        if (word.trim().length() == 16 || word.trim().length() == 13 || word.trim().length() == 19) {
            request.setCardNumber(word.trim());
            userStates.put(chatId,UserState.SEND_CARD);
            logger.debug("Определен номер карты " + word.trim() + "для chatId = " + chatId);
        } else {
            logger.debug("Номер банковской карты " + word + " не соответствует ожидаемому для chatId = " + chatId);
            sendAnswerMessage(sendMessage(chatId,"Номер карты должен быть 16, 13 или 19 цифр"));
            return;
        }
        requestCheckAutomate(chatId);
    }

    private void startDialog(long chatId,Message message) {
        logger.info("Начало диалога для chatId = " + chatId);
        userStates.put(chatId,UserState.START);
        request = new RequestRefund();
        request.setChatId(chatId);
        request.setFirstName(getUserFirstName(message));
        request.setFirstName(getUserLastName(message));

        InlineKeyboardButton inlineKeyboardButton1 = new InlineKeyboardButton();
        InlineKeyboardButton inlineKeyboardButton2 = new InlineKeyboardButton();
        inlineKeyboardButton1.setText("У меня проблема с автоматом");
        inlineKeyboardButton1.setCallbackData("BUTTON_PROBLEM");
        inlineKeyboardButton2.setText("У меня есть предложение");
        inlineKeyboardButton2.setCallbackData("BUTTON_SUGGESTION");
        List<InlineKeyboardButton> keyboardButtonsRow = new ArrayList<>();
        keyboardButtonsRow.add(inlineKeyboardButton1);
        keyboardButtonsRow.add(inlineKeyboardButton2);

        sendAnswerMessage(sendMessage(chatId, "Добрый день, чем я могу помочь? " + "\uD83E\uDDD0",
                InlineKeyboardMarkup.builder().keyboardRow(keyboardButtonsRow).build()));//   maker(floor1, floor2));)
    }
    private void finishDialog(long chatId) {
        userStates.remove(chatId);
        requestService.save(request);
        logger.info("Диалог завершен для chatId = " + chatId);
    }

    private void sendFeedback(long chatId,String word) {
        userStates.put(chatId,UserState.FEEDBACK_SENDED);
        request.setProblem(word);
        sendAnswerMessage(sendMessage(chatId, "Спасибо за обращение, мы его обязательно рассмотрим"));
        finishDialog(chatId);
    }
    private void setVendingAutomatNumber(long chatId, String word) {
        userStates.put(chatId,UserState.VENDING_NUMBER);

       // request.setVmNumber(String.valueOf(word));
        int number = 0;
        try {
            number = Integer.parseInt(word.trim());
        } catch (Exception e) {
            logger.debug("Номер автомата введен некорректно для chatId = " + chatId);
            sendAnswerMessage(sendMessage(chatId, "Введите номер корректно"));
        }
        request.setVmNumber(word.trim());
        describeProblem(chatId, word);
    }
    private void describeProblem(long chatId, String textMessage) {
        oneWordFromChat.put(chatId,textMessage);
        userStates.put(chatId, UserState.DESCRIBE_PROBLEM);
        sendAnswerMessage(sendMessage(chatId,"Опишите проблему"));
    }

    private SendMessage sendMessage(long chatId, String textMessage) {
        var response = new SendMessage();
        response.setChatId(chatId);
        response.setText(textMessage);
        return response;
    }

    private SendMessage sendMessage(long chatId, String textMessage, ReplyKeyboard replyMarkup) {
        var response = new SendMessage();
        response.setChatId(chatId);
        response.setText(textMessage);
        response.setReplyMarkup(replyMarkup);
        return response;
    }

    private void sendAnswerMessage(SendMessage message) {
        if (message != null) {
            try {
                execute(message);
            } catch (TelegramApiException e) {
                logger.error("Невозможно отправить сообщения в телеграмм по причине " + e.getMessage());
            }
        }
    }

    private void addProblem(long chatId, String word) {
        userStates.put(chatId, SEND_PROBLEM);

        InlineKeyboardButton inlineKeyboardButton1 = new InlineKeyboardButton();
        inlineKeyboardButton1.setText("Нет номера");
        inlineKeyboardButton1.setCallbackData("BUTTON_NOT_NUMBER");
        List<InlineKeyboardButton> keyboardButtonsRow = new ArrayList<>();
        keyboardButtonsRow.add(inlineKeyboardButton1);

        sendAnswerMessage(sendMessage(chatId, "Напишите номер автомата ",
                InlineKeyboardMarkup.builder().keyboardRow(keyboardButtonsRow).build()));
    }

    private void addSuggestion(long chatId, String word) {
        userStates.put(chatId,UserState.SEND_FEEDBACK);
        request.setReason(word);
        sendAnswerMessage(sendMessage(chatId, "Опишите ваше предложение"));
    }
    private void addLocation(long chatId, String word, Update update) {
        userStates.put(chatId, UserState.GET_LOCATION);
        String locationFromUrl = "";

        try {
            Double longitude = update.getMessage().getLocation().getLongitude();
            Double latitude = update.getMessage().getLocation().getLatitude();
            locationFromUrl = getLocationByUrl(String.valueOf(latitude),String.valueOf(longitude));
            sendAnswerMessage(sendMessage(chatId,"Определено местоположение: " + locationFromUrl));
            request.setLocation(locationFromUrl);
            logger.debug("Определено местоположение " + locationFromUrl + " для chatId = " + chatId);
        } catch (Exception e) {
            logger.info("Не удалось определить местоположение в автоматическом режиме для chatId = " + chatId);
        }
        if (locationFromUrl.isEmpty()) {
            sendAnswerMessage(sendMessage(chatId, "Не удалось определить локацию в автоматическом режиме \n" +
                    "Отправьте свою геолокацию "));
        }
    }

    private String getLocationByUrl(String latitude, String longitude) {
        String fullUrl = locationUrl.replace("{yandex.token}",yandexToken)
                .replace("{latitude}",String.valueOf(latitude))
                .replace("{longitude}",String.valueOf(longitude));
        URL urlObject = null;
        try {
            urlObject = new URL(fullUrl);
        } catch (MalformedURLException ex) {
            logger.info("Не удалось определить местоположение по координатам для latitude:" + latitude + " longitude" + longitude );
        }

        try (InputStream is = urlObject.openStream()) {
            ResponseEntity<String> response = getlocationRequest(fullUrl);
            JSONObject jsonObject = new JSONObject(response.getBody());
            return jsonObject.getJSONObject("response")
                    .getJSONObject("GeoObjectCollection")
                    .getJSONArray("featureMember")
                    .getJSONObject(0)
                    .getJSONObject("GeoObject")
                    .getJSONObject("metaDataProperty")
                    .getJSONObject("GeocoderMetaData").getString("text");
        }catch (IOException ex) {
            logger.info("Не удалось преобразовать местоположение по координатам из формата json для latitude:" + latitude + " longitude" + longitude);
        }
        return null;
    }
    private ResponseEntity<String> getlocationRequest(String fullUrl) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        HttpEntity<String> request = new HttpEntity<>(httpHeaders);
        return restTemplate.exchange(
                fullUrl,
                HttpMethod.GET,
                request,
                String.class );
    }

    private void getLocationAutomatically(long chatId, String word) {
        userStates.put(chatId, UserState.GET_LOCATION_AUTOMATICALLY);
        var response = new SendMessage();
        response.setChatId(chatId);
        response.setText("Let's started");

        // Создаем клавиуатуру
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        response.setReplyMarkup(replyKeyboardMarkup);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);

        // Создаем список строк клавиатуры
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Первая строчка клавиатуры
        KeyboardRow keyboardFirstRow = new KeyboardRow();
        // Добавляем кнопки в первую строчку клавиатуры
        KeyboardButton keyboardButton = new KeyboardButton();

        keyboardButton.setText("Share your location >");
        keyboardButton.setRequestLocation(true);
        keyboardFirstRow.add(keyboardButton);

        // Добавляем все строчки клавиатуры в список
        keyboard.add(keyboardFirstRow);
        // и устанваливаем этот список нашей клавиатуре
        replyKeyboardMarkup.setKeyboard(keyboard);
        sendAnswerMessage(sendMessage(chatId, "Поделитесь своей геолокацией", replyKeyboardMarkup));
    }
    private void getDescriptionProblem(long chatId, String message) {
        request.setProblem(message);
        oneWordFromChat.put(chatId,message);
        userStates.put(chatId, UserState.GET_DESCRIPTION_PROBLEM);

        sendAnswerMessage(sendMessage(chatId, "Напишите название напитка или товара, который вы брали "));
    }
    private void getDescriptionProduct(long chatId, String message) {
        oneWordFromChat.put(chatId,message);
        userStates.put(chatId, UserState.DESCRIBE_PRODUCT);
        request.setPruduct(message);

        sendAnswerMessage(sendMessage(chatId, "Сколько денег вы потратили?"));
    }
    private void getAmount(long chatId, String message) {
        oneWordFromChat.put(chatId, message);
        userStates.put(chatId, UserState.GET_AMOUNT);
        try {
            request.setSum(Double.parseDouble(message.trim()));
        } catch (Exception e) {
            sendAnswerMessage(sendMessage(chatId,"Не удалось распознать сумму. Введите еще раз"));
            logger.debug("Не удалось распознать сумму покупки для chatId = " + chatId);
        }
        getReturnMoney(chatId, message);
    }
    private void addMobilePayment(long chatId, String message, Update update) {
        oneWordFromChat.put(chatId, message);
        userStates.put(chatId, UserState.MOBILE_PHONE);
        request.setReason(message);
        try {
            request.setPhoneNumber(update.getMessage().getText());
        } catch (Exception e) {
            logger.info("Не удалось определить номер телефона для chatId = " + chatId);
        }
        getMobile(chatId, message);
    }

    private void getReturnMoney(long chatId, String message) {
        oneWordFromChat.put(chatId, message);
        userStates.put(chatId, UserState.RETURN_MONEY);

        InlineKeyboardButton inlineKeyboardButton1 = new InlineKeyboardButton();
        InlineKeyboardButton inlineKeyboardButton2 = new InlineKeyboardButton();
        InlineKeyboardButton inlineKeyboardButton3 = new InlineKeyboardButton();
        InlineKeyboardButton inlineKeyboardButton4 = new InlineKeyboardButton();
        inlineKeyboardButton1.setText("На счет мобильного номера телефона");
        inlineKeyboardButton1.setCallbackData("BUTTON_MOBILE");
        inlineKeyboardButton2.setText("На карту банка по номеру телефона");
        inlineKeyboardButton2.setCallbackData("BUTTON_BANK_CARD");
        inlineKeyboardButton3.setText("По номеру карты");
        inlineKeyboardButton3.setCallbackData("BUTTON_CARD");
        inlineKeyboardButton4.setText("Мне не нужно ничего возвращать");
        inlineKeyboardButton4.setCallbackData("BUTTON_NOTHING");
        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
        keyboardButtonsRow1.add(inlineKeyboardButton1);
        List<InlineKeyboardButton> keyboardButtonsRow2 = new ArrayList<>();

        keyboardButtonsRow2.add(inlineKeyboardButton2);
        List<InlineKeyboardButton> keyboardButtonsRow3 = new ArrayList<>();
        keyboardButtonsRow3.add(inlineKeyboardButton3);
        List<InlineKeyboardButton> keyboardButtonsRow4 = new ArrayList<>();
        keyboardButtonsRow4.add(inlineKeyboardButton4);
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        rowsInline.add(keyboardButtonsRow1);
        rowsInline.add(keyboardButtonsRow2);
        rowsInline.add(keyboardButtonsRow3);
        rowsInline.add(keyboardButtonsRow4);

        sendAnswerMessage(sendMessage(chatId, "Куда вернуть деньги? ",
                InlineKeyboardMarkup.builder().keyboard(rowsInline).build()));
    }
}
