/**
 * Copyright (C) 2010 eXo Platform SAS.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */(function(base, uiForm, webuiExt, $) {

function UIWikiSavePage() {
  this.disableClass = ["SavePage", "MinorEdit", "SaveTemplate"];
  this.actionToSynchronizeSave = ["SavePage", "MinorEdit", "SaveTemplate"];
};

UIWikiSavePage.prototype.confirm = function(uicomponentId, isNewMode, pageTitleInputId, untitled, titleMessage, addMessage, submitClass, submitLabel, cancelLabel, titleWarning, warningMsg, okLabel) {
  var pageTitleInput = document.getElementById(pageTitleInputId);
  if (isNewMode == true && (pageTitleInput.value == untitled || pageTitleInput.value == "")) {
    eXo.wiki.UIConfirmBox.render(uicomponentId, titleMessage, addMessage, submitClass, submitLabel, cancelLabel);
    return false;
  }
  return true;
};

UIWikiSavePage.prototype.disableButton = function(parent, action) {
  var isNeedToSynchronize = false;
  for (i = 0; i < this.actionToSynchronizeSave.length; i++) {
    if (action == this.actionToSynchronizeSave[i]) {
      isNeedToSynchronize = true;
      break;
    }
  }
  
  if (isNeedToSynchronize == false) {
    return;
  }
  for (i = 0; i < this.disableClass.length; i++) {
    var buttons = $(parent).find('a.'+this.disableClass[i]);
    for (k = 0; k < buttons.length; k++) {
      $(buttons[k]).addClass("DisableButton");
      $(buttons[k]).attr('href','javascript:void(0);');
      $(buttons[k]).click("return false;");
    }
  }
}

eXo.wiki.UIWikiSavePage = new UIWikiSavePage();
return eXo.wiki.UIWikiSavePage;

})(base, uiForm, webuiExt, $);
