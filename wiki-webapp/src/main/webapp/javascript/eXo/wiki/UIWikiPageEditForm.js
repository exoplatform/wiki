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

if (!eXo.wiki)
  eXo.wiki = {};

function UIWikiPageEditForm() {
};

UIWikiPageEditForm.prototype.init = function(pageEditFormId, restURL, isRunAutoSave, pageRevision, isDraftForNewPage, autoSaveSequeneTime, untitledLabel) {
	
  var me = eXo.wiki.UIWikiPageEditForm;
  me.pageEditFormId = pageEditFormId || me.pageEditFormId;
  me.changed = false;
  me.firstChanged = false;
  me.defaultTitle = untitledLabel || me.defaultTitle;
  me.restURL = restURL || me.restURL;
  me.isRunAutoSave = isRunAutoSave || me.isRunAutoSave;
  me.pageRevision = pageRevision || me.pageRevision;
  me.isDraftForNewPage = isDraftForNewPage || me.isDraftForNewPage;
  me.autoSaveSequeneTime = autoSaveSequeneTime || me.autoSaveSequeneTime;
  me.pageSaved = false;
	
  var pageEditForm = document.getElementById(me.pageEditFormId);
  if (!pageEditForm) {
    return;
  }
  

  // Declare the function to handle change to create draft
  var func = function() {
    var me = eXo.wiki.UIWikiPageEditForm;
    me.firstChanged = true;
    if (me.changed == false) {
      setTimeout("eXo.wiki.UIWikiPageEditForm.saveDraft()", me.autoSaveSequeneTime);
      me.changed = true;
    }
  }
  
  // Find title input
  var titleContainer = $(pageEditForm).find('div.uiWikiPageTitle')[0];
  var titleInput = $(titleContainer).find('input')[0];
  
  // Find textarea
  var textAreaContainer = $(pageEditForm).find('div.uiWikiPageContentInputContainer')[0];
  if (!textAreaContainer) {
    textAreaContainer = $(pageEditForm).find('div.UIWikiRichTextEditor')[0];
  } else {
    setTimeout(function () {
      $(textAreaContainer).find('textarea')[0].focus();
    }, 100);
  }
  
  // Bind event for text area and title input
  $(titleInput).keyup(func);
  $(titleInput).change(func);
  if (textAreaContainer) {
    var textarea = $(textAreaContainer).find('textarea');
    textarea.keyup(func);
    textarea.change(func);
    
    textarea = $(textAreaContainer).find('iframe')[0];
    if (textarea) {
    	$(textarea.contentWindow.document).bind('keyup', func);
    }
  }
};

UIWikiPageEditForm.prototype.checkToRemoveEditorMenu = function() {
  var me = eXo.wiki.UIWikiPageEditForm;
  var pageEditForm = document.getElementById(me.pageEditFormId);
  
  // Try to delete Import menu
  var menuItems = $(pageEditForm).find("div.gwt-MenuItemLabel");
  if (menuItems) {
    for (var i = 0; i < menuItems.length; i++) {
      if (menuItems[i].innerHTML == "Import") {
      	var parent = menuItems[i].parentNode;
      	parent.parentNode.removeChild(parent);
      	break;
      }
    }
  }
  
  // Try to delete "My recent changes" when add link to wiki page
  var portalApp = document.getElementById("UIPortalApplication");
  if (portalApp) {
    var bodyTag = portalApp.parentNode;
    var tabLabels = $(bodyTag).find("div.gwt-Label");
    if (tabLabels) {
      for (var i = 0; i < tabLabels.length; i++) {
        if (tabLabels[i].innerHTML == "My recent changes") {
      	  var parent = tabLabels[i].parentNode.parentNode;
      	  parent.parentNode.removeChild(parent);
      	  break;
        }
      }
    }
  }
  
  // set timeout to run sequently
  setTimeout(me.checkToRemoveEditorMenu, 200);
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

UIWikiPageEditForm.prototype.setRestParam = function(wikiType, wikiOwner, pageId) {
  var me = eXo.wiki.UIWikiPageEditForm;
  me.wikiType = wikiType;
  me.wikiOwner = wikiOwner;
  me.pageId = pageId;
  me.createRestParam();
};

UIWikiPageEditForm.prototype.setDraftName = function(draftName) {
  var me = eXo.wiki.UIWikiPageEditForm;
  me.draftName = draftName;
};


UIWikiPageEditForm.prototype.decorateInputOfTemplate = function(defaultTitleOfTemplate, defaultDescriptionOfTemplate) {
  var me = eXo.wiki.UIWikiPageEditForm;
  var pageEditForm = document.getElementById(me.pageEditFormId);
  var titleContainer = $(pageEditForm).find('div.uiWikiPageTitle')[0];
  var titleInput = $(titleContainer).find('input')[0];
  eXo.wiki.UIWikiPortlet.decorateInput(titleInput, defaultTitleOfTemplate, true);
  
  var descriptionContainer = $(pageEditForm).find('div.uiWikiTemplateDescriptionContainer')[0];
  var descriptionInput = $(descriptionContainer).find('input')[0];
  eXo.wiki.UIWikiPortlet.decorateInput(descriptionInput, defaultDescriptionOfTemplate, true);
};

UIWikiPageEditForm.prototype.createRestParam = function() {
  var me = eXo.wiki.UIWikiPageEditForm;
  var clientTime = new Date().getTime();
  
  me.restParam = "?wikiType=" + me.wikiType + "&wikiOwner=" + me.wikiOwner + "&pageId=" + me.pageId 
    + "&lastDraftName=" + me.draftName + "&pageRevision=" + me.pageRevision + "&isNewPage=" + me.isDraftForNewPage
    + "&clientTime=" + clientTime;
};

// sets status of page as saved to avoid running autosave after click 'save' button but before page is reloaded
UIWikiPageEditForm.prototype.setPageSaved = function() {
  var me = eXo.wiki.UIWikiPageEditForm;
  me.pageSaved = true;
}
 
UIWikiPageEditForm.prototype.saveDraft = function() {
  var me = eXo.wiki.UIWikiPageEditForm;
  if (!me.isRunAutoSave || me.pageSaved) {
    return;
  }
  
  // Get page title
  var pageEditForm = document.getElementById(me.pageEditFormId);
  var titleContainer = $(pageEditForm).find('div.uiWikiPageTitle')[0];
  var pageTitle = $(titleContainer).find('input')[0].value;
  if (pageTitle == me.defaultTitle) {
    pageTitle = '';
  }
  
  // Get page content
  var pageContent = "";
  var isMarkup = true;
  var textAreaContainer = $(pageEditForm).find('div.uiWikiPageContentInputContainer')[0];
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
    async : true,
    url : me.restURL + me.restParam,
    contentType: "application/x-www-form-urlencoded;",
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
  var me = eXo.wiki.UIWikiPageEditForm;
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
    var messageArea = $(pageEditForm).find('div.uiWikiPageEditForm_MessageArea')[0];
    messageArea.style.margin = "13px 0px 0px 10px";
    messageArea.innerHTML = me.saveDraftSuccessMessage + " " + timeSavedDraft;
  }
};

UIWikiPageEditForm.prototype.doCancelAction = function()  {
  var me = eXo.wiki.UIWikiPageEditForm;
  var action = document.getElementById("link_" + me.callBackComponentId);
  var href = action.getAttribute("href");
  if (action && href) {
    eval(href);
  }
};

UIWikiPageEditForm.prototype.onNotKeepDraftFunction = function()  {
  var me = eXo.wiki.UIWikiPageEditForm;
  
  // Call rest request to remove draft
  $.ajax({
    async : true,
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

UIWikiPageEditForm.prototype.cancelSave = function(uicomponentId, titleMessage, message, submitClass, submitLabel, cancelLabel) {
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

UIWikiPageEditForm.prototype.fixWikiNotificationTimeZone = function(){
  var uiWikiNotificationContainer = $(".uiWikiNotificationContainer")[0];
  if (uiWikiNotificationContainer) {
    innerHTML = uiWikiNotificationContainer.innerHTML;
    var i1 = innerHTML.indexOf("{");
    var i2 = innerHTML.indexOf("}");
    if (i1 && i2) {
      var timeLong = innerHTML.substring(i1+1, i2);
      if (timeLong) {
        var oldSt = "{" + timeLong +  "}";
        var date = new Date(parseInt(timeLong));
        timeLong = date.toLocaleDateString() + " " + date.toLocaleTimeString();
        innerHTML = innerHTML.replace(oldSt, timeLong);
        $(uiWikiNotificationContainer).html(innerHTML);
      }
    }
  }
};

eXo.wiki.UIWikiPageEditForm = new UIWikiPageEditForm();
return eXo.wiki.UIWikiPageEditForm;

})(base, uiForm, webuiExt, $);
