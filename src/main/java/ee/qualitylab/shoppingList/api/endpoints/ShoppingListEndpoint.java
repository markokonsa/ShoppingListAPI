package ee.qualitylab.shoppingList.api.endpoints;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.response.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import ee.qualitylab.shoppingList.api.entities.Family;
import ee.qualitylab.shoppingList.api.entities.Session;
import ee.qualitylab.shoppingList.api.entities.ShoppingList;
import ee.qualitylab.shoppingList.api.entities.ShoppingListItem;
import ee.qualitylab.shoppingList.api.entities.User;
import ee.qualitylab.shoppingList.api.entities.UserInFamily;
import ee.qualitylab.shoppingList.api.services.OfyService;
import ee.qualitylab.shoppingList.api.services.ValidationService;
import ee.qualitylab.shoppingList.api.utilities.CustomResponse;

@Api(
        name = "shoppingList",
        resource = "shoppinglist",
        namespace = @ApiNamespace(
                ownerDomain = "entities.api.shoppingList.qualitylab.ee",
                ownerName = "entities.api.shoppingList.qualitylab.ee",
                packagePath = ""
        )
)
public class ShoppingListEndpoint {

    private static final Logger logger = Logger.getLogger(ShoppingListEndpoint.class.getName());

    /**
     * Inserts a new {@code User}.
     */
    @ApiMethod(
            name = "registerUser",
            path = "register",
            httpMethod = ApiMethod.HttpMethod.POST)
    public Session registerUser(User user, HttpServletRequest request) throws NotFoundException {
        ValidationService.validateRegistrationDetails(user);
        OfyService.begin();
        User alreadyUser = OfyService.ofy().load().type(User.class).filter("userName", user.getUserName()).first().now();
        if (alreadyUser != null) {
            throw new AssertionError("User with username: " + user.getUserName() + " already exists");
        }
        OfyService.ofy().save().entity(user).now();
        logger.info("Created user with id: " + user.getId());
        assert user.getId() != null;
        SessionManager sessionManager = new SessionManager();
        return sessionManager.addNewSessionToUser(user, request);
    }

    /**
     * Loging in with user.
     *
     * @return Session
     */
    @ApiMethod(
            name = "login",
            path = "login",
            httpMethod = ApiMethod.HttpMethod.POST)
    public Session login(@Named("username") String userName, @Named("password") String password, HttpServletRequest request) throws NotFoundException {
        OfyService.begin();
        User user = OfyService.ofy().load().type(User.class).filter("userName", userName).filter("password", password).first().now();
        if (user == null) {
            throw new NotFoundException("Invalid login credentials");
        }
        SessionManager sessionManager = new SessionManager();
        return sessionManager.updateUserSession(user, request);
    }

    /**
     * Add new family
     *
     * @return Family
     */
    @ApiMethod(
            name = "addFamily",
            path = "familiy/add",
            httpMethod = ApiMethod.HttpMethod.POST)
    public Family addFamilty(@Named("familyName") String familyName, HttpServletRequest request) throws NotFoundException {
        OfyService.begin();
        Family family = new Family();
        SessionManager sessionManager = new SessionManager();
        family.setAdminId(sessionManager.getUserFromSession(request).getId());
        family.setFamilyName(familyName);
        OfyService.ofy().save().entity(family).now();
        assert family.getId() != null;
        UserInFamily userInFamily = new UserInFamily();
        userInFamily.setFamilyId(family.getId());
        userInFamily.setUserId(family.getAdminId());
        OfyService.ofy().save().entity(userInFamily).now();
        assert userInFamily.getId() != null;
        return family;
    }

    /**
     * Add new family member
     *
     * @return Family
     */
    @ApiMethod(
            name = "addFamilyMember",
            path = "familiy/{id}/add",
            httpMethod = ApiMethod.HttpMethod.POST)
    public Family addFamilyMember(@Named("id") Long id, @Named("newUserId") Long userId, HttpServletRequest request) throws NotFoundException {
        OfyService.begin();
        Family family = OfyService.ofy().load().type(Family.class).id(id).now();
        assert family.getId() != null;
        SessionManager sessionManager = new SessionManager();
        User user = sessionManager.getUserFromSession(request);
        if (!user.getId().equals(family.getAdminId())) {
            throw new AssertionError("You are not an admin of this family");
        }
        UserInFamily uif = OfyService.ofy().load().type(UserInFamily.class).filter("userId", userId).filter("familyId", family.getId()).first().now();
        if (uif != null) {
            throw new AssertionError("User with ID: " + userId + " is already a member of this family");
        }
        UserInFamily userInFamily = new UserInFamily();
        userInFamily.setUserId(userId);
        userInFamily.setFamilyId(family.getId());
        OfyService.ofy().save().entity(userInFamily).now();
        return family;
    }

    @ApiMethod(
            name = "removeFamilyMember",
            path = "familiy/{id}/remove",
            httpMethod = ApiMethod.HttpMethod.POST)
    public Family removeFamilyMember(@Named("id") Long id, @Named("userId") Long userId, HttpServletRequest request) throws NotFoundException {
        OfyService.begin();
        Family family = OfyService.ofy().load().type(Family.class).id(id).now();
        assert family.getId() != null;
        SessionManager sessionManager = new SessionManager();
        User user = sessionManager.getUserFromSession(request);
        if (!user.getId().equals(family.getAdminId())) {
            throw new AssertionError("You are not an admin of this family");
        }
        UserInFamily uif = OfyService.ofy().load().type(UserInFamily.class).filter("userId", userId).filter("familyId", family.getId()).first().now();
        if (uif == null) {
            throw new AssertionError("User with ID: " + userId + " is not a member of this family");
        }
        OfyService.ofy().delete().entities(uif);
        return family;
    }

    @ApiMethod(
            name = "addShoppingList",
            path = "familiy/{id}/shoppinglist/add",
            httpMethod = ApiMethod.HttpMethod.POST)
    public ShoppingList addShopingList(@Named("id") Long id, @Named("named") String shoppingListName, HttpServletRequest request) throws NotFoundException {
        OfyService.begin();
        SessionManager sessionManager = new SessionManager();
        User user = sessionManager.getUserFromSession(request);
        UserInFamily userInFamily = OfyService.ofy().load().type(UserInFamily.class).filter("familyId", id).filter("userId", user.getId()).first().now();
        assert userInFamily.getId() != null;
        if (!user.getId().equals(userInFamily.getUserId())) {
            throw new AssertionError("You are not a member of this family");
        }
        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setFamilyId(id);
        shoppingList.setShoppingListName(shoppingListName);
        OfyService.ofy().save().entities(shoppingList).now();
        assert shoppingList.getId() != null;
        return shoppingList;
    }

    @ApiMethod(
            name = "removeShoppingList",
            path = "familiy/{id}/shoppinglist/remove/{listId}",
            httpMethod = ApiMethod.HttpMethod.POST)
    public CustomResponse removeShopingList(@Named("id") Long id, @Named("listId") Long listId, HttpServletRequest request) throws NotFoundException {
        OfyService.begin();
        SessionManager sessionManager = new SessionManager();
        User user = sessionManager.getUserFromSession(request);
        UserInFamily userInFamily = OfyService.ofy().load().type(UserInFamily.class).filter("familyId", id).filter("userId", user.getId()).first().now();
        assert userInFamily.getId() != null;
        if (!user.getId().equals(userInFamily.getUserId())) {
            throw new AssertionError("You are not a member of this family");
        }
        ShoppingList shoppingList = OfyService.ofy().load().type(ShoppingList.class).id(listId).now();
        if (shoppingList == null) {
            throw new NotFoundException("Cannot find shopping list with id: " + listId);
        }

        List<ShoppingListItem> items = OfyService.ofy().load().type(ShoppingListItem.class).filter("listId", shoppingList.getId()).list();
        for (ShoppingListItem item : items) {
            OfyService.ofy().delete().entity(item).now();
        }
        OfyService.ofy().delete().entity(shoppingList);
        return new CustomResponse(0, "Shopping list is removed");
    }

    @ApiMethod(
            name = "addItem",
            path = "familiy/{famId}/shoppinglist/{shoppingListId}/add",
            httpMethod = ApiMethod.HttpMethod.POST)
    public ShoppingListItem addItem(@Named("famId") Long famId, @Named("shoppingListId") Long listId, ShoppingListItem item, HttpServletRequest request) throws NotFoundException {
        OfyService.begin();
        SessionManager sessionManager = new SessionManager();
        User user = sessionManager.getUserFromSession(request);
        UserInFamily userInFamily = OfyService.ofy().load().type(UserInFamily.class).filter("familyId", famId).filter("userId", user.getId()).first().now();
        assert userInFamily.getId() != null;
        if (!user.getId().equals(userInFamily.getUserId())) {
            throw new AssertionError("You are not a member of this family");
        }
        ShoppingList shoppingList = OfyService.ofy().load().type(ShoppingList.class).id(listId).now();
        if (shoppingList == null) {
            throw new NotFoundException("Cannot find shopping list with name: " + listId);
        }
        item.setIsCompleted(false);
        item.setListId(listId);
        OfyService.ofy().save().entity(item).now();
        assert item.getId() != null;
        return item;
    }

    @ApiMethod(
            name = "removeItem",
            path = "familiy/{famId}/shoppinglist/{listId}/remove/{itemId}",
            httpMethod = ApiMethod.HttpMethod.POST)
    public CustomResponse removeItem(@Named("famId") Long famId, @Named("listId") Long listId, @Named("itemId") Long itemId, HttpServletRequest request) throws NotFoundException {
        OfyService.begin();
        SessionManager sessionManager = new SessionManager();
        User user = sessionManager.getUserFromSession(request);
        UserInFamily userInFamily = OfyService.ofy().load().type(UserInFamily.class).filter("familyId", famId).filter("userId", user.getId()).first().now();
        assert userInFamily.getId() != null;
        if (!user.getId().equals(userInFamily.getUserId())) {
            throw new AssertionError("You are not a member of this family");
        }
        ShoppingList shoppingList = OfyService.ofy().load().type(ShoppingList.class).id(listId).now();
        if (shoppingList == null) {
            throw new NotFoundException("Cannot find shopping list with id: " + listId);
        }
        ShoppingListItem item = OfyService.ofy().load().type(ShoppingListItem.class).id(itemId).now();
        assert item != null;
        OfyService.ofy().delete().entity(item).now();
        return new CustomResponse(0, "Item is deleteted from the list");
    }

    @ApiMethod(
            name = "updateItem",
            path = "familiy/{famId}/shoppinglist/{shoppingListId}/update/{listItemId}",
            httpMethod = ApiMethod.HttpMethod.POST)
    public ShoppingListItem updateItem(@Named("famId") Long id, @Named("shoppingListId") Long listId, @Named("listItemId") Long itemId, ShoppingListItem recievedItem, HttpServletRequest request) throws NotFoundException {
        OfyService.begin();
        SessionManager sessionManager = new SessionManager();
        User user = sessionManager.getUserFromSession(request);
        UserInFamily userInFamily = OfyService.ofy().load().type(UserInFamily.class).filter("familyId", id).filter("userId", user.getId()).first().now();
        assert userInFamily.getId() != null;
        if (!user.getId().equals(userInFamily.getUserId())) {
            throw new AssertionError("You are not a member of this family");
        }
        ShoppingList shoppingList = OfyService.ofy().load().type(ShoppingList.class).id(listId).now();
        if (shoppingList == null) {
            throw new NotFoundException("Cannot find shopping list with id: " + listId);
        }
        ShoppingListItem item = OfyService.ofy().load().type(ShoppingListItem.class).id(itemId).now();
        assert item.getId() != null;
        item.setIsCompleted(recievedItem.isCompleted());
        item.setQty(recievedItem.getQty());
        item.setItemName(recievedItem.getItemName());
        OfyService.ofy().save().entity(item).now();
        return item;
    }

    @ApiMethod(
            name = "getShoppingLists",
            path = "shoppinglists",
            httpMethod = ApiMethod.HttpMethod.GET)
    public List<Family> getShoppingLists(HttpServletRequest request) throws NotFoundException {
        OfyService.begin();
        SessionManager sessionManager = new SessionManager();
        User user = sessionManager.getUserFromSession(request);
        List<UserInFamily> inFamilyQuery = OfyService.ofy().load().type(UserInFamily.class).filter("userId", user.getId()).list();

        List<Family> families = new ArrayList<>();

        for (UserInFamily inFamily : inFamilyQuery) {
            Family family = OfyService.ofy().load().type(Family.class).id(inFamily.getFamilyId()).now();
            assert family != null;
            List<ShoppingList> shoppingLists = OfyService.ofy().load().type(ShoppingList.class).filter("familyId", family.getId()).list();
            if (shoppingLists != null) {
                family.setShoppingLists(shoppingLists);
            }
            families.add(family);
        }
        return families;
    }

    @ApiMethod(
            name = "getItems",
            path = "shoppinglist/{shoppinglistId}/getItems",
            httpMethod = ApiMethod.HttpMethod.GET)
    public List<ShoppingListItem> getItems(@Named("shoppinglistId") Long listId,HttpServletRequest request) throws NotFoundException {
        OfyService.begin();
        SessionManager sessionManager = new SessionManager();
        User user = sessionManager.getUserFromSession(request);
        ShoppingList shoppingList = OfyService.ofy().load().type(ShoppingList.class).id(listId).now();
        if (shoppingList == null) {
            throw new NotFoundException("Cannot find shopping list with id: " + listId);
        }
        UserInFamily userInFamily = OfyService.ofy().load().type(UserInFamily.class).filter("familyId", shoppingList.getFamilyId()).filter("userId", user.getId()).first().now();
        if (userInFamily == null) {
            throw new AssertionError("You are not a member of this family");
        }
        return OfyService.ofy().load().type(ShoppingListItem.class).filter("listId",shoppingList.getId()).list();

    }

}