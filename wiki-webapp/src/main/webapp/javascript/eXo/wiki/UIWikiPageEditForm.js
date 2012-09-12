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
 */


if (!eXo.wiki)
  eXo.wiki = {};

function UIWikiPageEditForm() {
};

UIWikiPageEditForm.prototype.editPageContent = function(pageEditFormId) {
  var pageEditForm = document.getElementById(pageEditFormId);
  var titleContainer = gj(pageEditForm).find('div.UIWikiPageTitleControlForm_PageEditForm')[0];
  var titleInput = gj(titleContainer).find('input')[0];
  eXo.wiki.UIWikiPageEditForm.changed = false;

  gj(titleInput).change(function() {
    eXo.wiki.UIWikiPageEditForm.changed = true;
    gj(titleInput).change(null);
  });

  var textAreaContainer = gj(pageEditForm).find('div.UIWikiPageContentInputContainer')[0];
  if (textAreaContainer != null) {
    var textArea = gj(textAreaContainer).find('textarea')[0];
    gj(textArea).change(function() {
      eXo.wiki.UIWikiPageEditForm.changed = true;
      gj(textArea).change(null);
    });
  } else {
    eXo.wiki.UIWikiPageEditForm.changed = true;
  }
};

UIWikiPageEditForm.prototype.cancel = function(uicomponentId, titleMessage, message, submitClass, submitLabel, cancelLabel){
  if (eXo.wiki.UIWikiPageEditForm.changed == true) {
    eXo.wiki.UIConfirmBox.render(uicomponentId, titleMessage, message, submitClass, submitLabel, cancelLabel);
    return false;
  }
  return true;
};

UIWikiPageEditForm.prototype.synPublishActivityCheckboxesStatus = function(checkBoxName1, checkBoxName2) {
  var checkBox1 = document.getElementsByName(checkBoxName1)[0];
  var checkBox2 = document.getElementsByName(checkBoxName2)[0];
  
  gj(checkBox1).click(function() {
    gj(checkBox2).attr("checked", !gj(checkBox2).attr("checked"));
  });
  
  gj(checkBox2).click(function() {
    gj(checkBox1).attr("checked", !gj(checkBox1).attr("checked"));
  });
}

eXo.wiki.UIWikiPageEditForm = new UIWikiPageEditForm();