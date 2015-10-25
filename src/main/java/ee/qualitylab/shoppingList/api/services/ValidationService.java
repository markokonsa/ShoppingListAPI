package ee.qualitylab.shoppingList.api.services;

import ee.qualitylab.shoppingList.api.entities.User;

/**
 * Created by Marko on 25.10.2015.
 */
public class ValidationService {

    public static void validateRegistrationDetails(User user){
        if (user.getUserName().isEmpty() ||
                user.getPassword().isEmpty() ||
                user.getFirstName().isEmpty() ||
                user.getLastName().isEmpty()){
            throw new AssertionError("Registration failed! Some of the fields are empty");
        }
        }
    }
