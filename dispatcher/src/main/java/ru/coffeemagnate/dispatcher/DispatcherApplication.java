package ru.coffeemagnate.dispatcher;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.telegram.telegrambots.starter.TelegramBotStarterConfiguration;
import ru.coffeemagnate.dispatcher.repository.RequestRepository;

import static org.springframework.boot.SpringApplication.*;

@SpringBootApplication
@Import({TelegramBotStarterConfiguration.class})
public class DispatcherApplication {
    public static void main(String[] args) {
//        ConfigurableApplicationContext context =
                run(DispatcherApplication.class, args);
//        RequestRepository repository = context.getBean(RequestRepository.class);

    }
}
