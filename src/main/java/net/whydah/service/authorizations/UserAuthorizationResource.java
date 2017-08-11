package net.whydah.service.authorizations;

import net.whydah.util.CookieManager;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;

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
    //import org.glassfish.jersey.server.mvc.Viewable;

    /**
     * https://<host>/<context_root>/user?client_id=testclient&scopes=scopes with space delimiter
     * eg: http://localhost:8086/Whydah-OAuth2Service/user?client_id=testclient&scopes=email%20nick
     * @param clientId
     * @param scope
     * @param request
     * @return
     */
    @GET
    public Viewable getHello(@QueryParam("client_id") String clientId, @QueryParam("scope") String scope,
                             @QueryParam("code") String code, @Context HttpServletRequest request) {
        String userTokenIdFromCookie = CookieManager.getUserTokenIdFromCookie(request);
        if (userTokenIdFromCookie == null) {
            userTokenIdFromCookie = "";
        }
        final Map<String,String> user = new HashMap<>();
        user.put("name", userTokenIdFromCookie);
        user.put("id","1224");
        final Map<String, Object> map = new HashMap<>();
        map.put("user", user);
        map.put("client_id", clientId);
        if (code == null) {
            code = "";
        }
        map.put("code", code);

        List<String> scopes = new ArrayList<>();
        if (scope != null) {
            String[] scopeArr = scope.split(" ");
            scopes = Arrays.asList(scopeArr);
        }

        map.put("scopeList", scopes);
        map.put("scopes", scope);

        Viewable userA =  new Viewable("/UserAuthorization.ftl", map);
        return userA;
    }
}
