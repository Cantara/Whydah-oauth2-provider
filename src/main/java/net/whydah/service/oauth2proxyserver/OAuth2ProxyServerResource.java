package net.whydah.service.oauth2proxyserver;

import net.whydah.commands.config.ConstantValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.MalformedURLException;

@Path(OAuth2ProxyServerResource.OAUTH2TOKENSERVER_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class OAuth2ProxyServerResource {
    public static final String OAUTH2TOKENSERVER_PATH = "/token";


    private static final Logger log = LoggerFactory.getLogger(OAuth2ProxyServerResource.class);


    @GET
    public Response getOauth2ProxyServerController(@QueryParam("grant_type") String grant_type, @QueryParam("client_id") String client_id, @QueryParam("client_secret") String client_secret) throws MalformedURLException {
        log.trace("getOAuth2ProxyServerController - /token got grant_type: {}",grant_type);
        log.trace("getOAuth2ProxyServerController - /token got client_id: {}",client_id);
        log.trace("getOAuth2ProxyServerController - /token got client_secret: {}",client_secret);

        String accessToken = "{ \"access_token\":\"dummy\" }";

        return Response.status(Response.Status.OK).entity(accessToken).build();
    }

    @POST
    public Response oauth2ProxyServerController(@Context UriInfo uriInfo) throws MalformedURLException {


        String grant_type = uriInfo.getQueryParameters().getFirst("grant_type");
        log.trace("oauth2ProxyServerController - /token got grant_type: {}",grant_type);

        // Application authentication
        if ("client_credentials".equalsIgnoreCase(grant_type)){
            String client_id = uriInfo.getQueryParameters().getFirst("client_id");
            String client_secret = uriInfo.getQueryParameters().getFirst("client_secret");
            log.trace("oauth2ProxyServerController - /token got client_id: {}",client_id);
            log.trace("oauth2ProxyServerController - /token got client_secret: {}",client_secret);
            // stubbed accesstoken
            String accessToken = "{ \"access_token\":\"" + ConstantValue.ATOKEN + "\" }";
            return Response.status(Response.Status.OK).entity(accessToken).build();
        }

        // User token request
        if ("authorization_code".equalsIgnoreCase(grant_type)){
            String code = uriInfo.getQueryParameters().getFirst("code");
            String redirect_uri = uriInfo.getQueryParameters().getFirst("redirect_uri");
            String client_id = uriInfo.getQueryParameters().getFirst("client_id");
            String client_secret = uriInfo.getQueryParameters().getFirst("client_secret");
            log.trace("oauth2ProxyServerController - /token got code: {}",code);
            log.trace("oauth2ProxyServerController - /token got redirect_uri: {}",redirect_uri);
            log.trace("oauth2ProxyServerController - /token got client_id: {}",client_id);
            log.trace("oauth2ProxyServerController - /token got client_secret: {}",client_secret);

            String accessToken = "{\"access_token\":\"ACCESS_TOKEN\",\"token_type\":\"bearer\",\"expires_in\":2592000,\"refresh_token\":\"REFRESH_TOKEN\",\"scope\":\"read\",\"uid\":22022,\"info\":{\"name\":\"Totto\",\"email\":\"totto@totto.org\"}}";
            return Response.status(Response.Status.OK).entity(accessToken).build();
        }


        return Response.status(Response.Status.FORBIDDEN).build();
    }

}

