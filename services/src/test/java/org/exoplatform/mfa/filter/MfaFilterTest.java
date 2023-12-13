package org.exoplatform.mfa.filter;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.mfa.api.MfaService;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MfaFilterTest {

  MockedStatic<PortalContainer> PORTAL_CONTAINER = Mockito.mockStatic(PortalContainer.class);
  MockedStatic<ConversationState> CONVERSATION_STATE = Mockito.mockStatic(ConversationState.class);
  PortalContainer portalContainer = Mockito.mock(PortalContainer.class);
  ConversationState conversationState = Mockito.mock(ConversationState.class);

  @Before
  public void setUp() throws Exception {
    PORTAL_CONTAINER.when(PortalContainer::getInstance).thenReturn(portalContainer);
    CONVERSATION_STATE.when(ConversationState::getCurrent).thenReturn(conversationState);
  }

  @Test
  public void testDoFilter() {
    MfaFilter mfaFilter = new MfaFilter();
    HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
    HttpServletResponse httpServletResponse = Mockito.mock(HttpServletResponse.class);
    FilterChain chain = Mockito.mock(FilterChain.class);
    MfaService mfaService = Mockito.mock(MfaService.class);
    ConversationState conversationState = Mockito.mock(ConversationState.class);
    HttpSession httpSession = Mockito.mock(HttpSession.class);

    when(httpServletRequest.getRemoteUser()).thenReturn("root");
    when(mfaService.isMfaFeatureActivated()).thenReturn(true);
    Identity identity = new Identity("root");
    when(conversationState.getIdentity()).thenReturn(identity);
    when(ConversationState.getCurrent()).thenReturn(conversationState);
    when(mfaService.currentUserIsInProtectedGroup(identity)).thenReturn(true);
    when(httpServletRequest.getRequestURI()).thenReturn("/portal/dw/protectedUri");
    when(httpServletRequest.getQueryString()).thenReturn("initialUri=/portal/dw/protectedUri");
    when(portalContainer.getComponentInstanceOfType(MfaService.class)).thenReturn(mfaService);
    when(httpServletRequest.getSession()).thenReturn(httpSession);
    try {
      mfaFilter.doFilter(httpServletRequest, httpServletResponse, chain);
      verify(chain,times(0)).doFilter(httpServletRequest, httpServletResponse);
    } catch (IOException | ServletException e) {
      fail();
    }
    when(httpSession.getAttribute("mfaValidated")).thenReturn(Boolean.TRUE);
    when(httpServletRequest.getRequestURI()).thenReturn("/portal/dw/mfa-access?initialUri=/portal/dw/spaces");
    try {
      mfaFilter.doFilter(httpServletRequest, httpServletResponse, chain);
      verify(chain,times(0)).doFilter(httpServletRequest, httpServletResponse);
    } catch (IOException | ServletException e) {
      fail();
    }
  }

  @After
  public void tearDown() throws Exception {
    PORTAL_CONTAINER.close();
    CONVERSATION_STATE.close();
  }
}
