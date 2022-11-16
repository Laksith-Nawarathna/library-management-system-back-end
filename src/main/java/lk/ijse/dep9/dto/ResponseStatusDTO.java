package lk.ijse.dep9.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


@Data
@NoArgsConstructor
@AllArgsConstructor

public class ResponseStatusDTO implements Serializable {
    private Integer status;
    private String message;
    private String path;
    private Long timestamp;
}
