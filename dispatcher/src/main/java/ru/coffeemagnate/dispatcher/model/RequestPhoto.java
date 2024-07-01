package ru.coffeemagnate.dispatcher.model;

//import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
//import org.hibernate.annotations.TypeDef;

@Builder
@Entity
@Table(name="request_photo")
@Setter
@Getter
//@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
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
