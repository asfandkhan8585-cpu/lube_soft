package com.lubesoft.service;

import com.lubesoft.dao.UserDAO;
import com.lubesoft.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Authentication and role-based access control service.
 */
public class AuthService {

    private static AuthService instance;
    private static User currentUser;

    private final UserDAO userDAO = new UserDAO();

    private AuthService() {}

    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    /**
     * Attempts login. Returns the User on success, null on failure.
     */
    public User login(String username, String password) {
        try {
            Optional<User> userOpt = userDAO.findByUsername(username);
            if (userOpt.isEmpty()) return null;

            User user = userOpt.get();
            if (!user.isActive()) return null;
            if (!BCrypt.checkpw(password, user.getPasswordHash())) return null;

            currentUser = user;
            return user;
        } catch (SQLException e) {
            return null;
        }
    }

    public void logout() {
        currentUser = null;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Role-based permission check.
     * ADMIN: all permissions
     * MANAGER: inventory, discounts, reports (no cost prices)
     * TECHNICIAN: POS only
     */
    public static boolean hasPermission(String permission) {
        if (currentUser == null) return false;
        String role = currentUser.getRole();

        return switch (role) {
            case "ADMIN" -> true;
            case "MANAGER" -> switch (permission) {
                case "POS", "INVENTORY", "CUSTOMERS", "REPORTS", "DISCOUNTS", "EMPLOYEES_VIEW" -> true;
                case "COST_PRICES", "EMPLOYEES_MANAGE", "SETTINGS" -> false;
                default -> false;
            };
            case "TECHNICIAN" -> switch (permission) {
                case "POS", "CUSTOMERS_VIEW", "TIME_CLOCK" -> true;
                default -> false;
            };
            default -> false;
        };
    }
}
