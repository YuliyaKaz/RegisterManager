package ru.coffeemagnate.dispatcher.service;

// Состояния бота
public enum UserState {
        START,
        SEND_PROBLEM,
        SEND_FEEDBACK,
        FEEDBACK_SENDED,
        VENDING_NUMBER,
        GET_LOCATION,
        GET_LOCATION_AUTOMATICALLY,
        DESCRIBE_PROBLEM,
        GET_DESCRIPTION_PROBLEM,
        DESCRIBE_PRODUCT,
        GET_AMOUNT,
        RETURN_MONEY,
        MOBILE_PHONE,
        REQUEST_STATE,
        ADD_BANK_NAME,
        ADD_CARD,
        SEND_CARD,
        NOTHING,

        CHECK_AUTOMAT_NUMBER,
        PRESS_AGAIN,
        THANKS
}
