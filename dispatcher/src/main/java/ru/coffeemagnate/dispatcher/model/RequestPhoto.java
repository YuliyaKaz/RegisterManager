package ru.coffeemagnate.dispatcher.model;

import jakarta.persistence.*;
import lombok.*;

@Builder
@Entity
@Table(name="request_photo")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor

public class RequestPhoto {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(unique=true)
    private long id;
    private long chatId;
    private String telegramFileId;
    @OneToOne
    private BinaryContent binaryContent;
    private Integer size;
}
