/**
 * Copyright (C) 2012 eXo Platform SAS.
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
function UIWikiSpaceSwitcher() {
};

UIWikiSpaceSwitcher.prototype.init = function(uicomponentId, mySpaceRestUrl, defaultValueForTextSearch, selectSpaceAction) {
  var me = eXo.wiki.UIWikiSpaceSwitcher;
  me.mySpaceRestUrl = mySpaceRestUrl;
  me.lastSearchKeyword = "";
  me.defaultValueForTextSearch = defaultValueForTextSearch;
  me.selectSpaceAction = selectSpaceAction;
  
  var me = eXo.wiki.UIWikiSpaceSwitcher;
  var wikiSpaceSwitcher = document.getElementById(uicomponentId);
  var textField = $(wikiSpaceSwitcher).find("input.SpaceSearchText")[0];
  textField.value = defaultValueForTextSearch;
  
  textField.onkeydown = function() {
    me.onTextSearchChange(uicomponentId);
  };
  
  textField.onkeypress = function() {
    me.onTextSearchChange(uicomponentId);
  };
  
  textField.onkeyup = function() {
    me.onTextSearchChange(uicomponentId);
  };
  
  textField.onfocus = function() {
    if (textField.value == me.defaultValueForTextSearch) {
      textField.value = "";
    }
    textField.className="SpaceSearchText Focus"
  };
  
  textField.onclick = function() {
    var event = event || window.event;
    event.cancelBubble = true;
  };
  
  // When textField lost focus
  textField.onblur = function() {
    if (textField.value == "") {
      textField.value = me.defaultValueForTextSearch;
      textField.className="SpaceSearchText LostFocus"
    }
  };
  
  // Hide popup when user click outside
  document.onclick = function() {
    var wikiSpaceSwitcher = document.getElementById(uicomponentId);
    var spaceChooserPopups = document.getElementsByClassName('SpaceChooserPopup');
    
    // var spaceChooserPopup = $(wikiSpaceSwitcher).find("div.SpaceChooserPopup")[0];
    for (var i = 0; i < spaceChooserPopups.length; i++) {
      spaceChooserPopups[i].style.display = "none";
    }
  };
};

UIWikiSpaceSwitcher.prototype.requestData = function(keyword, uicomponentId) {
  var me = eXo.wiki.UIWikiSpaceSwitcher;
  $.ajax({
    async : false,
    url : me.mySpaceRestUrl + "?keyword=" + keyword,
    type : 'GET',
    data : '',
    success : function(data) {
      me.render(data, uicomponentId);
    }
  });
};

UIWikiSpaceSwitcher.prototype.render = function(dataList, uicomponentId) {
  var me = eXo.wiki.UIWikiSpaceSwitcher;
  me.dataList = dataList;
  
  var wikiSpaceSwitcher = document.getElementById(uicomponentId);
  var spaceChooserPopup = $(wikiSpaceSwitcher).find('div.SpaceList')[0];
  var spaces = dataList.jsonList;
  var groupSpaces = '';

  for (i = 0; i < spaces.length; i++) {
    var spaceId = spaces[i].spaceId;
    var spaceUrl = spaces[i].spaceUrl;
    var name = spaces[i].name;
    
    var spaceDiv = "<div class='SpaceOption' id='UIWikiSpaceSwitcher_" + spaceId 
      + "' title='" + name 
      + "' alt='" + name 
      + "' onclick=\"eXo.wiki.UIWikiSpaceSwitcher.onChooseSpace('" + spaceId + "')\">" 
      + name + "</div>";
    groupSpaces += spaceDiv;
  }
  spaceChooserPopup.innerHTML = groupSpaces;
};

UIWikiSpaceSwitcher.prototype.onChooseSpace = function(spaceId) {
  var me = eXo.wiki.UIWikiSpaceSwitcher;
  var url = decodeURIComponent(me.selectSpaceAction);
  url = url.substr(0, url.length - 2) + "&spaceId=" + spaceId + "')";
  eval(url);
}

UIWikiSpaceSwitcher.prototype.openComboBox = function(event, spaceChooserDiv) {
  var event = event || window.event;
  event.cancelBubble = true;
  var me = eXo.wiki.UIWikiSpaceSwitcher;
  var wikiSpaceSwitcher = spaceChooserDiv.parentNode;
  var spaceChooserPopup = $(wikiSpaceSwitcher).find("div.SpaceChooserPopup")[0];

  if (spaceChooserPopup.style.display == "none") {
    spaceChooserPopup.style.display = "block";
  } else {
    spaceChooserPopup.style.display = "none";
  }
};

UIWikiSpaceSwitcher.prototype.onTextSearchChange = function(uicomponentId) {
  var me = eXo.wiki.UIWikiSpaceSwitcher;
  var wikiSpaceSwitcher = document.getElementById(uicomponentId);
  var textSearch = $(wikiSpaceSwitcher).find("input.SpaceSearchText")[0].value;
  
  if (textSearch != me.lastSearchKeyword) {
    me.lastSearchKeyword = textSearch;
    me.requestData(textSearch, uicomponentId);
  }
};

eXo.wiki.UIWikiSpaceSwitcher = new UIWikiSpaceSwitcher();
_module.UIWikiSpaceSwitcher = eXo.wiki.UIWikiSpaceSwitcher;