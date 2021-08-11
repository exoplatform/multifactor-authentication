package org.exoplatform.mfa.notifications.utils;

import org.exoplatform.commons.api.notification.model.ArgumentLiteral;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;

public class MfaNotificationUtils {


  public static final ArgumentLiteral<String> MFA_REVOCATION_REQUEST_REQUESTER =new ArgumentLiteral<>(String.class,
                                                                                                      "mfa_revocation_request_requester");
  /**
   * Get the Redirect Mfa page url
   *
   * @return the Mfa page url
   */
  public static String getMfaAdminURL() {
    String portal = PortalContainer.getCurrentPortalContainerName();
    return CommonsUtils.getCurrentDomain()
        + "/"
        + portal
        + "/"
        + "g/:platform:administrators/multifactor-authentication";
  }


}
