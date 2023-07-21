package com.tible.ocm.models.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document
public class OAuthClient {

    @Id
    private String id;
    @Indexed(unique = true)
    private String clientId;
    private String clientSecret;
    private String resourceIds;
    private String scope;
    private String authorizedGrantTypes;
    private String webServerRedirectUri;
    private String authorities;
    private Integer accessTokenValidity;
    private Integer refreshTokenValidity;
    private String additionalInformation;
    private String autoApprove;
    private String url;
    private String rvmOwnerNumber;
    private String version;
    private String type;

    // OauthClient is only for one RvmMachine except the tible admin client details
    @DBRef
    private List<RvmMachine> rvmMachines;

}
