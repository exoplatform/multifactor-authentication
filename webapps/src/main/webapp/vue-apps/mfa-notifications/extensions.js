/*
 * Copyright (C) 2023 eXo Platform SAS
 *
 *  This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <gnu.org/licenses>.
 */

extensionRegistry.registerExtension('WebNotification', 'notification-group-extension', {
  rank: 100,
  name: 'security',
  plugins: [
    'MfaAdminRevocationRequestPlugin',
  ],
  icon: 'fa-shield-alt',
});
extensionRegistry.registerExtension('WebNotification', 'notification-content-extension', {
  type: 'MfaAdminRevocationRequestPlugin',
  rank: 10,
  vueComponent: Vue.options.components['user-notification-mfa-revocation'],
});
