package ee.qualitylab.shoppingList.api.services;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;

import ee.qualitylab.shoppingList.api.entities.Family;
import ee.qualitylab.shoppingList.api.entities.Session;
import ee.qualitylab.shoppingList.api.entities.ShoppingList;
import ee.qualitylab.shoppingList.api.entities.ShoppingListItem;
import ee.qualitylab.shoppingList.api.entities.User;
import ee.qualitylab.shoppingList.api.entities.UserInFamily;

/**
 * Created by Marko on 24.10.2015.
 */
public class OfyService {
    static {
        ObjectifyService.factory().register(Family.class);
        ObjectifyService.factory().register(ShoppingList.class);
        ObjectifyService.factory().register(ShoppingListItem.class);
        ObjectifyService.factory().register(User.class);
        ObjectifyService.factory().register(Session.class);
        ObjectifyService.factory().register(UserInFamily.class);
    }

    public static Objectify ofy() {
        return ObjectifyService.ofy();
    }

    public static void begin() {
        ObjectifyService.begin();
    }
}
