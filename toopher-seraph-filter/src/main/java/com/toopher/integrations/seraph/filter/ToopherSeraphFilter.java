package com.toopher.integrations.seraph.filter;

import com.toopher.ToopherIframe;
import org.apache.log4j.Category;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigInteger;
import java.security.Principal;
import java.security.SecureRandom;
import java.util.Map;

public class ToopherSeraphFilter implements javax.servlet.Filter {
    private static final Category log = Category.getInstance(ToopherSeraphFilter.class);

    private ToopherIframe toopherIframe;
    private String toopherConsumerKey;
    private String toopherConsumerSecret;
    private String toopherApiUrl;
    private boolean automationAllowed;
    private boolean challengeRequired;
    private boolean optOutAllowed;

    private static final String TOOPHER_SUCCESS = "toopherSuccess";
    private static final String TOOPHER_IFRAME_SRC = "toopherIframeSrc";
    private static final String TOOPHER_POSTBACK_URL = "toopherPostbackUrl";
    private static final String TOOPHER_REQUEST_TOKEN = "toopherRequestToken";

    final private String loginUrl = "/";
    private String toopherPluginServletUrl = "/plugins/servlet/toopher";
    private String[] publicDirectories = {
            "/download/resources/com.toopher.integrations.atlassian.servlet.toopher-atlassian:resources/",
            "/rest/gadget/1.0/login", // jira login
            "/login.jsp" // jira login
    };

    public void init(final FilterConfig filterConfig) {
        this.toopherConsumerKey = filterConfig.getInitParameter("TOOPHER_CONSUMER_KEY");
        this.toopherConsumerSecret = filterConfig.getInitParameter("TOOPHER_CONSUMER_SECRET");
        if (this.toopherConsumerKey == null || this.toopherConsumerSecret == null) {
            final String errorMessage =
                    "Please configure your Toopher Confluence Filter, adding your TOOPHER_CONSUMER_KEY and TOOPHER_CONSUMER_SECRET.";
            log.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        if (filterConfig.getInitParameter("BASE_URL") != null) {
            this.toopherApiUrl = filterConfig.getInitParameter("BASE_URL");
        } else {
            this.toopherApiUrl = "https://api.toopher.com/v1/";
        }
        this.toopherIframe = new ToopherIframe(toopherConsumerKey, toopherConsumerSecret, toopherApiUrl);

        if (filterConfig.getInitParameter("OPT_OUT_ALLOWED") != null) {
            this.optOutAllowed = filterConfig.getInitParameter("OPT_OUT_ALLOWED").toLowerCase().equals("true");
        } else {
            this.optOutAllowed = false;
        }

        if (filterConfig.getInitParameter("AUTOMATION_ALLOWED") != null) {
            this.automationAllowed = filterConfig.getInitParameter("AUTOMATION_ALLOWED").toLowerCase().equals("true");
        } else {
            this.automationAllowed = true;
        }

        if (filterConfig.getInitParameter("CHALLENGE_REQUIRED") != null) {
            this.challengeRequired = filterConfig.getInitParameter("CHALLENGE_REQUIRED").toLowerCase().equals("true");
        } else {
            this.challengeRequired = false;
        }

        if (filterConfig.getInitParameter("TOOPHER_SERVLET_URL") != null) {
            this.toopherPluginServletUrl = filterConfig.getInitParameter("TOOPHER_SERVLET_URL");
        }

        if (filterConfig.getInitParameter("TOOPHER_RESOURCES_DIRECTORY") != null) {
            this.publicDirectories = filterConfig.getInitParameter("TOOPHER_RESOURCES_DIRECTORY").split(" ");
        }
    }

    public void destroy() {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        final HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        final HttpSession session = httpServletRequest.getSession();
        final Principal principal = httpServletRequest.getUserPrincipal();

        if (!requestNeedsAuthentication(httpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }

        if (httpServletResponse.isCommitted()) {
            log.warn("Request was committed--cannot authenticate.");
            chain.doFilter(request, response);
            return;
        }

        final String username = principal.getName();
        final String email = ""; // ###TODO does Confluence expose user email somehow?
        final String pairIframeUrl = toopherIframe.pairUri(username, email);
        final String requestToken = (String)session.getAttribute(TOOPHER_REQUEST_TOKEN);
        if (requestToken != null && httpServletRequest.getParameterMap().containsKey("toopher_sig")) {
            // validate postback
            try {
                session.removeAttribute(TOOPHER_REQUEST_TOKEN);
                Map<String, String> validatedData = toopherIframe.validate(request.getParameterMap(), requestToken);

                if (validatedData.containsKey("error_code")) {
                    // check for API errors
                    final String errorCode = validatedData.get("error_code");
                    final String errorMessage = validatedData.get("error_message");
                    if (errorCode.equals(ToopherIframe.PAIRING_DEACTIVATED)) {
                        // User deleted the pairing on their mobile device.
                        // Your server should display a Toopher Pairing iframe so their account can be re-paired
                        session.setAttribute(TOOPHER_IFRAME_SRC, pairIframeUrl);
                    } else if (errorCode.equals(ToopherIframe.USER_OPT_OUT)) {
                        // User has been marked as "Opt-Out" in the Toopher API
                        // If your service allows opt-out, the user should be granted access.
                        if (this.optOutAllowed) {
                            session.setAttribute(TOOPHER_SUCCESS, true);
                            chain.doFilter(request, response);
                        } else {
                            log.info("User (" + username + ") opted out of Toopher authentication and was denied");
                            session.invalidate();
                            httpServletResponse.sendRedirect(loginUrl);
                            return;
                        }
                    } else if (errorCode.equals(ToopherIframe.USER_UNKNOWN)) {
                        // User has never authenticated with Toopher on this server
                        // Your server should display a Toopher Pairing iframe so their account can be paired
                        session.setAttribute(TOOPHER_IFRAME_SRC, pairIframeUrl);
                    } else {
                        if (errorCode.equals("601") && errorMessage.contains("Pairing has not been authorized")) {
                            session.setAttribute(TOOPHER_IFRAME_SRC, pairIframeUrl);
                        } else {
                            log.error("Unknown toopher error returned: " + errorCode);
                            session.invalidate();
                            httpServletResponse.sendRedirect(loginUrl);
                            return;
                        }
                    }
                } else {
                    // signature is valid, and no api errors.  check authentication result
                    if (isRequestGranted(validatedData)) {
                        session.setAttribute(TOOPHER_SUCCESS, true);
                        chain.doFilter(request, response);
                        return;
                    } else {
                        log.info("Toopher authentication was denied by " + username);
                        session.invalidate();
                        httpServletResponse.sendRedirect(loginUrl);
                        return;
                    }
                }
            } catch (ToopherIframe.SignatureValidationError e) {
                // signature was invalid.  User should not be authenticated
                // e.getMessage() will return more information about what specifically
                // went wrong (incorrect session token, expired TTL, invalid signature)
                log.warn("Invalid ToopherIframe signature", e);
                session.invalidate();
                httpServletResponse.sendRedirect(loginUrl);
                return;
            }
        } else {
            // first call - serve up a signed iframe link
            final String newRequestToken = generateRequestToken();
            final String authIframeUrl = generateAuthIframeUrl(username, email, newRequestToken);
            session.setAttribute(TOOPHER_REQUEST_TOKEN, newRequestToken);
            session.setAttribute(TOOPHER_IFRAME_SRC, authIframeUrl);
            session.setAttribute(TOOPHER_SUCCESS, null);
        }
        session.setAttribute(TOOPHER_POSTBACK_URL, "/");

        httpServletResponse.sendRedirect(toopherPluginServletUrl);
    }

    private String generateAuthIframeUrl(String username, String email, String newRequestToken) {
        final String action = "Log in";
        final String requesterMetadata = "None";
        final Long ttl = 10L;
        return toopherIframe.authUri(username,
                email,
                action,
                this.automationAllowed,
                this.challengeRequired,
                newRequestToken,
                requesterMetadata,
                ttl);
    }

    private String generateRequestToken() {
        final SecureRandom secureRandom = new SecureRandom();
        return new BigInteger(20 * 8, secureRandom).toString(32);
    }

    private boolean isRequestGranted(Map<String, String> validatedData) {
        final boolean authPending = validatedData.get("pending").toLowerCase().equals("true");
        final boolean authGranted = validatedData.get("granted").toLowerCase().equals("true");

        // authenticationResult is the ultimate result of Toopher second-factor authentication
        return (authGranted && !authPending);
    }

    private boolean isAlreadyToopherAuthenticated(HttpSession session) {
        return session.getAttribute(TOOPHER_SUCCESS) != null && (Boolean)session.getAttribute(TOOPHER_SUCCESS);
    }

    private boolean requestNeedsAuthentication(HttpServletRequest request) {
        final HttpSession session = request.getSession();
        final Principal principal = request.getUserPrincipal();
        final String requestUri = request.getRequestURI();
        return !(principal == null || !uriNeedsAuthentication(requestUri) || isAlreadyToopherAuthenticated(session));
    }

    private boolean uriNeedsAuthentication(String uri) {
        if (uri.equals(this.toopherPluginServletUrl)) {
            return false;
        }

        for (final String directory : this.publicDirectories) {
            if (uri.startsWith(directory)) {
                return false;
            }
        }

        return true;
    }
}
