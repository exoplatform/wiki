/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.webui;

import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormInputSet;
import org.exoplatform.wiki.commons.Utils;
import org.exoplatform.wiki.mow.api.WikiPreferences;

public class UIWikiSyntaxPreferences extends UIFormInputSet {

  public static final String FIELD_ALLOW = "AllowChooseOthers";

  public UIWikiSyntaxPreferences(String id) throws Exception {
    setId(id);

    UIFormCheckBoxInput<Boolean> allowSelect = new UIFormCheckBoxInput<Boolean>(FIELD_ALLOW, FIELD_ALLOW, null);
    addUIFormInput(allowSelect);

    updateData();
  }

  public void updateData() throws Exception {
    WikiPreferences currentPreferences = Utils.getCurrentPreferences();

    UIFormCheckBoxInput<Boolean> allowSelect = getUIFormCheckBoxInput(FIELD_ALLOW);
    boolean currentAllow = currentPreferences.getWikiPreferencesSyntax().isAllowMultipleSyntaxes();
    allowSelect.setChecked(currentAllow);
  }
}
