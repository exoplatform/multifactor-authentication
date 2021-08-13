package org.exoplatform.mfa.listeners;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.mfa.api.MfaService;
import org.exoplatform.mfa.notifications.plugin.MfaAdminRevocationRequestPlugin;
import org.exoplatform.mfa.notifications.utils.MfaNotificationUtils;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;

public class MfaRevocationRequestListener extends Listener<MfaService, Object> {

    @Override
    public void onEvent(Event<MfaService, Object> event) {
        NotificationContext ctx = NotificationContextImpl.cloneInstance().append(MfaNotificationUtils.MFA_REVOCATION_REQUEST_REQUESTER,
                                                                                 event.getData());
        ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(MfaAdminRevocationRequestPlugin.ID))).execute(ctx);
    }
}
