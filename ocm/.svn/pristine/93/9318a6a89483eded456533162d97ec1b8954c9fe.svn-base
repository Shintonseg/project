package com.tible.ocm.dto;

import com.tible.ocm.models.mongo.OAuthClient;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.thymeleaf.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class OAuthClientDto {

    private String id;
    private String clientId;
    private String clientSecret;
    private List<String> scope;
    private String resourceIds;
    private String authorizedGrantTypes;
    private Integer accessTokenValidity;
    private Integer refreshTokenValidity;
    private String rvmOwnerNumber;
    private String version;
    private String type;
    private List<RvmMachineDto> rvmMachines;

    private OAuthClientDto(OAuthClient oauthClient) {
        this.id = oauthClient.getId();
        this.clientId = oauthClient.getClientId();
        this.clientSecret = oauthClient.getClientSecret();
        this.scope = Arrays.asList(oauthClient.getScope().split(","));
        this.resourceIds = oauthClient.getResourceIds();
        this.authorizedGrantTypes = oauthClient.getAuthorizedGrantTypes();
        this.accessTokenValidity = oauthClient.getAccessTokenValidity();
        this.refreshTokenValidity = oauthClient.getRefreshTokenValidity();
        this.rvmOwnerNumber = oauthClient.getRvmOwnerNumber();
        this.version = oauthClient.getVersion();
        this.type = oauthClient.getType();
        if (!CollectionUtils.isEmpty(oauthClient.getRvmMachines())) {
            this.rvmMachines = oauthClient.getRvmMachines().stream().map(RvmMachineDto::from).collect(Collectors.toList());
        }
    }

    public static OAuthClientDto from(OAuthClient oauthClient) {
        return oauthClient == null ? null : new OAuthClientDto(oauthClient);
    }

    public OAuthClient toEntity(MongoTemplate mongoTemplate) {
        OAuthClient oauthClient = this.id != null ? mongoTemplate.findById(this.id, OAuthClient.class) : new OAuthClient();
        oauthClient = oauthClient != null ? oauthClient : new OAuthClient();

        oauthClient.setClientId(this.clientId);
        if (!StringUtils.isEmpty(this.clientSecret)) {
            oauthClient.setClientSecret(this.clientSecret);
        }
        oauthClient.setScope(this.scope == null ? null : String.join(",", this.scope));
        oauthClient.setResourceIds(this.resourceIds);
        oauthClient.setAuthorizedGrantTypes(this.authorizedGrantTypes);
        oauthClient.setAccessTokenValidity(this.accessTokenValidity);
        oauthClient.setRefreshTokenValidity(this.refreshTokenValidity);

        oauthClient.setRvmOwnerNumber(this.rvmOwnerNumber);
        oauthClient.setVersion(this.version);
        oauthClient.setType(this.type);
        oauthClient.setRvmMachines(this.rvmMachines != null ? this.rvmMachines.stream()
                .map(rvmMachineDto -> rvmMachineDto.toEntity(mongoTemplate)).collect(Collectors.toList()) : null);

        return oauthClient;
    }

}
