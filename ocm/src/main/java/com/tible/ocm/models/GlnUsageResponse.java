package com.tible.ocm.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlnUsageResponse {

    private String localizationNumber;
    private List<LabelResponse> usedLabels;
}
