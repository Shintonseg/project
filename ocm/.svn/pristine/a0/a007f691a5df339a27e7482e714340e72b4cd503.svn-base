package com.tible.ocm.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OcmTransactionResponse extends OcmResponse {

    private String transactionNumber;

    public OcmTransactionResponse(OcmStatus status, List<OcmMessage> messages, String transactionNumber) {
        super(status, messages);
        this.transactionNumber = transactionNumber;
    }
}
