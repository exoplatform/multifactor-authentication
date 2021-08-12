package org.exoplatform.mfa.notifications.provider;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.annotation.TemplateConfig;
import org.exoplatform.commons.api.notification.annotation.TemplateConfigs;
import org.exoplatform.commons.api.notification.channel.template.AbstractTemplateBuilder;
import org.exoplatform.commons.api.notification.channel.template.TemplateProvider;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.container.xml.InitParams;

import org.exoplatform.mfa.notifications.plugin.MfaAdminRevocationRequestPlugin;
import org.exoplatform.mfa.notifications.utils.MfaNotificationUtils;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.notification.LinkProviderUtils;
import org.exoplatform.social.notification.Utils;
import org.exoplatform.social.notification.plugin.SocialNotificationUtils;

import java.io.Writer;

@TemplateConfigs(templates = {
    @TemplateConfig(pluginId = MfaAdminRevocationRequestPlugin.ID, template = "war:/notifications/templates/mail/MfaAdminRevocationRequestPlugin.gtmpl"),
})
public class MailTemplateProvider extends TemplateProvider {

  private final IdentityManager identityManager;

  public MailTemplateProvider(InitParams initParams, IdentityManager identityManager) {
    super(initParams);
    this.templateBuilders.put(PluginKey.key(MfaAdminRevocationRequestPlugin.ID), mfaAdminRevocationRequest);
    this.identityManager = identityManager;
  }

  /** Defines the template builder for MfaAdminRevocationRequestPlugin*/
  private AbstractTemplateBuilder mfaAdminRevocationRequest = new AbstractTemplateBuilder() {
    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      MessageInfo messageInfo = new MessageInfo();

      NotificationInfo notification = ctx.getNotificationInfo();

      String language = getLanguage(notification);
      TemplateContext templateContext = new TemplateContext(notification.getKey().getId(), language);
      SocialNotificationUtils.addFooterAndFirstName(notification.getTo(), templateContext);

      Identity identity =
          identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, notification.getValueOwnerParameter(
              "username"));


      templateContext.put("USERNAME", notification.getValueOwnerParameter("username"));
      templateContext.put("MFA_ADMIN_PAGE_URL", MfaNotificationUtils.getMfaAdminURL());
      templateContext.put("USER", Utils.addExternalFlag(identity));
      templateContext.put("AVATAR", LinkProviderUtils.getUserAvatarUrl(identity.getProfile()));
      templateContext.put("PROFILE_URL", LinkProviderUtils.getRedirectUrl("user", identity.getRemoteId()));

      String subject = TemplateUtils.processSubject(templateContext);
      String body = TemplateUtils.processGroovy(templateContext);
      //binding the exception throws by processing template
      ctx.setException(templateContext.getException());

      return messageInfo.subject(subject).body(body).end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      return false;
    }
  };
}
