package lk.ijse.dep9.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnItemDTO implements Serializable {
    private Integer issueNoteId;
    private String isbn;
}
