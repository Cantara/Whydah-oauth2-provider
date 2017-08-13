package net.whydah.service.authorizations;

import net.whydah.service.oauth2proxyserver.AuthorizationService;
import net.whydah.sso.user.types.UserToken;
import net.whydah.util.CookieManager;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import java.util.*;

import static net.whydah.service.authorizations.UserAuthorizationResource.USER_PATH;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by baardl on 10.08.17.
 */
@Path(USER_PATH)
public class UserAuthorizationResource {
    private static final Logger log = getLogger(UserAuthorizationResource.class);
    public static final String USER_PATH = "/user";

    private final AuthorizationService authorizationService;

    @Autowired
    public UserAuthorizationResource(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    /**
     * https://<host>/<context_root>/user?client_id=testclient&scopes=scopes with space delimiter
     * eg: http://localhost:8086/Whydah-OAuth2Service/user?client_id=testclient&scopes=email%20nick
     * @param clientId
     * @param scope
     * @param request
     * @return
     */
    @GET
    public Viewable authorizationGui(@QueryParam("client_id") String clientId, @QueryParam("scope") String scope,
                             @QueryParam("code") String code,
                             @QueryParam("state") String state,
                             @QueryParam("redirect_url") String redirect_url,@Context HttpServletRequest request) {
        final Map<String,String> user = new HashMap<>();
        String userTokenIdFromCookie = CookieManager.getUserTokenIdFromCookie(request);
        String name = "Annonymous";
        user.put("id","-should-not-use-");
        if (userTokenIdFromCookie == null) {
            //FIXME remove stub data
            log.warn("Using stub'ed data for accessing usertokenid");
            userTokenIdFromCookie = "4efd7770-9b03-48c8-8992-5e9a5d06e45e";
            UserToken userToken = authorizationService.findUserToken(userTokenIdFromCookie);
            if (userToken != null) {
                name = userToken.getFirstName() + " " + userToken.getLastName();
            }
        }

        user.put("name", name);

        Map<String, Object> model = new HashMap<>();
        model.put("user", user);
        model = addParameter("client_id", clientId, model);
        model = addParameter("scope", scope, model);
        model = addParameter("code", code, model);
        model = addParameter("state", state, model);
        model = addParameter("redirect_url", redirect_url, model);

        List<String> scopes = new ArrayList<>();
        if (scope != null) {
            String[] scopeArr = scope.split(" ");
            scopes = Arrays.asList(scopeArr);
        }

        model.put("scopeList", scopes);

        Viewable userAuthorizationGui =  new Viewable("/UserAuthorization.ftl", model);
        return userAuthorizationGui;
    }



    protected Map<String, Object> addParameter(String key, String value, Map<String, Object> map) {
        if (key != null && map != null) {
          if (value == null) {
              map.put(key, "");
          } else {
              map.put(key, value);
          }
        }
        return map;

    }
}
