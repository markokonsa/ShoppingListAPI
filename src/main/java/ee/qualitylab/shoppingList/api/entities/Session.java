package ee.qualitylab.shoppingList.api.entities;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.Date;

/**
 * Created by Marko on 24.10.2015.
 */
@Entity
@Index
public class Session {

    @Id
    Long id;
    Long userId;
    String sessionKey;
    Date sessionEnd;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public Date getSessionEnd() {
        return sessionEnd;
    }

    public void setSessionEnd(Date sessionEnd) {
        this.sessionEnd = sessionEnd;
    }
}
