/*
 *
 *  * Copyright (C) 2003-2015 eXo Platform SAS.
 *  *
 *  * This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 */

package org.exoplatform.wiki.jpa;

import java.util.HashSet;
import java.util.Set;

import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * exo@exoplatform.com
 * 9/15/15
 */
public class SecurityUtils {

    public static void setCurrentUser(String userId, String... memberships) {
        Set<MembershipEntry> membershipEntrySet = new HashSet<MembershipEntry>();
        if (memberships!=null) {
            for (String membership : memberships) {
                String[] membershipSplit = membership.split(":");
                membershipEntrySet.add(new MembershipEntry(membershipSplit[1], membershipSplit[0]));
            }
        }
        ConversationState.setCurrent(new ConversationState(new Identity(userId, membershipEntrySet)));
    }
}
