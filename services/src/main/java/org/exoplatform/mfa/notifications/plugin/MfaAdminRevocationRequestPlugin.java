/*
 * Copyright (C) 2020 eXo Platform SAS.
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
package org.exoplatform.mfa.notifications.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.BaseNotificationPlugin;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.mfa.notifications.utils.MfaNotificationUtils;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.notification.LinkProviderUtils;
import org.exoplatform.social.notification.Utils;

public class MfaAdminRevocationRequestPlugin extends BaseNotificationPlugin {

    public static final String ID = "MfaAdminRevocationRequestPlugin";

    private static final Log LOGGER = ExoLogger.getExoLogger(MfaAdminRevocationRequestPlugin.class);

    private OrganizationService organizationService;

    private UserACL userACL;

    public MfaAdminRevocationRequestPlugin(InitParams initParams, OrganizationService organizationService, UserACL userACL) {
        super(initParams);
        this.organizationService = organizationService;
        this.userACL = userACL;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public NotificationInfo makeNotification(NotificationContext ctx) {
        String userId = ctx.value(MfaNotificationUtils.MFA_REVOCATION_REQUEST_REQUESTER);
        Identity identity =
            Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, userId);
        try {

            List<String> recipients = getRecipients();


            recipients.remove(userId);
            return NotificationInfo.instance().key(getId())
                    .with("username", userId)
                    .with("fullname", identity.getProfile().getFullName())
                    .with("avatar", LinkProviderUtils.getUserAvatarUrl(identity.getProfile()))
                    .with("url", MfaNotificationUtils.getMfaAdminURL())
                    .to(recipients);
        } catch (Exception e) {
            ctx.setException(e);
        }

        return null;
    }

    @Override
    public boolean isValid(NotificationContext ctx) {
        return true;
    }

    private List<String> getRecipients() {
        List<String> members = new ArrayList<>();
        try {
            ListAccess<User> administrators = organizationService.getUserHandler().findUsersByGroupId(userACL.getAdminGroups());
            int totalAdminGroupMembersSize = administrators.getSize();
            User[] users = administrators.load(0, totalAdminGroupMembersSize);
            return Arrays.stream(users)
                            .map(User::getUserName)
                            .collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.error("Error when getting Admin group members");
        }
        return members;
    }
}
