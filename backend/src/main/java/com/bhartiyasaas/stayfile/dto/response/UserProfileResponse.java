package com.bhartiyasaas.stayfile.dto.response;

import com.bhartiyasaas.stayfile.entity.enums.UserRole;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private UUID profileId;

    private String email;

    private String fullName;

    private String phone;

    private String avatarUrl;

    private UserRole role;

    private UUID organizationId;

    private String organizationName;

    private String organizationSlug;

    private Boolean isActive;
}
