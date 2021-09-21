/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
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
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.mfa.notifications.plugin.MfaAdminRevocationRequestPlugin;
import org.exoplatform.mfa.notifications.utils.MfaNotificationUtils;

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

@TemplateConfigs(templates = {
    @TemplateConfig(pluginId = MfaAdminRevocationRequestPlugin.ID, template = "war:/notifications/templates/push/MfaAdminRevocationRequestPlugin.gtmpl")
})
public class PushTemplateProvider extends WebTemplateProvider {

  private final Map<PluginKey, AbstractTemplateBuilder> webTemplateBuilders = new HashMap<>();

  public PushTemplateProvider(InitParams initParams) {
    super(initParams);
    this.webTemplateBuilders.putAll(this.templateBuilders);
    this.templateBuilders.put(PluginKey.key(MfaAdminRevocationRequestPlugin.ID), mfaRevocationRequest);
  }

  /** Defines the template builder for MfaRevocationRequestPlugin*/
  private AbstractTemplateBuilder mfaRevocationRequest = new AbstractTemplateBuilder() {

    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      MessageInfo messageInfo = webTemplateBuilders.get(new PluginKey(MfaAdminRevocationRequestPlugin.ID)).buildMessage(ctx);

      return messageInfo.subject(MfaNotificationUtils.getMfaAdminURL()).end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      return false;
    }
  };
}
