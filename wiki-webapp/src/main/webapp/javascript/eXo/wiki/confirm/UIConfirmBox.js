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
 */

if (!eXo.wiki)
  eXo.wiki = {};

function UIConfirmBox() {
};

UIConfirmBox.prototype.init = function() {
  eXo.wiki.UIConfirmBox.closeConfirm();
};

UIConfirmBox.prototype.render = function(uicomponentId, titleMessage, message,
    submitClass, submitLabel, cancelLabel) {
  
  var me = eXo.wiki.UIConfirmBox;
  var submitAction = document.getElementById(uicomponentId);
  me.confirmBox = gj('<div/>', {
    'class' : 'ConfirmBox',
    align : 'center'
  }).append(gj('<div/>', {
    'class' : 'ConfirmBar'
  }).append(gj('<div/>', {
    'class' : 'ConfirmTitle',
    text : titleMessage
  }), gj('<a/>', {
    'class' : 'CloseButton',
    href : 'javascript:eXo.wiki.UIConfirmBox.closeConfirm()'
  })));

  var container = gj('<div/>').append(gj('<div/>', {
    'class' : 'ConfirmMessage',
    text : message
  }))

  if (submitAction && submitLabel) {
    me.createInput(container, submitAction, submitLabel);
  }
  if (cancelLabel) {
    me.createInput(container, null, cancelLabel);
  }
  gj(submitAction).append(gj(me.confirmBox).append(container));
  this.maskLayer = eXo.core.UIMaskLayer.createMask("UIPortalApplication",
      me.confirmBox[0], 30);
  return false;
};

UIConfirmBox.prototype.createInput = function(container, action, message) {
 gj(container).append(gj('<input/>', {
   value: message,
   type: 'button',
   click: function(event) {
     if (action && action.href){
       window.location = action.href;
     }
     eXo.wiki.UIConfirmBox.closeConfirm();
    }
   }
  )
 )
};

UIConfirmBox.prototype.closeConfirm = function() {
  var me = eXo.wiki.UIConfirmBox;
  if (this.maskLayer) {
    eXo.core.UIMaskLayer.removeMask(this.maskLayer);
    this.maskLayer = null;
  }
  if (me.confirmBox) {
    gj(me.confirmBox).remove();
    me.confirmBox = null;
  }
};

UIConfirmBox.prototype.resetPosition = function() {
  var me = eXo.wiki.UIConfirmBox;
  var confirmbox = me.confirmBox;

  if (confirmbox && (gj(confirmbox).css('display') == "block")) {
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