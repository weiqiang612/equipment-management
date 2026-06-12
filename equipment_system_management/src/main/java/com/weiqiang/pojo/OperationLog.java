package com.weiqiang.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationLog {
    private Integer id;
    private String operator;
    private Integer operatorRole;
    private String opType;
    private String targetType;
    private String targetId;
    private LocalDateTime opTime;
    private String summary;
    private Integer status;
    private String errorMsg;
}
