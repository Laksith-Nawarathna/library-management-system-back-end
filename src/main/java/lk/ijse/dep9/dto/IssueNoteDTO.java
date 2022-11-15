package lk.ijse.dep9.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class IssueNoteDTO implements Serializable {
    private Integer id;
    private LocalDate date;
    private String memberId;
    private ArrayList<String> books = new ArrayList<>();
}
