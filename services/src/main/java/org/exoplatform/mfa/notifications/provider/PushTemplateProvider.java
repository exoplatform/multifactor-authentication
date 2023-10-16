/*
 * Copyright (C) 2021 eXo Platform SAS.
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

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.annotation.TemplateConfig;
import org.exoplatform.commons.api.notification.annotation.TemplateConfigs;
import org.exoplatform.commons.api.notification.channel.template.AbstractTemplateBuilder;
import org.exoplatform.commons.api.notification.channel.template.TemplateProvider;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.mfa.notifications.plugin.MfaAdminRevocationRequestPlugin;
import org.exoplatform.mfa.notifications.utils.MfaNotificationUtils;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.notification.Utils;

import java.io.Writer;

@TemplateConfigs(templates = {
    @TemplateConfig(pluginId = MfaAdminRevocationRequestPlugin.ID, template = "war:/notifications/templates/push/MfaAdminRevocationRequestPlugin.gtmpl")
})
public class PushTemplateProvider extends TemplateProvider {

  public PushTemplateProvider(InitParams initParams) {
    super(initParams);
    this.templateBuilders.put(PluginKey.key(MfaAdminRevocationRequestPlugin.ID), new TemplateBuilder());
  }

  /** Defines the template builder for MfaRevocationRequestPlugin*/
  private class TemplateBuilder extends AbstractTemplateBuilder {
    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      NotificationInfo notification = ctx.getNotificationInfo();
      String language = getLanguage(notification);
      TemplateContext
          templateContext = TemplateContext.newChannelInstance(getChannelKey(), notification.getKey().getId(), language);
      Identity identity =
          Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, notification.getValueOwnerParameter(
              "username"));
      templateContext.put("USER", Utils.addExternalFlag(identity));
      ctx.setException(templateContext.getException());

      MessageInfo messageInfo = new MessageInfo();
      return messageInfo.subject(MfaNotificationUtils.getMfaAdminURL()).end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      return false;
    }
  }
}
