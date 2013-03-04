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


function UIWikiTemplateForm() {
};

UIWikiTemplateForm.prototype.init = function(componentid, inputId, defaultText) {
  var me = eXo.wiki.UIWikiTemplateForm;
  me.component = document.getElementById(componentid);
  if (!me.component) {
    return;
  }
  var input = $(me.component).find('#' + inputId)[0];
  $(input).attr('autocomplete', 'off');
  $(input).keyup(function(evt) {
    evt = window.event || evt;
    eXo.wiki.UIWikiTemplateForm.pressHandler(evt, this);
  });
  eXo.wiki.UIWikiPortlet.decorateInput(input, defaultText, true);
  input.form.onsubmit = function() {
    return false;
  }
};

UIWikiTemplateForm.prototype.pressHandler = function(evt, textbox) {
  var me = eXo.wiki.UIWikiTemplateForm;
  evt = window.event || evt;
  var keyNum = eXo.wiki.UIWikiPortlet.getKeynum(evt);
  if (evt.altKey || evt.ctrlKey || evt.shiftKey)
    return;
  switch (keyNum) {
  case 13:
    me.enterHandler(evt);
    break;
  case 27:
    me.escapeHandler();
    break;
  case 38:
    me.arrowUpHandler();
    break;
  case 40:
    me.arrowDownHandler();
    break;
  default:
    break;
  }
  return;
};

/**
 * Handler key press
 */

UIWikiTemplateForm.prototype.enterHandler = function(evt){
    var me = eXo.wiki.UIWikiTemplateForm;
    me.doAdvanceSearch();
};

UIWikiTemplateForm.prototype.escapeHandler = function() {
};

UIWikiTemplateForm.prototype.arrowUpHandler = function() {
};

UIWikiTemplateForm.prototype.arrowDownHandler = function() {
};

UIWikiTemplateForm.prototype.typeHandler = function(textbox) {  
};

UIWikiTemplateForm.prototype.doAdvanceSearch = function() {
  var me = eXo.wiki.UIWikiTemplateForm;
  if (me.component) {
    var action = $(me.component).find('button.TemplateSearchButton')[0];
    action.onclick();
  }
}

eXo.wiki.UIWikiTemplateForm = new UIWikiTemplateForm();
return eXo.wiki.UIWikiTemplateForm;

})(base, uiForm, webuiExt, $);
