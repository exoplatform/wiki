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

/**
 * @author Lai Trung Hieu
 */


function UIWikiSearchBox() {
  this.restURL = null;
  this.input = null;
  this.searchPopup = null;
  this.searchType = "";
  this.menu = null;
  this.wikiNodeURI = null;
};

UIWikiSearchBox.prototype.init = function(componentId, searchInputName, searchLabel, wikiNodeURI) {

  this.wikiNodeURI = wikiNodeURI;
  var uiComponent = document.getElementById(componentId);
  var restInput = uiComponent["restURL"];
  this.input = uiComponent[searchInputName];
  gj(this.input).attr('autocomplete', 'off');
  gj(this.input).val(searchLabel);  
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
    var link = gj(me.currentItem).find('a.ItemIcon')[0];
    if (link.href) {
      evt.cancelBubble = true;
      if (evt.stopPropagation)
        evt.stopPropagation();
      gj(this.searchPopup).hide();
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
    gj(me.currentItem).hover();
    return;
  }
  gj(me.currentItem).removeClass('ItemOver');
  if (me.currentItem.previousSibling)
    me.currentItem = me.currentItem.previousSibling;
  else
    me.currentItem = this.menu.lastChild;
  gj(me.currentItem).addClass('ItemOver');
};

UIWikiSearchBox.prototype.arrowDownHandler = function() {
  var me = eXo.wiki.UIWikiSearchBox;
  if (!me.currentItem) {
    me.currentItem = this.menu.firstChild;
    gj(me.currentItem).addClass('ItemOver');
    return;
  }
  gj(me.currentItem).removeClass('ItemOver');
  if (me.currentItem.nextSibling)
    me.currentItem = me.currentItem.nextSibling;
  else
    me.currentItem = this.menu.firstChild;
  gj(me.currentItem).addClass('ItemOver');
};

UIWikiSearchBox.prototype.typeHandler = function(textbox) {
  var keyword = this.createKeyword(textbox.value);
  if (keyword == '') {
    eXo.wiki.UIWikiSearchBox.hideMenu();
    return;
  }
  var url = this.restURL + keyword;  
  // Create loading
  var me = eXo.wiki.UIWikiSearchBox;
  var searchBox = gj(this.input).closest(".UIWikiSearchBox")[0];
  this.searchPopup = gj(searchBox).find("div.SearchPopup")[0];
  gj(this.searchPopup).show();
  gj(this.searchPopup).mouseup(function(evt) {
    gj(this.style).hide();
  });
  this.menu = gj(this.searchPopup).find("div.SubBlock")[0];
  gj(this.menu).html('');
  var textNode = document.createTextNode('');
  gj(this.menu).append(textNode);
  var searchItemNode = gj('<div/>', {
    'class': 'MenuItem Horizon'
  }).append(gj('<div/>', {
    'class': 'MenuText'
  }).append(gj('<a/>', {
      'class': 'ItemIcon MenuIcon',
      'text' : me.loading
     }
    )
   )
  )
  gj(searchItemNode).insertBefore(textNode);
  this.makeRequest(url, this.typeCallback);
};

UIWikiSearchBox.prototype.makeRequest = function(url, callback) {
  var me = eXo.wiki.UIWikiSearchBox;
  gj.get(url,{},function(data) {
    callback(data);
  });  
};

UIWikiSearchBox.prototype.typeCallback = function(data) {
  if (!data)
    return;
  eXo.wiki.UIWikiSearchBox.renderMenu(data);
};

UIWikiSearchBox.prototype.doAdvanceSearch = function() {
  var action = gj(this.input).closest('.SearchForm')[0];
  action = gj(action).find('a.AdvancedSearch')[0];
  eXo.wiki.UIWikiAjaxRequest.makeNewHash('#AdvancedSearch');
  action.onclick();
}

/**
 * Render Contextual Search Menu
 */

UIWikiSearchBox.prototype.renderMenu = function(data) {
  var me = eXo.wiki.UIWikiSearchBox;
  var searchBox = gj(this.input).closest('.UIWikiSearchBox');
  this.searchPopup = gj(searchBox).find('div.SearchPopup')[0];
  gj(this.searchPopup).show();
  gj(this.searchPopup).mouseup(function(evt) {
    gj(this).hide();
    evt.cancelBubble = true;
    if (evt.stopPropagation())
      evt.stopPropagation();
  })
  this.menu = gj(this.searchPopup).find('div.SubBlock')[0];
  gj(this.menu).html('');
  var textNode = document.createTextNode('');
  gj(this.menu).append(textNode);

  var searchItemNode = gj('<div/>',{
    'class': 'MenuItem Horizon'
  });
  var searchText = gj('<div/>', {
    'class': 'MenuText'
  });
  var linkNode = gj('<a/>', {
    'class': 'ItemIcon MenuIcon SearchIcon',
    'href' : 'javascript:eXo.wiki.UIWikiSearchBox.doAdvanceSearch();',
    'text' : "Seach for \'" + this.input.value + "\'",
    'title': "Seach for \'" + this.input.value + "\'"
   });
  gj(searchItemNode).append(gj(searchText).append(linkNode));
  gj(searchItemNode).insertBefore(textNode);
  me.shortenWord(linkNode[0], searchText[0]); 
  var resultLength = data.jsonList.length;
  for ( var i = 0; i < resultLength; i++) {
    var itemNode = this.buildChild(data.jsonList[i]);
    gj(itemNode).insertBefore(searchItemNode);
    // Check if title is outside of the container
    var linkContainer = gj(itemNode).find(':first')[0];
    var link = gj(linkContainer).find(':first')[0];
    var keyword = this.input.value.trim();    
    var origin =  gj(link).html();    
    var shorten =  me.shortenWord(link, linkContainer);
    if (origin!= shorten && keyword.length >= shorten.length-3)
      gj(link).html(me.doHighLight(shorten, shorten.substring(0,shorten.length-3)));
    else
      gj(link).html(me.doHighLight(shorten, keyword));
  }  
  gj(this.menu.lastChild).remove();
};

UIWikiSearchBox.prototype.buildChild = function(dataObject) {
  var menuItemNode = gj('<div/>');
  gj(menuItemNode).attr('class','MenuItem TextItem ' + dataObject.fileType);
  if (this.searchType != dataObject.type) {
    gj(menuItemNode).addClass('Horizon');
  }  
  this.searchType = dataObject.type;
  var searchText = gj('<div/>',{
    'class' : 'MenuText'
  });
  var linkNode = gj('<a/>', {
    'class' : 'ItemIcon MenuIcon'
  });
  if (dataObject.type == "wiki:attachment") {
    gj(linkNode).attr('href', dataObject.uri);
  } else {
    gj(linkNode).attr('href', this.wikiNodeURI + dataObject.uri);
  }
  var keyword = this.input.value.trim();
  var labelResult = dataObject.fullTitle;
  gj(linkNode).attr('title', labelResult);
  gj(linkNode).html(labelResult);
  gj(menuItemNode).append(gj(searchText).append(linkNode));
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
  if (this.searchPopup)
    gj(this.searchPopup).hide();
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
    var size = gj(source).html().length;
    gj(source).html(gj(source).html().substring(0, size - 1));
  }
  if (isCut) {    
    gj(source).html(gj(source).html().substring(0, gj(source).html().length - 4) + "...");
  }
  return gj(source).html();
};

eXo.wiki.UIWikiSearchBox = new UIWikiSearchBox();
