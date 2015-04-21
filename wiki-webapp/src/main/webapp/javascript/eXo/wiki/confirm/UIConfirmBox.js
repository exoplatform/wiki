/**
 * Copyright (C) 2010 eXo Platform SAS.
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */(function(base, uiForm, webuiExt, $) {

if (!eXo.wiki)
  eXo.wiki = {};

function UIConfirmBox() {
};

UIConfirmBox.prototype.init = function() {
  eXo.wiki.UIConfirmBox.closeConfirm();
};

UIConfirmBox.prototype.render = function(uicomponentId, titleMessage, message, submitClass, submitLabel, cancelLabel) {
   var me = eXo.wiki.UIConfirmBox;
  me.uicomponentId = uicomponentId;
  var buttonLabelArray = [submitLabel, cancelLabel];
  var callBackFunctionArray = [me.doAction, null];
  me.renderConfirmBox(uicomponentId, titleMessage, message, buttonLabelArray, callBackFunctionArray);
  return false;
};
 
UIConfirmBox.prototype.doAction = function()  {
  var me = eXo.wiki.UIConfirmBox;
  var action = document.getElementById("link_" + me.uicomponentId);
  var href = action.getAttribute("href");
  if (action && href) {
    eval(href);
  }
};
 
UIConfirmBox.prototype.renderConfirmBox = function(componentId, titleMessage, message, buttonLabelArray, callBackFunctionArray) {
    var me = eXo.wiki.UIConfirmBox;
    
    // Build the confirm box
    me.confirmBox = $('<div/>', {'class':'uiPopup UIDragObject NormalStyle', 'width':'460','height':'215'});
    me.confirmBox.append($(""
      + "<div class='popupHeader clearfix'>"
	  +   "<a href='javascript:eXo.wiki.UIConfirmBox.closeConfirm()' class='uiIconClose pull-right'></a>"
	  +   "<span class='PopupTitle popupTitle'>" + titleMessage + "</span>"
	  + "</div>"
	  + "<div class='PopupContent popupContent'>"
	  +   "<div class='confirmMessage'><i class='uiIconQuestion uiIconBlue'></i>" + message + "</div>"
	  +   "<div class='uiAction uiActionBorder'></div>"
	  + "</div>"));
	  
	// Create buttons
    var actionContainer = $(me.confirmBox).find('div.uiAction')[0];
    for (i = 0; i < buttonLabelArray.length; i++) {
      if (buttonLabelArray[i]) {
        me.createInput(actionContainer, callBackFunctionArray[i], buttonLabelArray[i]);
      }
    }
	  
	// Append confirm box to UI
    var component = document.getElementById(componentId);
    $(component).append(me.confirmBox);
    
    // Create maskLayer
    this.maskLayer = eXo.core.UIMaskLayer.createMask("UIPortalApplication", me.confirmBox[0], 30);
    return false;
};

UIConfirmBox.prototype.renderWarningBox = function(componentId, titleMessage, message, buttonLabel) {
    var me = eXo.wiki.UIConfirmBox;
    var INVALID_CHARACTERS  = "% = : @ / \\ | ^ # ; [ ] { } < > * ' \" + ? &";
    message = message.replace("{0}", INVALID_CHARACTERS);
    
    // Build the box
    me.confirmBox = $('<div/>', {'class':'UIPopupWindow UIDragObject uiPopup', 'width':'460','height':'185'});
    me.confirmBox.append($(""
	  + "<div class='popupHeader clearfix'>"
	  +   "<a href='javascript:eXo.wiki.UIConfirmBox.closeConfirm()' class='uiIconClose pull-right'></a>"
	  +   "<span class='PopupTitle popupTitle'>" + titleMessage + "</span>"
	  + "</div>"
	  + "<div class='PopupContent popupContent'>"
	  +   "<ul class='singleMessage popupMessage resizable'><li><span class='warningIcon'>" + message + "</span></li></ul>"
	  +   "<div class='uiAction uiActionBorder'>"
	  +     "<a class='btn' href='javascript:eXo.wiki.UIConfirmBox.closeConfirm()'>" + buttonLabel + "</a>"
	  +   "</div>"
	  + "</div>"));
	  
	// Append confirm box to UI
    var component = document.getElementById(componentId);
    $(component).append(me.confirmBox);
    
    // Create maskLayer
    this.maskLayer = eXo.core.UIMaskLayer.createMask("UIPortalApplication", me.confirmBox[0], 30);
    return false;
};

UIConfirmBox.prototype.validate = function(pageTitleInputId) {
  var pageTitleInput = document.getElementById(pageTitleInputId);
  var invalidChars = [];
  var valid = true;
  for (var i=0; i<invalidChars.length; i++) {
    var current = invalidChars[i];
    if (pageTitleInput.value.indexOf(current) != -1) {
      valid = false;
      break;
    }
  }
  return valid;
};

UIConfirmBox.prototype.createInput = function(container, callBackFunction, message) {
  var button = $('<button/>', {
    'class': 'btn',
    click: function(event) {
      if (callBackFunction) {
        callBackFunction();
      }
      eXo.wiki.UIConfirmBox.closeConfirm();
    }
  });
  button.append(message);
  $(container).append(button);
};

UIConfirmBox.prototype.closeConfirm = function() {
  var me = eXo.wiki.UIConfirmBox;
  if (this.maskLayer) {
    eXo.core.UIMaskLayer.removeMask(this.maskLayer);
    this.maskLayer = null;
  }
  if (me.confirmBox) {
    $(me.confirmBox).remove();
    me.confirmBox = null;
  }
};

UIConfirmBox.prototype.resetPosition = function() {
  var me = eXo.wiki.UIConfirmBox;
  var confirmbox = me.confirmBox;

  if (confirmbox && ($(confirmbox).css('display') == "block")) {
    try {
      eXo.core.UIMaskLayer.blockContainer = document
          .getElementById("UIPortalApplication");
      eXo.core.UIMaskLayer.object = confirmbox;
      eXo.core.UIMaskLayer.setPosition();
    } catch (e) {
    }
  }
};

eXo.wiki.UIConfirmBox = new UIConfirmBox();
return eXo.wiki.UIConfirmBox;

})(base, uiForm, webuiExt, $);
