package com.tible.ocm.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.compress.utils.Lists;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcmResponse {

    private OcmStatus status;
    private List<OcmMessage> messages;

    public OcmResponse addMessage(OcmMessage message) {
        if (messages == null || messages.isEmpty()) messages = Lists.newArrayList();
        messages.add(message);
        return this;
    }
}
