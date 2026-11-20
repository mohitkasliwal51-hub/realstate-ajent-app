package com.bhartiyasaas.stayfile.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class BankRpdInitiateRequest {
    @Size(max = 20)
    private String phone;

    @Size(max = 200)
    private String name;

    @Size(max = 100)
    private String email;

    @Size(max = 2048)
    private String redirectUrl;

    private UUID landlordId;
}
