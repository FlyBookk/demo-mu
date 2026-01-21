package com.musheng.business.common.service.csv;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * CSV Parse Error
 */
@Data
@Builder
@Schema(description = "CSV Parse Error")
public class CsvParseError {

    @Schema(description = "Row number")
    private Integer rowNumber;

    @Schema(description = "Error field name")
    private String fieldName;

    @Schema(description = "Error field value")
    private String fieldValue;

    @Schema(description = "Error code")
    private String errorCode;

    @Schema(description = "Error message")
    private String errorMessage;

    /**
     * Create field error
     */
    public static CsvParseError fieldError(int rowNumber, String fieldName, String fieldValue, String errorMessage) {
        return CsvParseError.builder()
                .rowNumber(rowNumber)
                .fieldName(fieldName)
                .fieldValue(fieldValue)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Create row error
     */
    public static CsvParseError rowError(int rowNumber, String errorMessage) {
        return CsvParseError.builder()
                .rowNumber(rowNumber)
                .errorMessage(errorMessage)
                .build();
    }
}
