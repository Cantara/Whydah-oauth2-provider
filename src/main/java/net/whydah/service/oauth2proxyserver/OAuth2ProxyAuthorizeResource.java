package net.whydah.service.oauth2proxyserver;

import net.whydah.commands.config.ConstantValue;
import net.whydah.service.authorizations.UserAuthorization;
import net.whydah.service.authorizations.UserAuthorizationResource;
import net.whydah.service.authorizations.UserAuthorizationService;
import net.whydah.service.clients.Client;
import net.whydah.service.clients.ClientService;
import net.whydah.sso.user.types.UserToken;
import net.whydah.util.CookieManager;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;

import static net.whydah.service.authorizations.UserAuthorizationService.DEVELOPMENT_USER_TOKEN_ID;
import static org.slf4j.LoggerFactory.getLogger;


@Path(OAuth2ProxyAuthorizeResource.OAUTH2AUTHORIZE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class OAuth2ProxyAuthorizeResource {
    public static final String OAUTH2AUTHORIZE_PATH = "/authorize";


    private static final Logger log = getLogger(OAuth2ProxyAuthorizeResource.class);
    private static final Logger auditLog = getLogger("auditLog");

    private final TokenService tokenService;
    private final UserAuthorizationService authorizationService;
    private final ClientService clientService;


    @Autowired
    public OAuth2ProxyAuthorizeResource(TokenService tokenService, UserAuthorizationService authorizationService, ClientService clientService) {
        this.tokenService = tokenService;
        this.authorizationService = authorizationService;
        this.clientService = clientService;
    }


    /**
     * Ask the end user to authorize the client to access information described in scope.
     * Implementation of https://tools.ietf.org/html/rfc6749#section-4.1.1
     * @param response_type "code" or "token" REQUIRED
     * @param scope OPTIONAl
     * @param client_id REQUIRED
     * @param redirect_uri OPTIONAL
     * @param state value used by the client to maintain state between the request and callback. OPTIONAL
     * @return HTTP 302 https://client.example.com/cb?code=SplxlOBeZQQYbYS6WxSbIA&state=xyz
     * @throws MalformedURLException
     */
    @GET
    public Response getOauth2ProxyServerController(@QueryParam("response_type") String response_type,
                                                   @QueryParam("scope") String scope,
                                                   @QueryParam("client_id") String client_id,
                                                   @QueryParam("redirect_uri") String redirect_uri,
                                                   @QueryParam("state") String state, 
                                                   @Context HttpServletRequest request) throws MalformedURLException {
        log.trace("OAuth2ProxyAuthorizeResource - /authorize got response_type: {}" +
                "\n\tscope: {} \n\tclient_id: {} \n\tredirect_uri: {} \n\tstate: {}", response_type, scope, client_id, redirect_uri, state);

        Client client = clientService.getClient(client_id);
        
        String subPath = "?scope=" + encode(scope) + "&" + "response_type=" + response_type + "&" +"client_id="+ client_id + "&client_name=" + client.getApplicationName()  + "&" + "redirect_uri=" +redirect_uri + "&" + "state=" + state; 
        String userTokenIdFromCookie = CookieManager.getUserTokenIdFromCookie(request);
        if(userTokenIdFromCookie!=null) {
        	  String url = "." +UserAuthorizationResource.USER_PATH + subPath;
            URI userAuthorization = URI.create(url);
            return Response.seeOther(userAuthorization).build();
        } else {
        	String url = ConstantValue.MYURI + "/" + OAUTH2AUTHORIZE_PATH + subPath;
        	URI login_redirect = URI.create(ConstantValue.SSO_URI + "/login?redirectURI=" + url);
        	return Response.seeOther(login_redirect).build();
        }
 


    }

    @POST
    @Path("/acceptance")
    @Consumes("application/x-www-form-urlencoded")
    public Response userAcceptance(@FormParam("state") String state, MultivaluedMap<String, String> formParams,@Context HttpServletRequest request) {
        log.trace("Acceptance sent. Values {}", formParams);

        String code = tokenService.buildCode();
        String client_id = formParams.getFirst("client_id");
        
        String accepted = formParams.getFirst("accepted");
        if ("yes".equals(accepted.trim())) {
            auditLog.info("User accepted authorization. Code {}, FormParams {}", code, formParams);
            List<String> scopes = findAcceptedScopes(formParams);
            String userTokenId = formParams.getFirst("usertoken_id");
            String whydahUserId = null; //Ignoring userId for now findWhydahUserId(formParams, request);
            if (userTokenId != null) {
                UserAuthorization userAuthorization = new UserAuthorization(code, scopes, whydahUserId, userTokenId);
                userAuthorization.setClientId(client_id);
                authorizationService.addAuthorization(userAuthorization);
            }
        }

        //TODO add UserAuthorization with code and user info.
        String redirect_url = formParams.getFirst("redirect_uri");
        log.info("Resolving redirect_uri from POST form, found:", redirect_url);
        if (redirect_url == null || redirect_url.isEmpty()) {
            
            Client client = clientService.getClient(client_id);
            if (client != null) {
                redirect_url = client.getRedirectUrl(); //clientService."http://localhost:8888/oauth/generic/callback";
                log.info("Resolving redirect_uri from clientService.getClient.getRedirectUrl(), found:", redirect_url);
            }
        }
        URI userAgent_goto = URI.create(redirect_url + "?code=" + code +"&state=" + state);
        return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
    }

    protected String findWhydahUserId(MultivaluedMap<String, String> formParams, HttpServletRequest request) {
        String userTokenId = formParams.getFirst("usertoken_id");
        String userTokenIdFromCookie = CookieManager.getUserTokenIdFromCookie(request);
        //Validate that usertoken has stayed the same. Ie user has not loged into another account.
        if (userTokenIdFromCookie == null) {
            userTokenIdFromCookie = DEVELOPMENT_USER_TOKEN_ID; //FIXME temporary
        }
        String whydahUserId = null;
        if (userTokenId != null && userTokenId.equals(userTokenIdFromCookie)) {
            UserToken userToken = authorizationService.findUserTokenFromUserTokenId(userTokenId);
            if (userToken != null) {
                whydahUserId = userToken.getUid();
            }
        }
        return whydahUserId;
    }

    protected List<String> findAcceptedScopes(MultivaluedMap<String, String> formParams) {
        String scope = formParams.getFirst("scope");
        return authorizationService.buildScopes(scope);
    }

    protected String encode(String value) {
        try {
            if (value != null) {
                return URLEncoder.encode(value, "UTF-8");
            } else {
                return "";
            }
        } catch (UnsupportedEncodingException e) {
            log.warn("Encoding exception should not happen. Value {}, Reason {}", value, e.getMessage());
        }
        return value;
    }


}

