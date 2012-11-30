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

UIWikiPageEditForm.prototype.init = function(pageEditFormId, restURL, isRunAutoSave, pageRevision, isDraftForNewPage, autoSaveSequeneTime, untitledLabel) {
  var pageEditForm = document.getElementById(pageEditFormId);
  var titleContainer = $(pageEditForm).find('div.UIWikiPageTitleControlForm_PageEditForm')[0];
  var titleInput = $(titleContainer).find('input')[0];

  var me = eXo.wiki.UIWikiPageEditForm;
  me.pageEditFormId = pageEditFormId;
  me.changed = false;
  me.firstChanged = false;
  me.defaultTitle = untitledLabel;

  $(titleInput).change(function() {
    me.firstChanged = true;
    if (me.changed == false) {
      setTimeout("eXo.wiki.UIWikiPageEditForm.saveDraft()", me.autoSaveSequeneTime);
      me.changed = true;
    }
  });

  var func = function() {
    var me = eXo.wiki.UIWikiPageEditForm;
    me.firstChanged = true;
    if (me.changed == false) {
      setTimeout("eXo.wiki.UIWikiPageEditForm.saveDraft()", me.autoSaveSequeneTime);
      me.changed = true;
    }
  }

  // Get page content
  var textAreaContainer = $(pageEditForm).find('div.UIWikiPageContentInputContainer')[0];
  if (textAreaContainer != null) {
    pageContent = $(textAreaContainer).find('textarea')[0].onkeyup = func;
  } else {
    var textAreaContainer = $(pageEditForm).find('div.UIWikiRichTextEditor')[0];
    if (textAreaContainer) {
      $(textAreaContainer).find('textarea')[0].onkeyup = func;
    }
  }

  me.restURL = restURL;
  me.isRunAutoSave = isRunAutoSave;
  me.pageRevision = pageRevision;
  me.isDraftForNewPage = isDraftForNewPage;
  me.autoSaveSequeneTime = autoSaveSequeneTime;
};

UIWikiPageEditForm.prototype.setMessageResource = function(saveDraftSuccessMessage, discardDraftConfirmMessage) {
  var me = eXo.wiki.UIWikiPageEditForm;
  me.saveDraftSuccessMessage = saveDraftSuccessMessage;
  me.discardDraftConfirmMessage = discardDraftConfirmMessage;
};

UIWikiPageEditForm.prototype.setCancelDraftAction = function(removeDraftRestUrl, cancelDraftConfirmTitle, cancelDraftConfirmMessage, yesLabel, noLabel) {
  var me = eXo.wiki.UIWikiPageEditForm;
  me.removeDraftRestUrl = removeDraftRestUrl;
  me.cancelDraftConfirmTitle = cancelDraftConfirmTitle;
  me.cancelDraftConfirmMessage = cancelDraftConfirmMessage;
  me.yesLabel = yesLabel;
  me.noLabel = noLabel;
};

UIWikiPageEditForm.prototype.setRestParam = function(wikiType, wikiOwner, pageId, draftName) {
  var me = eXo.wiki.UIWikiPageEditForm;
  me.wikiType = wikiType;
  me.wikiOwner = wikiOwner;
  me.pageId = pageId;
  me.draftName = draftName;
  me.createRestParam();
};

UIWikiPageEditForm.prototype.createRestParam = function() {
  var me = eXo.wiki.UIWikiPageEditForm;
  var clientTime = new Date().getTime();
  
  me.restParam = "?wikiType=" + me.wikiType + "&wikiOwner=" + me.wikiOwner + "&pageId=" + me.pageId 
    + "&lastDraftName=" + me.draftName + "&pageRevision=" + me.pageRevision + "&isNewPage=" + me.isDraftForNewPage
    + "&clientTime=" + clientTime;
};
 
UIWikiPageEditForm.prototype.saveDraft = function() {
  var me = eXo.wiki.UIWikiPageEditForm;
  if (!me.isRunAutoSave) {
    return;
  }
  
  // Get page title
  var pageEditForm = document.getElementById(me.pageEditFormId);
  var titleContainer = $(pageEditForm).find('div.UIWikiPageTitleControlForm_PageEditForm')[0];
  var pageTitle = $(titleContainer).find('input')[0].value;
  if (pageTitle == me.defaultTitle) {
    pageTitle = '';
  }
  
  // Get page content
  var pageContent = "";
  var isMarkup = true;
  var textAreaContainer = $(pageEditForm).find('div.UIWikiPageContentInputContainer')[0];
  if (textAreaContainer != null) {
    isMarkup = true;
    pageContent = $(textAreaContainer).find('textarea')[0].value;
  } else {
    isMarkup = false;
    var textAreaContainer = $(pageEditForm).find('div.UIWikiRichTextEditor')[0];
    if (textAreaContainer) {
      pageContent = $(textAreaContainer).find('textarea')[0].value;
    }
  }
  
  // Create rest request
  if (me.restParam) {
    var dataString = {'title': pageTitle, 'content': pageContent, 'isMarkup': isMarkup}
    $.ajax({
    async : false,
    url : me.restURL + me.restParam,
    contentType: "application/x-www-form-urlencoded;charset=ISO-8859-15",
    type : 'POST',
    data : dataString,
    dataType: 'json',
    success : function(data) {
      me.onSaveDraftSuccess(data);
    }
    });
  }
  me.changed = false;
};

UIWikiPageEditForm.prototype.onSaveDraftSuccess = function(data) {
  var me = eXo.wiki.;
  if (data.draftName) {
    me.draftName = data.draftName;
    me.createRestParam();
    
    // Show save draft success message
    var now = new Date();
    
    var hours = "" + now.getHours();
    if (hours.length < 2) {
      hours = "0" + hours;
    }
    
    var minutes = "" + now.getMinutes();
    if (minutes.length < 2) {
      minutes = "0" + minutes;
    }
    
    var timeSavedDraft = hours + ":" + minutes;
    var pageEditForm = document.getElementById(me.pageEditFormId);
    var messageArea = $(pageEditForm).find('div.UIWikiPageEditForm_MessageArea')[0];
    messageArea.style.margin = "25px 0px 0px 10px";
    messageArea.innerHTML = me.saveDraftSuccessMessage + " " + timeSavedDraft;
  }
};

UIWikiPageEditForm.prototype.doCancelAction = function()  {
  var me = eXo.wiki.UIWikiPageEditForm;
  var action = document.getElementById(me.callBackComponentId);
  if (action && action.href) {
    window.location = action.href;
  }
};

UIWikiPageEditForm.prototype.onNotKeepDraftFunction = function()  {
  var me = eXo.wiki.UIWikiPageEditForm;
  
  // Call rest request to remove draft
  $.ajax({
    async : false,
    url : me.removeDraftRestUrl + "?draftName=" + me.draftName,
    type : 'GET'
  });
  
  // Back to view mode
  me.doCancelAction();
};

UIWikiPageEditForm.prototype.cancel = function(uicomponentId, titleMessage, message, submitClass, submitLabel, cancelLabel) {
  var me = eXo.wiki.UIWikiPageEditForm;
  me.callBackComponentId = uicomponentId;
  
  // if draft exist then show confirm that user want to keep draft or not
  if ((me.draftName != null) && (me.draftName != '')) {
    // Show the confirm box
    var buttonLabelArray = [me.yesLabel, me.noLabel];
    var callBackFunctionArray = [me.doCancelAction, me.onNotKeepDraftFunction];
    eXo.wiki.UIConfirmBox.renderConfirmBox(uicomponentId, me.cancelDraftConfirmTitle, me.cancelDraftConfirmMessage, buttonLabelArray, callBackFunctionArray);
    return false;
  }
  
  // If there no draft then confirm that user have change, user want to cancel or not
  if (me.firstChanged == true) {
    eXo.wiki.UIConfirmBox.render(uicomponentId, titleMessage, message, submitClass, submitLabel, cancelLabel);
    return false;
  }
  return true;
};

UIWikiPageEditForm.prototype.synPublishActivityCheckboxesStatus = function(checkBoxName1, checkBoxName2) {
  var checkBox1 = document.getElementsByName(checkBoxName1)[0];
  var checkBox2 = document.getElementsByName(checkBoxName2)[0];
  
  $(checkBox1).click(function() {
    $(checkBox2).attr("checked", !$(checkBox2).attr("checked"));
  });
  
  $(checkBox2).click(function() {
    $(checkBox1).attr("checked", !$(checkBox1).attr("checked"));
  });
};

eXo.wiki.UIWikiPageEditForm = new UIWikiPageEditForm();
_module.UIWikiPageEditForm = eXo.wiki.UIWikiPageEditForm;
