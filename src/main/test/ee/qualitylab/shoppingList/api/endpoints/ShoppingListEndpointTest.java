package ee.qualitylab.shoppingList.api.endpoints;

import com.google.api.server.spi.response.NotFoundException;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import ee.qualitylab.shoppingList.api.entities.Family;
import ee.qualitylab.shoppingList.api.entities.Session;
import ee.qualitylab.shoppingList.api.entities.ShoppingList;
import ee.qualitylab.shoppingList.api.entities.ShoppingListItem;
import ee.qualitylab.shoppingList.api.entities.User;
import ee.qualitylab.shoppingList.api.services.OfyService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.BDDMockito.given;

public class ShoppingListEndpointTest {

    private final LocalServiceTestHelper helper =
            new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

    ShoppingListEndpoint shoppingListEndpoint;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        helper.setUp();
        OfyService.begin();
        shoppingListEndpoint = new ShoppingListEndpoint();
    }

    @After
    public void tearDown() throws Exception {
        helper.tearDown();
    }

    @Test
    public void testValidRegistration() throws NotFoundException {
        shoppingListEndpoint.registerUser(mockUser(), mockHttpServletRequestHeader()).getSessionKey();
    }

    @Test
    public void testInvalidRegistration() throws NotFoundException {
        expectedEx.expect(AssertionError.class);
        expectedEx.expectMessage("Registration failed! Some of the fields are empty");
        User user = mockUser();
        user.setPassword("");
        shoppingListEndpoint.registerUser(user, mockHttpServletRequestHeader()).getSessionKey();
    }

    @Test
    public void testUsernameAlreadyInUse() throws NotFoundException {
        expectedEx.expect(AssertionError.class);
        expectedEx.expectMessage("User with username: "+mockUser().getUserName()+" already exists");
        shoppingListEndpoint.registerUser(mockUser(), mockHttpServletRequestHeader()).getSessionKey();
        shoppingListEndpoint.registerUser(mockUser(), mockHttpServletRequestHeader()).getSessionKey();
    }

    @Test
    public void testLogin() throws Exception {
        shoppingListEndpoint.registerUser(mockUser(), mockHttpServletRequestHeader()).getSessionKey();
        Session session = shoppingListEndpoint.login(mockUser().getUserName(), mockUser().getPassword(), mockHttpServletRequestHeader());
        assertNotNull(session);
    }

    @Test
    public void testInvalidLogin() throws Exception {
        expectedEx.expect(NotFoundException.class);
        expectedEx.expectMessage("Invalid login credentials");
        shoppingListEndpoint.registerUser(mockUser(), mockHttpServletRequestHeader()).getSessionKey();
        shoppingListEndpoint.login(mockUser2().getUserName(), mockUser().getPassword(), mockHttpServletRequestHeader());
    }

    @Test
    public void testAddFamilty() throws Exception {
        Session session = shoppingListEndpoint.registerUser(mockUser(), mockHttpServletRequestHeader());
        Family family = shoppingListEndpoint.addFamilty("TestFamily", mockHttpServletRequestHeader());
        assertNotNull(family);
    }

    @Test
    public void testAddFamiltyWithinvalidSessionKey() throws Exception {
        expectedEx.expect(NotFoundException.class);
        shoppingListEndpoint.addFamilty("TestFamily", mockHttpServletRequestHeader());
    }

    @Test
    public void testAddFamilyMember() throws Exception {
        shoppingListEndpoint.registerUser(mockUser(), mockHttpServletRequestHeader());
        Session session = shoppingListEndpoint.registerUser(mockUser2(), mockHttpServletRequestHeader());
        Family family = shoppingListEndpoint.addFamilty("TestFamily", mockHttpServletRequestHeader());
        Family fam = shoppingListEndpoint.addFamilyMember(family.getId(), session.getUserId(), mockHttpServletRequestHeader());
        assertNotNull(fam);
    }

    @Test
    public void testAddFamilyAlreadyAMember() throws Exception {
        expectedEx.expect(AssertionError.class);
        Session session = shoppingListEndpoint.registerUser(mockUser(), mockHttpServletRequestHeader());
        Family family = shoppingListEndpoint.addFamilty("TestFamily", mockHttpServletRequestHeader());
        Family fam = shoppingListEndpoint.addFamilyMember(family.getId(), session.getUserId(), mockHttpServletRequestHeader());
        assertNotNull(fam);
    }

    @Test
    public void testRemoveFamilyMember() throws Exception {
        shoppingListEndpoint.registerUser(mockUser(), mockHttpServletRequestHeader());
        Session session = shoppingListEndpoint.registerUser(mockUser2(), mockHttpServletRequestHeader());
        Family family = shoppingListEndpoint.addFamilty("TestFamily", mockHttpServletRequestHeader());
        shoppingListEndpoint.addFamilyMember(family.getId(), session.getUserId(), mockHttpServletRequestHeader());
        Family finalFamily = shoppingListEndpoint.removeFamilyMember(family.getId(), session.getUserId(), mockHttpServletRequestHeader());
        assertNotNull(finalFamily);
    }

    @Test
    public void testRemoveFamilyMemberAlreadyRemoved() throws Exception {
        expectedEx.expect(AssertionError.class);
        shoppingListEndpoint.registerUser(mockUser(), mockHttpServletRequestHeader());
        Session session = shoppingListEndpoint.registerUser(mockUser2(), mockHttpServletRequestHeader());
        Family family = shoppingListEndpoint.addFamilty("TestFamily", mockHttpServletRequestHeader());
        shoppingListEndpoint.addFamilyMember(family.getId(), session.getUserId(), mockHttpServletRequestHeader());
        shoppingListEndpoint.removeFamilyMember(family.getId(), session.getUserId(), mockHttpServletRequestHeader());
        shoppingListEndpoint.removeFamilyMember(family.getId(), session.getUserId(), mockHttpServletRequestHeader());
    }

    @Test
    public void testAddShopingList() throws Exception {
        Session session = shoppingListEndpoint.registerUser(mockUser(), mockHttpServletRequestHeader());
        Family family = shoppingListEndpoint.addFamilty("TestFamily", mockHttpServletRequestHeader());
        ShoppingList shoppingList = shoppingListEndpoint.addShopingList(family.getId(), "ShoppingList", mockHttpServletRequestHeader());
        assertNotNull(shoppingList);
    }

    @Test
    public void testRemoveShopingList() throws Exception {
        Session session = shoppingListEndpoint.registerUser(mockUser(), mockHttpServletRequestHeader());
        Family family = shoppingListEndpoint.addFamilty("TestFamily", mockHttpServletRequestHeader());
        ShoppingList shoppingList = shoppingListEndpoint.addShopingList(family.getId(), "ShoppingList", mockHttpServletRequestHeader());
        shoppingListEndpoint.removeShopingList(family.getId(), shoppingList.getId(), mockHttpServletRequestHeader());
    }

    @Test
    public void testRemoveShopingListAlreadyRemoved() throws Exception {
        expectedEx.expect(NotFoundException.class);
        Session session = shoppingListEndpoint.registerUser(mockUser(), mockHttpServletRequestHeader());
        Family family = shoppingListEndpoint.addFamilty("TestFamily", mockHttpServletRequestHeader());
        ShoppingList shoppingList = shoppingListEndpoint.addShopingList(family.getId(), "ShoppingList", mockHttpServletRequestHeader());
        shoppingListEndpoint.removeShopingList(family.getId(), shoppingList.getId(), mockHttpServletRequestHeader());
        shoppingListEndpoint.removeShopingList(family.getId(), shoppingList.getId(), mockHttpServletRequestHeader());
    }

    @Test
    public void testAddItem() throws Exception {
        Session session = shoppingListEndpoint.registerUser(mockUser(), mockHttpServletRequestHeader());
        Family family = shoppingListEndpoint.addFamilty("TestFamily", mockHttpServletRequestHeader());
        ShoppingList shoppingList = shoppingListEndpoint.addShopingList(family.getId(), "ShoppingList", mockHttpServletRequestHeader());
        ShoppingListItem item = shoppingListEndpoint.addItem(family.getId(), shoppingList.getId(), mockShoppinglistItem(shoppingList.getId()), mockHttpServletRequestHeader());
        assertNotNull(item);

    }

    @Test
    public void testRemoveItem() throws Exception {
        Session session = shoppingListEndpoint.registerUser(mockUser(), mockHttpServletRequestHeader());
        Family family = shoppingListEndpoint.addFamilty("TestFamily", mockHttpServletRequestHeader());
        ShoppingList shoppingList = shoppingListEndpoint.addShopingList(family.getId(), "ShoppingList", mockHttpServletRequestHeader());
        ShoppingListItem item = shoppingListEndpoint.addItem(family.getId(), shoppingList.getId(), mockShoppinglistItem(shoppingList.getId()), mockHttpServletRequestHeader());
        shoppingListEndpoint.removeItem(family.getId(), shoppingList.getId(), item.getId(), mockHttpServletRequestHeader());
    }

    @Test
    public void testRemoveItemAlreadyRemoved() throws Exception {
        expectedEx.expect(AssertionError.class);
        Session session = shoppingListEndpoint.registerUser(mockUser(), mockHttpServletRequestHeader());
        Family family = shoppingListEndpoint.addFamilty("TestFamily", mockHttpServletRequestHeader());
        ShoppingList shoppingList = shoppingListEndpoint.addShopingList(family.getId(), "ShoppingList", mockHttpServletRequestHeader());
        ShoppingListItem item = shoppingListEndpoint.addItem(family.getId(), shoppingList.getId(), mockShoppinglistItem(shoppingList.getId()), mockHttpServletRequestHeader());
        shoppingListEndpoint.removeItem(family.getId(),shoppingList.getId(),item.getId(),mockHttpServletRequestHeader());
        shoppingListEndpoint.removeItem(family.getId(),shoppingList.getId(),item.getId(),mockHttpServletRequestHeader());
    }

    @Test
    public void testUpdateItem() throws Exception {
        Session session = shoppingListEndpoint.registerUser(mockUser(), mockHttpServletRequestHeader());
        Family family = shoppingListEndpoint.addFamilty("TestFamily", mockHttpServletRequestHeader());
        ShoppingList shoppingList = shoppingListEndpoint.addShopingList(family.getId(), "ShoppingList", mockHttpServletRequestHeader());
        ShoppingListItem item = shoppingListEndpoint.addItem(family.getId(), shoppingList.getId(), mockShoppinglistItem(shoppingList.getId()), mockHttpServletRequestHeader());
        item.setIsCompleted(true);
        item.setQty(999);
        item = shoppingListEndpoint.updateItem(family.getId(),shoppingList.getId(),item.getId(),item,mockHttpServletRequestHeader());
        assertEquals(item.isCompleted(), true);
        assertEquals(item.getQty(), 999);
    }

    @Test
    public void testGetShoppingLists() throws Exception {
        shoppingListEndpoint.registerUser(mockUser(), mockHttpServletRequestHeader());
        Family family1 = shoppingListEndpoint.addFamilty("TestFamily1", mockHttpServletRequestHeader());
        Family family2 = shoppingListEndpoint.addFamilty("TestFamily2", mockHttpServletRequestHeader());
        Family family3 = shoppingListEndpoint.addFamilty("TestFamily3", mockHttpServletRequestHeader());
        shoppingListEndpoint.addShopingList(family1.getId(), "ShoppingList1", mockHttpServletRequestHeader());
        shoppingListEndpoint.addShopingList(family1.getId(), "ShoppingList2", mockHttpServletRequestHeader());
        shoppingListEndpoint.addShopingList(family2.getId(), "ShoppingList3", mockHttpServletRequestHeader());
        shoppingListEndpoint.addShopingList(family2.getId(), "ShoppingList4", mockHttpServletRequestHeader());
        shoppingListEndpoint.addShopingList(family3.getId(), "ShoppingList5", mockHttpServletRequestHeader());
        shoppingListEndpoint.addShopingList(family3.getId(), "ShoppingList6", mockHttpServletRequestHeader());

        List<Family> families = shoppingListEndpoint.getShoppingLists(mockHttpServletRequestHeader());
        assertEquals(families.size(),3);
        assertEquals(families.get(0).getShoppingLists().size(),2);

    }

    @Test
    public void testGetItems() throws Exception {
        shoppingListEndpoint.registerUser(mockUser(), mockHttpServletRequestHeader());
        Family family = shoppingListEndpoint.addFamilty("TestFamily", mockHttpServletRequestHeader());
        ShoppingList shoppingList = shoppingListEndpoint.addShopingList(family.getId(), "ShoppingList", mockHttpServletRequestHeader());
        ShoppingListItem item1 = shoppingListEndpoint.addItem(family.getId(), shoppingList.getId(), mockShoppinglistItem(shoppingList.getId()), mockHttpServletRequestHeader());
        ShoppingListItem item2 = shoppingListEndpoint.addItem(family.getId(), shoppingList.getId(), mockShoppinglistItem(shoppingList.getId()), mockHttpServletRequestHeader());
        ShoppingListItem item3 = shoppingListEndpoint.addItem(family.getId(), shoppingList.getId(), mockShoppinglistItem(shoppingList.getId()), mockHttpServletRequestHeader());
        ShoppingListItem item4 = shoppingListEndpoint.addItem(family.getId(), shoppingList.getId(), mockShoppinglistItem(shoppingList.getId()), mockHttpServletRequestHeader());
        ShoppingListItem item5 = shoppingListEndpoint.addItem(family.getId(), shoppingList.getId(), mockShoppinglistItem(shoppingList.getId()), mockHttpServletRequestHeader());
        ShoppingListItem item6 = shoppingListEndpoint.addItem(family.getId(), shoppingList.getId(), mockShoppinglistItem(shoppingList.getId()), mockHttpServletRequestHeader());
        ShoppingListItem item7 = shoppingListEndpoint.addItem(family.getId(), shoppingList.getId(), mockShoppinglistItem(shoppingList.getId()), mockHttpServletRequestHeader());

        List<ShoppingListItem> list = shoppingListEndpoint.getItems(shoppingList.getId(),mockHttpServletRequestHeader());
        assertEquals(list.size(),7);

    }

    private User mockUser() {
        User user = new User();
        user.setFirstName("first");
        user.setLastName("last");
        user.setUserName("name");
        user.setPassword("pass");
        return user;
    }

    private User mockUser2() {
        User user = new User();
        user.setFirstName("first2");
        user.setLastName("last2");
        user.setUserName("name2");
        user.setPassword("pass2");
        return user;
    }

    private ShoppingListItem mockShoppinglistItem(Long listId){
        ShoppingListItem item = new ShoppingListItem();
        item.setItemName("Test item");
        item.setQty(1);
        item.setListId(listId);
        return item;
    }

    private HttpServletRequest mockHttpServletRequestHeader() {
        HttpServletRequest mockedRequest = Mockito.mock(HttpServletRequest.class);
        given(mockedRequest.getSession()).willReturn(Mockito.mock(HttpSession.class));
        given(mockedRequest.getSession().getId()).willReturn("testing");
        given(mockedRequest.getHeader("X-API-KEY")).willReturn("testing");
        return mockedRequest;
    }
}