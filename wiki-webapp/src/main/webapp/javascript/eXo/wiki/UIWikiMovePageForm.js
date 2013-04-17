/**
 * Copyright (C) 2013 eXo Platform SAS.
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

if (!eXo.wiki) {
  eXo.wiki = {};
};

function UIWikiMovePageForm() {
};

UIWikiMovePageForm.prototype.init = function(spaceSwitcherId) {
  var me = eXo.wiki.UIWikiMovePageForm;
  me.spaceSwitcherId = spaceSwitcherId;
  
  $(window).ready(function(){
    var me = eXo.wiki.UIWikiMovePageForm;
    me.initSizeForSpaceSwitcher();
  });
}

UIWikiMovePageForm.prototype.initSizeForSpaceSwitcher = function() {
  var me = eXo.wiki.UIWikiMovePageForm;
  var spaceSwitcher = document.getElementById(me.spaceSwitcherId);
  if (spaceSwitcher) {
    var spacePopup = $(spaceSwitcher).find("ul.spaceChooserPopup")[0];
    var dropDownButton = $(spaceSwitcher).find("div.spaceChooser")[0];
  	if (spacePopup && dropDownButton) {
  	  spacePopup.style.width = dropDownButton.offsetWidth + "px";
  	}
  }
}

eXo.wiki.UIWikiMovePageForm = new UIWikiMovePageForm();
return eXo.wiki.UIWikiMovePageForm;

})(base, uiForm, webuiExt, $);