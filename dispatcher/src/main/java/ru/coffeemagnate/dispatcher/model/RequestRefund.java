package ru.coffeemagnate.dispatcher.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.sql.Date;
import java.sql.Time;

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
    @OneToOne
    private RequestPhoto requestPhoto;
    @OneToOne
    private RequestDocument requestDocument;
}
