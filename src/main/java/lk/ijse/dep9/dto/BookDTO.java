package lk.ijse.dep9.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookDTO implements Serializable {
    private String isbn;
    private String title;
    private String author;
    private Integer copies;
}
