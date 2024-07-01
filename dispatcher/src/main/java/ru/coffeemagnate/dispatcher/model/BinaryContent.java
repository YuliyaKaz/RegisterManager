package ru.coffeemagnate.dispatcher.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="binary_content")
public class BinaryContent {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private long id;
    private byte[] arraysOfBytes;
}
