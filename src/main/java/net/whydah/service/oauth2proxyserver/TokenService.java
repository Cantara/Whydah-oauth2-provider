package net.whydah.service.oauth2proxyserver;

import net.whydah.service.authorizations.UserAuthorization;
import net.whydah.service.authorizations.UserAuthorizationService;
import net.whydah.sso.user.types.UserToken;
import net.whydah.util.AccessTokenMapper;
import net.whydah.util.ClientIDUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by baardl on 09.08.17.
 */
@Service
public class TokenService {
	private static final Logger log = getLogger(TokenService.class);

	private final UserAuthorizationService authorizationService;

	@Autowired
	public TokenService(UserAuthorizationService authorizationService) {
		this.authorizationService = authorizationService;
	}


	public String buildAccessToken(String client_id, String client_secret, String theUsersAuthorizationCode) {
		log.info("buildAccessToken called");
		log.trace("oauth2ProxyServerController - /token got code: {}",theUsersAuthorizationCode);
		log.trace("oauth2ProxyServerController - /token got client_id: {}",client_id);
		log.trace("oauth2ProxyServerController - /token got client_secret: {}",client_secret);

		UserAuthorization userAuthorization = authorizationService.getAuthorization(theUsersAuthorizationCode);
		String accessToken = null;
		if (userAuthorization == null) {
			log.trace("User has not accepted with this code. UserAuthorization were not found in repository for code {}", theUsersAuthorizationCode);
			// accessToken = "{\"access_token\":\"ACCESS_TOKEN\",\"token_type\":\"bearer\",\"expires_in\":2592000,\"refresh_token\":\"REFRESH_TOKEN\",\"scope\":\"read\",\"uid\":22022,\"info\":{\"name\":\"Totto\",\"email\":\"totto@totto.org\"}}";
		} else {

			String userTokenId = userAuthorization.getUserTokenId();
			UserToken userToken = authorizationService.findUserTokenFromUserTokenId(userTokenId);
			log.trace("Found userToken {}", userToken);
			String applicationId = findApplicationId(userAuthorization.getClientId());
			List<String> userAuthorizedScopes = userAuthorization.getScopes();
			accessToken = AccessTokenMapper.buildToken(userToken, client_id, applicationId, userAuthorizedScopes);
		}
		return accessToken;
	}

	private String findApplicationId(String clientId) {

		return ClientIDUtil.getApplicationId(clientId);
	}


	public String buildCode() {
		return UUID.randomUUID().toString();
	}


	public String refreshAccessToken(String client_id, String client_secret, String refresh_token) {
		log.info("refreshAccessToken called");
		log.trace("oauth2ProxyServerController - /token got refresh_token: {}", refresh_token);
		log.trace("oauth2ProxyServerController - /token got client_id: {}",client_id);
		log.trace("oauth2ProxyServerController - /token got client_secret: {}",client_secret);
		
		String[] parts = ClientIDUtil.decrypt(refresh_token).split(":", 2);
		String old_usertoken_id = parts[0];
		String scopeList = parts[1];
		
		UserToken userToken = authorizationService.findUserTokenFromUserTokenId(old_usertoken_id);
		String applicationId = findApplicationId(client_id);
		
		return  AccessTokenMapper.buildToken(userToken, client_id, applicationId, authorizationService.buildScopes(scopeList));
	}
}
