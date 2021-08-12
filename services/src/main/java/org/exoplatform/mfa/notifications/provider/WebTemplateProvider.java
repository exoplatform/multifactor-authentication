/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.mfa.notifications.provider;

import java.io.Writer;
import java.util.Calendar;
import java.util.Locale;


import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.NotificationMessageUtils;
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
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.notification.LinkProviderUtils;
import org.exoplatform.social.notification.Utils;
import org.exoplatform.webui.utils.TimeConvertUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          thanhvc@exoplatform.com
 * Dec 14, 2014  
 */


@TemplateConfigs (
   templates = {
       @TemplateConfig(pluginId = MfaAdminRevocationRequestPlugin.ID, template = "war:/notifications/templates/web/MfaAdminRevocationRequestPlugin.gtmpl")
   }
)
public class WebTemplateProvider extends TemplateProvider {

  /**
   * Defines the template builder for MfaAdminRevocationRequestPlugin
   */
  private AbstractTemplateBuilder mfaAdminRevocationRequest = new AbstractTemplateBuilder() {

    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      NotificationInfo notification = ctx.getNotificationInfo();

      String language = getLanguage(notification);

      TemplateContext templateContext = TemplateContext.newChannelInstance(getChannelKey(), notification.getKey().getId(), language);

      Identity identity =
          Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME,notification.getValueOwnerParameter(
              "username"));

      templateContext.put("isIntranet", "true");
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(notification.getLastModifiedDate());
      templateContext.put("READ", Boolean.parseBoolean(notification.getValueOwnerParameter(NotificationMessageUtils.READ_PORPERTY.getKey())) ? "read" : "unread");
      templateContext.put("NOTIFICATION_ID", notification.getId());
      templateContext.put("LAST_UPDATED_TIME", TimeConvertUtils.convertXTimeAgoByTimeServer(cal.getTime(), "EE, dd yyyy", new Locale(language), TimeConvertUtils.YEAR));
      templateContext.put("USERNAME", notification.getValueOwnerParameter("username"));
      templateContext.put("USER", Utils.addExternalFlag(identity));
      templateContext.put("AVATAR", LinkProviderUtils.getUserAvatarUrl(identity.getProfile()));
      templateContext.put("MFA_ADMIN_PAGE_URL", MfaNotificationUtils.getMfaAdminURL());

      //
      String body = TemplateUtils.processGroovy(templateContext);
      //binding the exception throws by processing template
      ctx.setException(templateContext.getException());
      MessageInfo messageInfo = new MessageInfo();
      return messageInfo.body(body).end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      return false;
    }


  };

    
  public WebTemplateProvider(InitParams initParams) {
    super(initParams);
    this.templateBuilders.put(PluginKey.key(MfaAdminRevocationRequestPlugin.ID), mfaAdminRevocationRequest);
  }

}
