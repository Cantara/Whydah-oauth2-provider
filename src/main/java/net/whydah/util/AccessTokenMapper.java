package net.whydah.util;

import io.jsonwebtoken.Claims;
import net.whydah.commands.config.ConstantValue;
import net.whydah.service.oauth2proxyserver.RSAKeyFactory;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import net.whydah.sso.user.types.UserToken;
import org.slf4j.Logger;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by baardl on 15.08.17.
 * Modified by huy on 12.06.20
 * Upgrade to support OpenID Connect
 */
public class AccessTokenMapper {
    private static final Logger log = getLogger(AccessTokenMapper.class);

    //Within the OpenID Connect specification, the standard scopes are defined as openid, profile, email, address, and phone
    //If you use any scope beyond those, you’re beyond the OIDC specification and back into general OAuth and this is where it gets complicated.
    private static final String SCOPE_OPENID = "openid";
    private static final String SCOPE_PROFILE = "profile"; //a lack of support for a profile url (for example: https://fb.com/me) in UserToken, but can possibly define it as a usertoken's role
    private static final String SCOPE_EMAIL = "email";
    private static final String SCOPE_ADDRESS = "address"; //we lack this field in UserToken
    private static final String SCOPE_PHONE = "phone";
    
    
   
    
    public static String buildToken(UserToken userToken, String clientId, String applicationId, String applicationName, String applicationUrl, List<String> userAuthorizedScope) throws Exception {
        String accessToken = null;
        if (userToken != null) {
			int expireSec = (int) (Long.valueOf(userToken.getLifespan()) / 1000);
			expireSec = expireSec - 10; // subtract processing time for OAuth2 flow

			JsonObjectBuilder tokenBuilder = Json.createObjectBuilder()
					.add("access_token", buildAccessToken(userToken, clientId, applicationId, applicationName, applicationUrl, userAuthorizedScope)) //this client will use this to access other servers' resources
					.add("token_type", "bearer")
					.add("expires_in", expireSec)
					.add("refresh_token", ClientIDUtil.encrypt(userToken.getUserTokenId() + ":" + String.join(" ", userAuthorizedScope)));

			if (userAuthorizedScope.contains(SCOPE_OPENID)) {
				//OpenID Connect requires "id_token"
				tokenBuilder = tokenBuilder.add("id_token", buildClientToken(userToken, clientId, applicationId, applicationName, applicationUrl, userAuthorizedScope)); //attach granted scopes to JWT
            } else {
            	 //back to general OAuth
                 tokenBuilder = buildUserInfoJson(tokenBuilder, userToken, applicationId, userAuthorizedScope);
            }
            accessToken = tokenBuilder.build().toString();
        }
        log.info("token built: {}", accessToken);
        return accessToken;
    }

	public static JsonObjectBuilder buildUserInfoJson(JsonObjectBuilder tokenBuilder, UserToken userToken, String applicationId, List<String> userAuthorizedScope) {
		if (userAuthorizedScope.contains(SCOPE_EMAIL)) {
		     tokenBuilder = tokenBuilder.add(SCOPE_EMAIL, userToken.getEmail());
		 }
		 if (userAuthorizedScope.contains(SCOPE_PHONE)) {
		     tokenBuilder = tokenBuilder.add(SCOPE_PHONE, userToken.getCellPhone());
		 }
		 tokenBuilder = buildRoles(userToken.getRoleList(), applicationId, userAuthorizedScope, tokenBuilder);
		return tokenBuilder;
	}
    
	private static String buildClientToken(UserToken userToken, String clientId, String applicationId, String applicationName, String applicationUrl, List<String> userAuthorizedScope) throws Exception {
		Map<String, Object> claims = new HashMap<String, Object>();
		claims.put(Claims.ID, UUID.randomUUID().toString());
		claims.put(Claims.SUBJECT, userToken.getUserName());
		claims.put(Claims.AUDIENCE, applicationUrl!=null? applicationUrl:applicationName);
		claims.put(Claims.ISSUER, ConstantValue.MYURI);
		claims.put("first_name", userToken.getFirstName());
		claims.put("last_name", userToken.getLastName());
		
		if(userAuthorizedScope.contains(SCOPE_EMAIL)) {
			claims.put(SCOPE_EMAIL, userToken.getEmail());
		}
		if(userAuthorizedScope.contains(SCOPE_PHONE)) {
			claims.put(SCOPE_PHONE, userToken.getCellPhone());
		}
		//for profile, address and other custom scopes we can look into this
		for (UserApplicationRoleEntry role : userToken.getRoleList()) {
			if (role.getApplicationId().equals(applicationId)) {
				if (userAuthorizedScope.contains(role.getRoleName())) {
					claims.put(role.getRoleName(), role.getRoleValue());
				}
			}
		} 	
		return JwtUtils.generateJwtToken(claims, new Date(System.currentTimeMillis() + Long.valueOf(userToken.getLifespan())), RSAKeyFactory.getKey().getPrivate());
	}

	public static String buildAccessToken(UserToken usertoken, String clientId, String appId, String applicationName, String applicationUrl, List<String> userAuthorizedScope) throws Exception {
    	Map<String, Object> claims = new HashMap<String, Object>();
    	claims.put(Claims.ID, UUID.randomUUID().toString());
    	claims.put(Claims.SUBJECT, usertoken.getUserName());  //a locally unique identity in the context of the issuer. The processing of this claim is generally application specific
		claims.put(Claims.AUDIENCE, applicationUrl!=null? applicationUrl:applicationName);
		claims.put(Claims.ISSUER, ConstantValue.MYURI);
		//useful info for back-end services
		claims.put("app_id", appId); //used by other back-end services
		claims.put("usertoken_id", usertoken.getUserTokenId()); //used by other back-end services
		claims.put("scope", String.join(" ", userAuthorizedScope));  //used for /userinfo endpoint, re-populating user info with this granted scope list	
    	return JwtUtils.generateJwtToken(claims,  new Date(System.currentTimeMillis() + Long.valueOf(usertoken.getLifespan())), RSAKeyFactory.getKey().getPrivate());
    }

    protected static JsonObjectBuilder buildRoles(List<UserApplicationRoleEntry> roleList, String applicationId, List<String> userAuthorizedScope, JsonObjectBuilder tokenBuilder) {
        for (UserApplicationRoleEntry role : roleList) {
            if (role.getApplicationId().equals(applicationId)) {
                if (userAuthorizedScope.contains(role.getRoleName())) {
                    tokenBuilder.add(role.getRoleName(), role.getRoleValue());
                }
            }
        }
        return tokenBuilder;
    }
}
