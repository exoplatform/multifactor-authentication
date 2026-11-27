/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.mfa.notifications;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.mail.MailPalette;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.groovyscript.GroovyTemplate;
import org.exoplatform.mfa.notifications.plugin.MfaAdminRevocationRequestPlugin;
import org.exoplatform.mfa.notifications.provider.MailTemplateProvider;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.notification.LinkProviderUtils;
import org.exoplatform.social.notification.Utils;
import org.exoplatform.social.notification.plugin.SocialNotificationUtils;

/**
 * The revocation request mail is written on the recipient's palette: its
 * builder binds the palette before the template is processed, and the template
 * keeps no palette colour as a literal, shows the logo and renders the bound
 * palette. The template lives in the <code>webapps</code> module and is read
 * from the source tree of the reactor.
 */
public class MfaAdminRevocationMailPaletteTest {

  private static final Path    TEMPLATE        =
                                        Paths.get("../webapps/src/main/webapp/WEB-INF/notifications/templates/mail/MfaAdminRevocationRequestPlugin.gtmpl");

  // the light literals the palette replaces; white on a coloured button stays as written
  private static final Pattern PALETTE_LITERAL = Pattern.compile("(?i)(bgcolor=\"|background-color:\\s*)#(ffffff|f5f5f5|d6d6d6|476a9c)"
      + "|(?<![a-z-])color:\\s*#(333333|333|2f5e92|476a98)|border[a-z-]*:\\s*\\d+px solid\\s*#(c7c7c7|ccc)");

  @Test
  public void theBuilderBindsThePaletteOfTheRecipient() {
    Identity identity = new Identity("1");
    identity.setProfile(new Profile(identity));
    IdentityManager identityManager = mock(IdentityManager.class);
    when(identityManager.getOrCreateIdentity(any(), any())).thenReturn(identity);
    NotificationInfo notification = NotificationInfo.instance()
                                                    .setTo("root")
                                                    .key(PluginKey.key(MfaAdminRevocationRequestPlugin.ID))
                                                    .with("username", "john");
    NotificationContext ctx = mock(NotificationContext.class);
    when(ctx.getNotificationInfo()).thenReturn(notification);
    MailTemplateProvider provider = new MailTemplateProvider(new InitParams(), identityManager);
    try (MockedStatic<SocialNotificationUtils> social = mockStatic(SocialNotificationUtils.class);
         MockedStatic<TemplateUtils> templates = mockStatic(TemplateUtils.class);
         MockedStatic<LinkProviderUtils> links = mockStatic(LinkProviderUtils.class);
         MockedStatic<Utils> utils = mockStatic(Utils.class);
         MockedStatic<org.exoplatform.mfa.notifications.utils.MfaNotificationUtils> mfa =
                                                                                       mockStatic(org.exoplatform.mfa.notifications.utils.MfaNotificationUtils.class);
         MockedStatic<org.exoplatform.commons.api.notification.plugin.NotificationPluginUtils> plugins =
                                                                                                     mockStatic(org.exoplatform.commons.api.notification.plugin.NotificationPluginUtils.class)) {
      plugins.when(() -> org.exoplatform.commons.api.notification.plugin.NotificationPluginUtils.getLanguage("root"))
             .thenReturn("en");

      provider.getTemplateBuilder().get(PluginKey.key(MfaAdminRevocationRequestPlugin.ID)).buildMessage(ctx);

      // the social helper binds the palette of the recipient it is given
      social.verify(() -> SocialNotificationUtils.addFooterAndFirstName(eq("root"), any(TemplateContext.class)));
    }
  }

  @Test
  public void theTemplateKeepsNoPaletteColourAsALiteralAndShowsTheLogo() throws Exception {
    String text = read();
    assertFalse("The template keeps a palette colour as a literal", PALETTE_LITERAL.matcher(text).find());
    assertTrue(text.contains("MailPalette.of(_templateContext)"));
    assertTrue(text.contains("war:/notification/templates/mail/NotificationLogo.gtmpl"));
  }

  @Test
  public void theBoundPaletteReachesTheRenderedMail() throws Exception {
    Map<String, String> palette = new HashMap<>();
    palette.put("surface", "#1E2328");
    palette.put("background", "#14181B");
    palette.put("text", "#E6EAEE");
    palette.put("secondaryText", "#9AA4AD");
    palette.put("link", "#5FA9AC");
    palette.put("border", "#3A4148");
    palette.put("primary", "#3F8487");
    palette.put(MailPalette.MODE, "dark");
    List<String> included = new ArrayList<>();
    String html = render(palette, included);
    assertTrue(html, html.contains("bgcolor=\"#14181B\""));
    assertTrue(html, html.contains("bgcolor=\"#1E2328\""));
    assertTrue(html, html.contains("color:#E6EAEE"));
    assertTrue(html, html.contains("class=\"mail-bg mail-link mail-border\""));
    assertTrue("the body text cell is painted", html.contains("<td align=\"left\" bgcolor=\"#1E2328\" style=\"background-color: #1E2328; line-height: 18px;font-size: 14px;color:#E6EAEE;"));
    assertTrue("the body cell is painted", html.contains("bgcolor=\"#1E2328\" style=\"background-color: #1E2328; padding: 25px 0 25px\" class=\"mail-surface\""));
    assertTrue("the body cells carry the surface class", html.contains("class=\"mail-surface mail-text\""));
    assertTrue(html, html.contains("background-color: #3F8487"));
    assertFalse(html, html.contains("#d6d6d6"));
    assertTrue(included.toString(), included.contains("war:/notification/templates/mail/NotificationLogo.gtmpl"));
  }

  @Test
  public void aContextWithoutPaletteRendersInTheLightColours() throws Exception {
    String html = render(null, new ArrayList<>()).toLowerCase();
    assertTrue(html, html.contains("bgcolor=\"#ffffff\""));
    assertTrue(html, html.contains("bgcolor=\"#f0f0f0\""));
    assertFalse(html, html.contains("null"));
  }

  private String read() throws Exception {
    Assert.assertTrue("The mail template of the webapps module is not reachable from this module", Files.exists(TEMPLATE));
    return Files.readString(TEMPLATE, StandardCharsets.UTF_8);
  }

  private String render(Map<String, String> palette, List<String> included) throws Exception {
    Map<String, Object> templateContext = new HashMap<>();
    if (palette != null) {
      templateContext.put(MailPalette.CONTEXT_KEY, palette);
    }
    Map<String, Object> binding = new HashMap<>();
    binding.put("_ctx", new TemplateContextStub(included));
    binding.put("_templateContext", templateContext);
    binding.put("USER", "John");
    binding.put("AVATAR", "/avatar");
    binding.put("PROFILE_URL", "http://host/portal/profile/john");
    binding.put("MFA_ADMIN_PAGE_URL", "http://host/portal/administration");
    return new GroovyTemplate("MfaAdminRevocationRequestPlugin", "MfaAdminRevocationRequestPlugin", read()).render(binding);
  }

  /** Stands for the notification context: labels echo their key, includes are recorded. */
  public static class TemplateContextStub {

    private final List<String> included;

    public TemplateContextStub(List<String> included) {
      this.included = included;
    }

    public String appRes(String key, Object... args) {
      return "[" + key + "]";
    }

    public String escapeHTML(String text) {
      return text;
    }

    public void include(String path, Object templateContext) {
      included.add(path);
    }
  }
}
