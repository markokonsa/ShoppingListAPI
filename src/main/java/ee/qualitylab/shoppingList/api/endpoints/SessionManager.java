package ee.qualitylab.shoppingList.api.endpoints;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.NotFoundException;

import java.util.Calendar;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import ee.qualitylab.shoppingList.api.entities.Session;
import ee.qualitylab.shoppingList.api.entities.User;
import ee.qualitylab.shoppingList.api.services.OfyService;

@Api(
        name = "session",
        resource = "session",
        namespace = @ApiNamespace(
                ownerDomain = "entities.api.shoppingList.qualitylab.ee",
                ownerName = "entities.api.shoppingList.qualitylab.ee",
                packagePath = ""
        )
)
public class SessionManager {

    @ApiMethod(
            name = "updateUserSession",
            path = "update",
            httpMethod = ApiMethod.HttpMethod.POST)
    public Session updateUserSession(User user, HttpServletRequest request) {
        OfyService.begin();
        Session session = getSessionForUser(user);
        session.setSessionKey(request.getSession().getId());
        session.setSessionEnd(getDatePlusOneDay());
        OfyService.ofy().save().entity(session).now();
        session = getSessionForUser(user);
        if (getSessionForUser(user).getSessionEnd().before(new Date())) {
            session.setSessionEnd(getDatePlusOneDay());
            session.setSessionKey(request.getSession().getId());
            OfyService.ofy().save().entity(session).now();
            return getSessionForUser(user);
        } else {
            return session;
        }
    }
    @ApiMethod(
            name = "addNewSession",
            path = "add",
            httpMethod = ApiMethod.HttpMethod.POST)
    public Session addNewSessionToUser(User user, HttpServletRequest request) throws NotFoundException {
        OfyService.begin();
        if (user != null) {
            Session session = new Session();
            session.setUserId(user.getId());
            session.setSessionEnd(getDatePlusOneDay());
            session.setSessionKey(request.getSession().getId());
            OfyService.ofy().save().entity(session).now();
            assert session.getId() != null;
            return session;
        }else {
            return null;
        }
    }

    @ApiMethod(
            name = "getSession",
            path = "get",
            httpMethod = ApiMethod.HttpMethod.POST)
    public Session getSessionForUser(User user) {
        OfyService.begin();
        return OfyService.ofy().load().type(Session.class).filter("userId", user.getId()).first().now();
    }

    @ApiMethod(
            name = "getUserFromSession",
            path = "user",
            httpMethod = ApiMethod.HttpMethod.POST)
     public User getUserFromSession(HttpServletRequest request) throws NotFoundException {
         String sessionKey = getSessionKeyFromRequest(request);
       Session session = OfyService.ofy().load().type(Session.class).filter("sessionKey",sessionKey).first().now();
         if (session == null){
             throw new NotFoundException("Cannot find session with id: "+sessionKey);
         }
         User user = OfyService.ofy().load().type(User.class).id(session.getUserId()).now();
         if (user == null){
             throw new NotFoundException("Cannot find user with id: "+session.getUserId());
         }
         return user;
     }


    private String getSessionKeyFromRequest(HttpServletRequest request) throws NotFoundException {
        String sessionKey = request.getHeader("X-API-KEY");
        if (sessionKey == null) {
            throw new NotFoundException("Cannot find header X-API-KEY from header");
        }else {
            return sessionKey;
        }
    }

    private Date getDatePlusOneDay() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DATE, 1);
        return cal.getTime();
    }

}
