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

 (function(base, uiForm, webuiExt, $) {

function UIWikiAjaxRequest() {
  this.DEFAULT_TIMEOUT_CHECK = 100;
  this.autoCheckAnchorId = false;
  this.actionPrefix = false;
  this.defaultAction = false;
  this.currentAnchor = null;
  this.isEnableCheck = true;
};

UIWikiAjaxRequest.prototype.init = function(actionPrefix, defaultAction) {
  this.actionPrefix = actionPrefix;
  this.defaultAction = defaultAction;
  this.isEnableCheck = true;
  if (this.actionPrefix && this.defaultAction) {
    this.autoCheckAnchorId = window.setInterval(this.autoCheckAnchor, this.DEFAULT_TIMEOUT_CHECK);
    $(window).on('unload', this.destroyAll);
  }
};

UIWikiAjaxRequest.prototype.autoCheckAnchor = function() {
  eXo.wiki.UIWikiAjaxRequest.checkAnchor();
};

UIWikiAjaxRequest.prototype.getCurrentHash = function() {
  var r = window.location.href;
  var i = r.indexOf("#");
  return (i >= 0 ? r.substr(i + 1) : false);
};

UIWikiAjaxRequest.prototype.urlHasActionParameters = function() {
  var r = window.location.href;
  var i = r.indexOf("?");
  if (i >= 0) {
    r = r.substr(i + 1);
    if (r && r.length > 0) {
      i = r.indexOf("action=");
      if (i >= 0) {
        return true;
      }
      i = r.indexOf("op=");
      return (i >= 0 ? true : false);
    }
  }
  return false;
};

UIWikiAjaxRequest.prototype.checkAnchor = function() {
  // Check if it has changes
  if (this.currentAnchor != this.getCurrentHash()) {
    this.currentAnchor = this.getCurrentHash();
    if (this.isEnableCheck == false){
      this.isEnableCheck = true;
      return;
    }
    var action = null;
    if (this.currentAnchor && this.currentAnchor.length > 0) {
      var splits = this.currentAnchor.split('&');
      // Get the action name
      action = splits[0];
      if(action && action.length > 0 && action.charAt(0) == 'H'){
        // This is an anchor in the document, so skip.
        return;
      }
      var queryParams = '&wikiMode=' + action;

      for ( var index = 1; index < splits.length; index++) {
        queryParams += '&';
        queryParams += splits[index];
      }
      action = document.getElementById(this.actionPrefix + action);
      if (action) {
        var ajaxGetLink = action.getAttributeNode('onclick').value.replace('&ajaxRequest=true', queryParams + '&ajaxRequest=true');
        action.onclick = function() {
          eval(ajaxGetLink);
        };
      }
    } else if (!this.urlHasActionParameters()) {
      action = document.getElementById(this.actionPrefix + this.defaultAction);
    }
    if (action) {
      action.onclick();
    }
  }
};

UIWikiAjaxRequest.prototype.onFrameLoaded = function(hash) {
  location.hash = hash;
};


UIWikiAjaxRequest.prototype.makeNewHash = function(hash) {
  this.onFrameLoaded(hash);
};

/**
 * Make hash and disable auto check
 */
UIWikiAjaxRequest.prototype.makeHash = function(hash) {
  this.isEnableCheck = false;
  eXo.wiki.UIWikiAjaxRequest.makeNewHash(hash);
};

/**
 * Stop auto check anchor by interval
 */
UIWikiAjaxRequest.prototype.stopAutoCheckAnchor = function() {
  if (this.autoCheckAnchorId) {
    window.clearInterval(this.autoCheckAnchorId);
    this.autoCheckAnchorId = false;
  }
};

UIWikiAjaxRequest.prototype.destroyAll = function() {
  eXo.wiki.UIWikiAjaxRequest.stopAutoCheckAnchor();
};

eXo.wiki.UIWikiAjaxRequest = new UIWikiAjaxRequest();
return eXo.wiki.UIWikiAjaxRequest;

})(base, uiForm, webuiExt, $);
