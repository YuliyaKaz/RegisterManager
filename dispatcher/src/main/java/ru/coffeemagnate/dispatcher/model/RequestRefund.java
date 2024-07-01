package ru.coffeemagnate.dispatcher.model;

//import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
//import org.hibernate.annotations.TypeDef;
import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Builder
@Entity
@Table(name="request_refund")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor

public class RequestRefund {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name="chat_id")
    private long chatId;

    @CreationTimestamp
    private Date date;
    @CreationTimestamp
    private Time time;
    @Column(name="first_name")
    private String firstName;
    @Column(name="last_name")
    private String lastName;
    @Column(name="phone_number")
    private String phoneNumber;
    @Column(name="vm_number")
    private String vmNumber;
    @Column(name="bank_name")
    private String bankName;
    @Column(name="card_number")
    private String cardNumber;
    @Column(name="sum")
    private double sum;
    @Column(name="problem")
    private String problem;
    @Column(name="product")
    private String pruduct;
    @Column(name="location")
    private String location;
    @Column(name="reason")
    private String reason;


//    @OneToMany(fetch = FetchType.LAZY)
//    @JoinColumn(name="id")
    @OneToOne
    private RequestPhoto requestPhoto;
//    @OneToMany(fetch = FetchType.EAGER)
//    @PrimaryKeyJoinColumn
//    @Column(name="request_photo_id")
//    private List<RequestPhoto> requestPhoto;
//    private List<RequestPhoto> ListRequestPhoto;
//    @OneToOne
//    private RequestDocument requestDocument;
//    @OneToMany(mappedBy = "id", fetch = FetchType.EAGER)// cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH,
////            CascadeType.REFRESH })
//    private List<RequestPhoto> photoList;
//
//    @OneToMany(mappedBy = "id", fetch = FetchType.EAGER)// cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH,
////            CascadeType.REFRESH })
//    private List<RequestDocument> documentList;
//    @OneTo
//    private RequestPhoto requestPhoto;
    @OneToOne
    private RequestDocument requestDocument;
}
