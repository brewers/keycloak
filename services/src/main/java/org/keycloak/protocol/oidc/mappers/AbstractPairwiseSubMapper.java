package org.keycloak.protocol.oidc.mappers;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.oidc.utils.PairwiseSubMapperUtils;
import org.keycloak.protocol.oidc.utils.PairwiseSubMapperValidator;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

import java.util.LinkedList;
import java.util.List;

/**
 * Set the 'sub' claim to pairwise .
 *
 * @author <a href="mailto:martin.hardselius@gmail.com">Martin Hardselius</a>
 */
public abstract class AbstractPairwiseSubMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {
    public static final String PROVIDER_ID_SUFFIX = "-pairwise-sub-mapper";

    public static String getId(String prefix) {
        return prefix + PROVIDER_ID_SUFFIX;
    }

    public abstract String getIdPrefix();

    /**
     * Generates a pairwise subject identifier.
     *
     * @param mappingModel
     * @param sectorIdentifier client sector identifier
     * @param localSub         local subject identifier (user id)
     * @return A pairwise subject identifier
     */
    public abstract String generateSub(ProtocolMapperModel mappingModel, String sectorIdentifier, String localSub);

    /**
     * Override to add additional provider configuration properties. By default, a pairwise sub mapper will only contain configuration for a sector identifier URI.
     *
     * @return A list of provider configuration properties.
     */
    public List<ProviderConfigProperty> getAdditionalConfigProperties() {
        return new LinkedList<>();
    }

    /**
     * Override to add additional configuration validation. Called when instance of mapperModel is created/updated for this protocolMapper through admin endpoint.
     *
     * @param session
     * @param realm
     * @param mapperContainer client or clientTemplate
     * @param mapperModel
     * @throws ProtocolMapperConfigException if configuration provided in mapperModel is not valid
     */
    public void validateAdditionalConfig(KeycloakSession session, RealmModel realm, ProtocolMapperContainerModel mapperContainer, ProtocolMapperModel mapperModel) throws ProtocolMapperConfigException {
    }

    @Override
    public final String getDisplayCategory() {
        return AbstractOIDCProtocolMapper.TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public final IDToken transformIDToken(IDToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionModel clientSession) {
        setSubject(token, generateSub(mappingModel, getSectorIdentifier(clientSession.getClient(), mappingModel), userSession.getUser().getId()));
        return token;
    }

    @Override
    public final AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionModel clientSession) {
        setSubject(token, generateSub(mappingModel, getSectorIdentifier(clientSession.getClient(), mappingModel), userSession.getUser().getId()));
        return token;
    }

    @Override
    public final AccessToken transformUserInfoToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionModel clientSession) {
        setSubject(token, generateSub(mappingModel, getSectorIdentifier(clientSession.getClient(), mappingModel), userSession.getUser().getId()));
        return token;
    }

    private void setSubject(IDToken token, String pairwiseSub) {
        token.getOtherClaims().put("sub", pairwiseSub);
    }

    @Override
    public final List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> configProperties = new LinkedList<>();
        configProperties.add(PairwiseSubMapperHelper.createSectorIdentifierConfig());
        configProperties.addAll(getAdditionalConfigProperties());
        return configProperties;
    }

    private String getSectorIdentifier(ClientModel client, ProtocolMapperModel mappingModel) {
        String sectorIdentifierUri = PairwiseSubMapperHelper.getSectorIdentifierUri(mappingModel);
        if (sectorIdentifierUri != null && !sectorIdentifierUri.isEmpty()) {
            return PairwiseSubMapperUtils.resolveValidSectorIdentifier(sectorIdentifierUri);
        }
        return PairwiseSubMapperUtils.resolveValidSectorIdentifier(client.getRootUrl(), client.getRedirectUris());
    }

    @Override
    public final void validateConfig(KeycloakSession session, RealmModel realm, ProtocolMapperContainerModel mapperContainer, ProtocolMapperModel mapperModel) throws ProtocolMapperConfigException {
        ClientModel client = null;
        if (mapperContainer instanceof ClientModel) {
            client = (ClientModel) mapperContainer;
            PairwiseSubMapperValidator.validate(session, client, mapperModel);
        }
        validateAdditionalConfig(session, realm, mapperContainer, mapperModel);
    }

    @Override
    public final String getId() {
        return getIdPrefix() + PROVIDER_ID_SUFFIX;
    }
}


