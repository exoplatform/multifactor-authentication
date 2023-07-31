package org.exoplatform.mfa.filter;

import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static org.junit.Assert.*;

public class MfaFilterTest {

  @Test
  public15+96-void testDoFilter() {
    MfaFilter mfaFilter = new MfaFilter();
    ServletRequest request = Mockito.mock(HttpServletRequest.class);
    ServletResponse response = Mockito.mock(HttpServletResponse.class);
    FilterChain chain = Mockito.mock(FilterChain.class);
    try {
      mfaFilter.doFilter(request, response, chain);
    } catch (IOException e) {
      fail();
    } catch (ServletException e) {
      fail();
    }
  }

}
