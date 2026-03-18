package org.hartford.fireinsurance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceDTO {
    private Long id;
    private String username;
    private Boolean emailEnabled;
    private Set<String> enabledEventKeys = new HashSet<>();
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
}
