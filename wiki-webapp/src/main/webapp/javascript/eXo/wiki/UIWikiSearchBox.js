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

function UIWikiSearchBox() {
  this.restURL = null;
  this.input = null;
  this.searchPopup = null;
  this.searchType = "";
  this.menu = null;
  this.wikiNodeURI = null;
  UIWikiSearchBox.searchMsg = "Search for";
};

UIWikiSearchBox.prototype.init = function(componentId, searchInputName, searchLabel, wikiNodeURI) {
  this.wikiNodeURI = wikiNodeURI;
  var uiComponent = document.getElementById(componentId);
  if (!uiComponent) {
    return;
  }
  
  var restInput = uiComponent["restURL"];
  this.input = uiComponent[searchInputName];
  $(this.input).attr('autocomplete', 'off');
  $(this.input).attr('title', searchLabel);
  $(this.input).val(searchLabel);  
  this.restURL = restInput.value;
  this.input.onkeyup = function(evt) {
    evt = window.event || evt;
    eXo.wiki.UIWikiSearchBox.pressHandler(evt, this);
  }
  this.input.form.onsubmit = function() {
    return false;
  }  
  eXo.wiki.UIWikiPortlet.decorateInput(this.input, searchLabel, true);
};

UIWikiSearchBox.prototype.pressHandler = function(evt, textbox) {
  var me = eXo.wiki.UIWikiSearchBox;
  evt = window.event || evt;
  var keyNum = eXo.wiki.UIWikiPortlet.getKeynum(evt);
  if (evt.altKey || evt.ctrlKey || evt.shiftKey)
    return;
  switch (keyNum) {
  case 13:
    if (textbox.value.trim() != "")
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
    if (me.typeTimeout)
      clearTimeout(me.typeTimeout);
    if (me.currentItem)
      me.currentItem = null;
    me.typeTimeout = setTimeout(function() {
      if (me.xhr) {
        me.xhr.abort();
        me.xhr = null;
        delete me.xhr;
      }
      me.typeHandler(textbox);
      clearTimeout(me.typeTimeout);
    }, 300)
  }
  return;
};

/**
 * Handler key press
 */
UIWikiSearchBox.prototype.enterHandler = function(evt) {
  var me = eXo.wiki.UIWikiSearchBox;
  if (me.currentItem) {
    var link = $(me.currentItem).find('a.ItemIcon')[0];
    if (link.href) {
      evt.cancelBubble = true;
      if (evt.stopPropagation)
        evt.stopPropagation();
      $(this.searchPopup).hide();
      window.location = link.href;
    }
  } else {
    me.doAdvanceSearch();
  }
};

UIWikiSearchBox.prototype.escapeHandler = function() {
  var me = eXo.wiki.UIWikiSearchBox;
  if (me.currentItem)
    me.currentItem = null;
  eXo.wiki.UIWikiSearchBox.hideMenu();
};

UIWikiSearchBox.prototype.arrowUpHandler = function() {
  var me = eXo.wiki.UIWikiSearchBox;
  if (!me.currentItem) {
    me.currentItem = this.menu.lastChild;
    $(me.currentItem).hover();
    return;
  }
  $(me.currentItem).removeClass('ItemOver');
  if (me.currentItem.previousSibling)
    me.currentItem = me.currentItem.previousSibling;
  else
    me.currentItem = this.menu.lastChild;
  $(me.currentItem).addClass('ItemOver');
};

UIWikiSearchBox.prototype.arrowDownHandler = function() {
  var me = eXo.wiki.UIWikiSearchBox;
  if (!me.currentItem) {
    me.currentItem = this.menu.firstChild;
    $(me.currentItem).addClass('ItemOver');
    return;
  }
  $(me.currentItem).removeClass('ItemOver');
  if (me.currentItem.nextSibling)
    me.currentItem = me.currentItem.nextSibling;
  else
    me.currentItem = this.menu.firstChild;
  $(me.currentItem).addClass('ItemOver');
};

UIWikiSearchBox.prototype.typeHandler = function(textbox) {
  var me = eXo.wiki.UIWikiSearchBox;
  var keyword = this.createKeyword(textbox.value);
  if (keyword == '') {
    eXo.wiki.UIWikiSearchBox.hideMenu();
    return;
  }
  var url = this.restURL + keyword;  
  // Create loading
  var me = eXo.wiki.UIWikiSearchBox;
  var searchBox = $(me.input).closest(".uiWikiSearchBox")[0];
  me.searchPopup = $(searchBox).find("ul.dropdown-menu")[0];
  $(me.searchPopup).show();
  $(me.searchPopup).mouseup(function(evt) {
    $(me.searchPopup).hide();
  });
  $(me.searchPopup).html('');
  var textNode = document.createTextNode('');
  $(me.searchPopup).append(textNode);
  var searchItemNode = $('<li/>').append($('<a/>', {'text' : me.loading}));
  $(searchItemNode).insertBefore(textNode);
  me.makeRequest(url, me.typeCallback);
};

UIWikiSearchBox.prototype.makeRequest = function(url, callback) {
  var me = eXo.wiki.UIWikiSearchBox;
  $.get(url,{},function(data) {
    callback(data);
  });  
};

UIWikiSearchBox.prototype.typeCallback = function(data) {
  if (!data)
    return;
  eXo.wiki.UIWikiSearchBox.renderMenu(data);
};

UIWikiSearchBox.prototype.doAdvanceSearch = function() {
  var me = eXo.wiki.UIWikiSearchBox;
  var action = $(me.input).closest('.uiWikiSearchBox')[0];
  action = $(action).find('a.AdvancedSearch')[0];
  eXo.wiki.UIWikiAjaxRequest.makeNewHash('#AdvancedSearch');
  action.onclick();
}

/**
 * Render Contextual Search Menu
 */
UIWikiSearchBox.prototype.renderMenu = function(data) {
  var me = eXo.wiki.UIWikiSearchBox;
  var searchBox = $(me.input).closest(".uiWikiSearchBox")[0];
  me.searchPopup = $(searchBox).find("ul.dropdown-menu")[0];
  var resultLength = data.jsonList.length;
  
  if (resultLength > 0) {
    $(me.searchPopup).show();
    $(me.searchPopup).mouseup(function(evt) {
      $(me.searchPopup).hide();
      evt.cancelBubble = true;
      if (evt.stopPropagation())
        evt.stopPropagation();
    })
    $(me.searchPopup).html('');
    var textNode = document.createTextNode('');
    $(me.searchPopup).append(textNode);
  }

  for ( var i = 0; i < resultLength; i++) {
    var itemNode = me.buildChild(data.jsonList[i]);
    $(itemNode).insertBefore(textNode);
    // Check if title is outside of the container
    var link = $(itemNode).find(':first')[0];
    var keyword = me.input.value.trim();    
    var origin =  $(link).html();    
    var shorten =  me.shortenWord(link, me.input);
    if (origin!= shorten && keyword.length >= shorten.length-3) {
      $(link).html(me.doHighLight(shorten, shorten.substring(0,shorten.length-3)));
    } else {
      $(link).html(me.doHighLight(shorten, keyword));
    }
  }  
};

UIWikiSearchBox.prototype.buildChild = function(dataObject) {
  var me = eXo.wiki.UIWikiSearchBox;
  var menuItemNode = $('<li/>');
  var linkNode = $('<a/>');
  if (dataObject.type == "wiki:attachment") {
    $(linkNode).attr('href', dataObject.uri);
  } else {
    $(linkNode).attr('href', me.wikiNodeURI + dataObject.uri);
  }
  var keyword = me.input.value.trim();
  var labelResult = dataObject.fullTitle;
  $(linkNode).attr('title', labelResult);
  $(linkNode).html(labelResult);
  $(menuItemNode).append(linkNode);
  return menuItemNode;
};

/**
 * Other functions
 */
UIWikiSearchBox.prototype.createKeyword = function(str) {
  if (str.indexOf(",") != -1) {
    str = str.substr(str.lastIndexOf(",") + 1, str.length);
  }
  str = str.replace(/^\s*/, "");
  return str;
};

UIWikiSearchBox.prototype.hideMenu = function() {
  var me = eXo.wiki.UIWikiSearchBox;
  if (me.searchPopup)
    $(me.searchPopup).hide();
};

UIWikiSearchBox.prototype.doHighLight = function(text, keyword) {
  var hiRE = new RegExp("(" + keyword + ")", "gi");
  text = text.replace(hiRE, "<strong>$1</strong>");  
  return text;
}

UIWikiSearchBox.prototype.shortenWord = function(source, container) {
  var isCut = false;
  while (source.offsetWidth > container.offsetWidth) {
    isCut = true;
    var size = $(source).html().length;
    $(source).html($(source).html().substring(0, size - 1));
  }
  if (isCut) {    
    $(source).html($(source).html().substring(0, $(source).html().length - 4) + "...");
  }
  return $(source).html();
};

eXo.wiki.UIWikiSearchBox = new UIWikiSearchBox();
return eXo.wiki.UIWikiSearchBox;

})(base, uiForm, webuiExt, $);
