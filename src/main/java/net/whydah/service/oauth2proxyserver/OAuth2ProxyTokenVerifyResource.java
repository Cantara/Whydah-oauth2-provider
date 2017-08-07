package net.whydah.service.oauth2proxyserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.whydah.util.Configuration;
import net.whydah.commands.config.ConstantValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

@Path(OAuth2ProxyTokenVerifyResource.OAUTH2TOKENVERIFY_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class OAuth2ProxyTokenVerifyResource {
    public static final String OAUTH2TOKENVERIFY_PATH = "/verify";
    private static final ObjectMapper mapper = new ObjectMapper();


    private static final Logger log = LoggerFactory.getLogger(OAuth2ProxyServerResource.class);


    @GET
    public Response getOAuth2ProxyServerController(@Context HttpHeaders headers) throws MalformedURLException {

        if (headers != null) {
            log.debug(getClass().getName() + ": headers=" + headers);
            if (!headers.getHeaderString("Authorization").equalsIgnoreCase("Bearer " + ConstantValue.ATOKEN)) {
                log.error("Illegal OAUTH token provided");
                return Response.status(Response.Status.FORBIDDEN).build();
            }
        }
        String jsonResult = "";
        Map systemEventsJson = Configuration.getMap("oauth.dummy.verifiedtoken");
        try {
            jsonResult = mapper.writeValueAsString(systemEventsJson);
        } catch (IOException e) {
            log.warn("Could not convert to Json {}", jsonResult);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        log.trace("getOAuth2ProxyServerController - returning:{}", jsonResult);
        return Response.status(Response.Status.OK).entity(jsonResult).build();
    }
}
