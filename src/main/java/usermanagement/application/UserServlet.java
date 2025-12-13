package usermanagement.application;

import connector.Facade;


import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/UserServlet")
public class UserServlet extends HttpServlet {

    final Logger logger = Logger.getLogger(getClass().getName());
    static final Facade facade = new Facade();

    private static final String ACTION_LOGIN = "login";
    private static final String ACTION_LOGOUT = "logout";
    private static final String ACTION_VIEW_PROFILE = "viewprofile";
    private static final String USERNAME = "username";
    private static final String LOGIN_ERROR = "loginError";
    private static final String PAGE_DASHBOARD = "dashboard.jsp";
    private static final String PAGE_LOGIN = "login.jsp";
    private static final String PAGE_HOME = "index.jsp";
    private static final String PAGE_USER_DETAILS = "userDetails.jsp";

    @Override
    public void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        final String action = request.getParameter("submit");

        if (action == null) {
            return;
        }

        try {
            // Switch statement is cleaner than multiple if-else blocks
            switch (action.toLowerCase()) {
                case ACTION_LOGIN:
                    handleLogin(request, response);
                    break;
                case ACTION_LOGOUT:
                    handleLogout(request, response);
                    break;
                case ACTION_VIEW_PROFILE:
                    handleViewProfile(response);
                    break;
                default:
                    logger.log(Level.SEVERE, "Unknown action: {0}", action);
            }
        } catch (final Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Handles the user login process.
     */
    private void handleLogin(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        final String username = request.getParameter(USERNAME);
        final String password = request.getParameter("password");

        logger.log(Level.INFO,"Attempting login for user: {0}", username);

        // 1. Validation Logic: Check username first
        if (!usernameCheck(username)) {
            logger.info("Login error: User not registered");
            request.setAttribute(LOGIN_ERROR, "Utente non registrato");
            forwardToLogin(request, response);
            return;
        }

        // 2. Validation Logic: Check password
        if (!passwordCheck(username, password)) {
            logger.info("Login error: Wrong password");
            request.setAttribute(LOGIN_ERROR, "Password errata");
            forwardToLogin(request, response);
            return;
        }

        // 3. Success Logic: Retrieve user and setup session
        logger.info("User authenticated successfully");
        final ArrayList<UserBean> users = (ArrayList<UserBean>) facade.findUsers(USERNAME, username);

        if (users != null && !users.isEmpty()) {
            final UserBean user = users.get(0);
            final HttpSession session = request.getSession(true);
            session.setAttribute("currentSessionUser", user);

            // Redirect based on user type (Medical staff or Warehouse)
            if (user.getType() == 1 || user.getType() == 2) {
                response.sendRedirect(PAGE_DASHBOARD);
            } else {
                // Fallback for other user types (optional)
                response.sendRedirect(PAGE_HOME);
            }
        } else {
            // Edge case: Authentication passed but user not found in DB list
            request.setAttribute(LOGIN_ERROR, "Errore recupero dati utente");
            forwardToLogin(request, response);
        }
    }

    /**
     * Handles user logout and invalidates the session.
     */
    private void handleLogout(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        logger.info("Processing logout");
        final HttpSession session = request.getSession(false); // Do not create if not exists
        if (session != null) {
            session.removeAttribute("currentSessionUser");
            session.invalidate();
        }
        logger.info("Redirecting to homepage");
        response.sendRedirect(PAGE_HOME);
    }

    /**
     * Redirects to the user profile page.
     */
    private void handleViewProfile(final HttpServletResponse response) throws IOException {
        // The JSP will handle retrieving the user from the session
        response.sendRedirect(PAGE_USER_DETAILS);
    }

    /**
     * Helper method to forward requests back to the login page with errors.
     */
    private void forwardToLogin(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        final RequestDispatcher requestDispatcher = request.getRequestDispatcher(PAGE_LOGIN);
        requestDispatcher.forward(request, response);
    }

    /*
    Il metodo recupera dal db le istanze di 'utente' in cui appare 'username': se username e password sono
    entrambi corretti, allora il metodo restituisce true, perchÃ¨ l'utente ha accesso al sito
     */
    private boolean usernameCheck(final String username) {
        logger.info("Chiamata db");
        final ArrayList<UserBean> users = (ArrayList<UserBean>) facade.findUsers(USERNAME, username);
        logger.info("Controllo utente effettuato");
        boolean valid = false;
        for(final UserBean us : users){
            if (us.getUsername().equals(username)) {
                valid = true;
                break;
            }
        }
        return valid;
    }

    private boolean passwordCheck(final String username, final String password) {
        logger.info("Chiamata db");
        final ArrayList<UserBean> users = (ArrayList<UserBean>) facade.findUsers(USERNAME, username);
        logger.info("Controllo utente effettuato");
        boolean result = false;
        for(final UserBean us : users){
            if (us.getPassword().equals(password)) {
                result = true;
                break;
            }
        }
        return result;
    }
}
