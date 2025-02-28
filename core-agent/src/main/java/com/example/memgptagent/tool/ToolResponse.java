package com.example.memgptagent.tool;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public record ToolResponse(String status, String message, String time) {

    public static ToolResponse emptyOKStatus() {

        return new ToolResponse("OK", "None", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now()));
    }

    public static ToolResponse contentOKStatus(String content) {

        return new ToolResponse("OK", content, DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now()));
    }
}
