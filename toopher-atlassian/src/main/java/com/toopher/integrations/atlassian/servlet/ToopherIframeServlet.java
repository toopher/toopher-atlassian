package com.toopher.integrations.atlassian.servlet;

import com.atlassian.templaterenderer.TemplateRenderer;
import org.apache.log4j.Category;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ToopherIframeServlet extends HttpServlet {
    // These get autowired by Spring
    private static final Category log = Category.getInstance(ToopherIframeServlet.class);
    private final TemplateRenderer renderer;

    // Session keys set by Toopher Seraph Filter
    private static final String TOOPHER_IFRAME_SRC = "toopherIframeSrc";
    private static final String TOOPHER_POSTBACK_URL = "toopherPostbackUrl";

    public ToopherIframeServlet(TemplateRenderer renderer) {
        this.renderer = renderer;
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        final HttpSession session = request.getSession();
        final String iframeUrl = (String)session.getAttribute(TOOPHER_IFRAME_SRC);
        final String postbackUrl = (String)session.getAttribute(TOOPHER_POSTBACK_URL);
        Map<String, Object> velocityContext = new HashMap<String, Object>();
        velocityContext.put("IFRAME_REQUEST_URL", iframeUrl);
        velocityContext.put("POSTBACK_URL", postbackUrl);

        renderer.render("toopher.vm", velocityContext, response.getWriter());
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            response.sendRedirect(UriBuilder.fromUri("/").build().toString());
    }
}
