package ru.coffeemagnate.dispatcher.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TypeDef;

@Builder
@Entity
@Table(name="request_document")
@Setter
@Getter
//@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@NoArgsConstructor
@AllArgsConstructor

public class RequestDocument {
    @Id@GeneratedValue(strategy= GenerationType.IDENTITY)
    private long id;
    private long chatId;
    private String telegramFileId;
    private String docname;
    @OneToOne
    private BinaryContent binaryContent;
    private String mimeType;
    private long size;


}
