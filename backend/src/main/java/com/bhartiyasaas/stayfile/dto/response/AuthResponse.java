package com.bhartiyasaas.stayfile.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;

    private AuthUserResponse user;

    @Builder.Default
    private String tokenType = "Bearer";

}
