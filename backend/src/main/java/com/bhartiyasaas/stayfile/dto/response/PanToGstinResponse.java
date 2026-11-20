package com.bhartiyasaas.stayfile.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PanToGstinResponse {
    private String pan;
    private String verificationId;
    private String status;
    private List<GstinRecord> gstins;
    private String referenceId;
    private boolean linkedToLandlord;

    @Data
    @Builder
    public static class GstinRecord {
        private String gstin;
        private String status;
        private String state;
    }
}
