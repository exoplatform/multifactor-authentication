package org.exoplatform.mfa.filter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.mfa.api.MfaService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.web.filter.Filter;

public class MfaFilter implements Filter {
  
  private static final String MFA_URI = "/portal/dw/mfa-access";
  private static final Log    LOG     = ExoLogger.getLogger(MfaFilter.class);
  private List<String> excludedUrls = new ArrayList<>(
      Arrays.asList("/portal/skins",
                    "/portal/scripts",
                    "/portal/javascript",
                    "/portal/rest",
                    "/portal/service-worker.js",
                    MFA_URI
      )
  );
  
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpServletRequest = (HttpServletRequest) request;
    HttpServletResponse httpServletResponse = (HttpServletResponse) response;
    HttpSession session = httpServletRequest.getSession();
    PortalContainer container = PortalContainer.getInstance();
    MfaService mfaService = container.getComponentInstanceOfType(MfaService.class);

    String requestUri = httpServletRequest.getRequestURI();
    if (httpServletRequest.getRemoteUser() != null && mfaService.isMfaFeatureActivated() && (mfaService.isProtectedUri(requestUri)
        || mfaService.currentUserIsInProtectedGroup(ConversationState.getCurrent().getIdentity()))) {
      if (shouldAuthenticateFromSession(session) && excludedUrls.stream().noneMatch(requestUri::startsWith)) {
        LOG.debug("Mfa Filter must redirect on page to fill token");
        httpServletResponse.sendRedirect(MFA_URI + "?initialUri=" + requestUri);
        return;
      } else if (!shouldAuthenticateFromSession(session) && requestUri.startsWith(MFA_URI)) {
        String queryString = httpServletRequest.getQueryString();
        String initialUri = "/";
        if (StringUtils.isNotBlank(queryString) && queryString.contains("initialUri=")) {
          initialUri = queryString.substring(11);
        }
        httpServletResponse.sendRedirect(initialUri);
        return;
      }
    }
    chain.doFilter(request, response);

  }

  private boolean shouldAuthenticateFromSession(HttpSession session) {
    if (session.getAttribute("mfaValidated") == null) return true;
    if (!(boolean) session.getAttribute("mfaValidated")) return true;
    Instant expiration = (Instant)session.getAttribute("mfaExpiration");
    return expiration != null &&
        expiration.isBefore(Instant.now());
  }
}
