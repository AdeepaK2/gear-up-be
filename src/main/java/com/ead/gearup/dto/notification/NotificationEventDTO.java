package com.ead.gearup.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEventDTO {
    
    private Long id;
    private String title;
    private String message;
    private String type;
    private String timestamp;
}
