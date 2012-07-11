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

function UIWikiPortlet() {
};

gj(document).ready(function(){
    var breadCrumb = gj('div.UIWikiBreadCrumb')[0];
    var selected = gj(breadCrumb).find('a.Selected')[0];
    if(selected) {
      gj('title').html(gj(selected).text());
    }
});

UIWikiPortlet.prototype.init = function(portletId, linkId) {
  var me = eXo.wiki.UIWikiPortlet;
  me.wikiportlet = document.getElementById(portletId);
  me.changeModeLink = document.getElementById(linkId);

  // window.onload = function(event) {me.changeMode(event);};
  /*window.onbeforeunload = function(event) {
    me.changeMode(event);
  };*/

  gj(me.wikiportlet).mouseup(me.onMouseUp);
  /*gj(me.wikiportlet).keyup(me.onKeyUp);*/
}

UIWikiPortlet.prototype.onMouseUp = function(evt) {
  var me = eXo.wiki.UIWikiPortlet;
  var evt = evt || window.event;
  var target = evt.target || evt.srcElement;
  if (evt.button == 2)
    return;
  var searchPopup = gj(me.wikiportlet).find('div.SearchPopup')[0];
  if (searchPopup)
    gj(searchPopup).hide();
  var breadCrumbPopup = eXo.wiki.UIWikiPortlet.getBreadcrumbPopup();
  if (breadCrumbPopup) {
    gj(breadCrumbPopup).hide();
  }
  /*if (target.tagName == "A" || (target.tagName == "INPUT" && target.type == "button") || target.tagName == "SELECT"
      || target.tagName == "DIV" && target.className.indexOf("RefreshModeTarget") > 0) {
    eXo.wiki.UIWikiPortlet.changeMode(evt);
  }*/
}

UIWikiPortlet.prototype.onKeyUp = function(evt) {
  var evt = evt || window.event;
  var target = evt.target || evt.srcElement;
  if (target.tagName == "INPUT" && target.type == "text")
    if (evt.keyCode == 13)
      eXo.wiki.UIWikiPortlet.changeMode(evt);
}

UIWikiPortlet.prototype.changeMode = function(event) {
  setTimeout("eXo.wiki.UIWikiPortlet.timeChangeMode()", 200);
};

UIWikiPortlet.prototype.timeChangeMode = function() {
  var me = eXo.wiki.UIWikiPortlet;
  var currentURL = document.location.href;
  var mode = "";
   if(currentURL.indexOf("action=AddPage") > 0) {
    mode = "AddPage";
  } else if (currentURL.indexOf("#") > 0) {
    mode = currentURL.substring(currentURL.indexOf("#") + 1, currentURL.length);
    if (mode && mode.length > 0 && mode.charAt(0) == 'H') {
      mode = "";
    }
    if (mode.indexOf("/") > 0)
      mode = mode.substring(0, mode.indexOf("/"));
  } 
  var link = me.changeModeLink;
  var endParamIndex = link.href.lastIndexOf("')");
  var modeIndex = link.href.indexOf("&mode");
  if (modeIndex < 0)
    link.href = link.href.substring(0, endParamIndex) + "&mode=" + mode + "')";
  else
    link.href = link.href.substring(0, modeIndex) + "&mode=" + mode + "')";
  window.location = link.href;
};

UIWikiPortlet.prototype.showPopup = function(elevent, e) {
  var strs = [ "AddTagId", "goPageTop", "goPageBottom", "SearchForm" ];
  for ( var t = 0; t < strs.length; t++) {
    var elm = document.getElementById(strs[t]);
    if (elm)
      gj(elm).click(eXo.wiki.UIWikiPortlet.cancel);
  }
  if (!e)
    e = window.event;
  e.cancelBubble = true;
  var parent = gj(elevent).closest('div');
  var popup = gj(parent).find('div.UIPopupCategory')[0];
  if (gj(popup).css('display') == 'none') {
    gj(popup).show();
  } else {
    gj(popup).hide();
  }
};

UIWikiPortlet.prototype.cancel = function(evt) {
  var _e = window.event || evt;
  _e.cancelBubble = true;
};

/*
 * Render the breadcrumb again to fit with a half of screen width
 */
UIWikiPortlet.prototype.renderBreadcrumbs = function(uicomponentid, isLink) {
  var me = eXo.wiki.UIWikiPortlet;
  var component = document.getElementById(uicomponentid);
  var breadcrumb = gj(component).find('div.BreadcumbsInfoBar')[0];
  var breadcrumbPopup = gj(component).find('div.SubBlock')[0];
  var itemArray = gj(breadcrumb).find('a');
  var shortenFractor = 3 / 4;
  itemArray.splice(0,1);
  var ancestorItem = itemArray.get(0);
  itemArray.splice(0,1);
  var lastItem = itemArray.get(itemArray.length-1);
  itemArray.splice(itemArray.length-1,1);
  if (lastItem == undefined){
    return;
  }
  var parentLastItem = itemArray.get(itemArray.length-1);
  itemArray.splice(itemArray.length-1,1);
  if(parentLastItem == undefined) {
    return;
  }
  var popupItems = new Array();
  var firstTime = true;
  var content = gj(lastItem).html();
  while (breadcrumb.offsetWidth > shortenFractor * breadcrumb.parentNode.offsetWidth) {
    if (itemArray.length > 0) {
      var arrayLength = itemArray.length;
      var item = itemArray.splice(itemArray.length-1,1)[0];
      popupItems.push(item);
      if (firstTime) {
        firstTime = false;
        var newItem = gj(item).clone()[0];
        gj(newItem).html(' ... ');
        if (isLink) {
          gj(newItem).attr('href','#');
          gj(newItem).mouseover(me.showBreadcrumbPopup);
        }
        gj(item).replaceWith(newItem);
      } else {
        var leftBlock = gj(item).prev('div')[0];
        gj(leftBlock).remove();
        gj(item).remove();
      }
    } else {
      break;
    }
  }

  if (content.length != gj(lastItem).html().length) {
    gj(lastItem).html('<span title="' + content + '">' + gj(lastItem).html() + '...' + '</span>');
  }
  me.createPopup(popupItems, isLink, breadcrumbPopup);
};

UIWikiPortlet.prototype.createPopup = function(popupItems, isLink, breadcrumbPopup){
  if (isLink) {
    var popupItemDepth = -1;
    for (var index = popupItems.length - 1; index >= 0; index--) {
      gj(popupItems[index]).attr('class','ItemIcon MenuIcon');
      popupItemDepth++;
      var menuItem = gj('<div/>', {
        'class': 'MenuItem'
      });
      var previousDiv = menuItem;
      for (var i = 0; i < popupItemDepth; i++) {
        var marginLeftDiv = gj('<div/>', {
          'class': 'MarginLeftDiv'
        });
        gj(previousDiv).append(marginLeftDiv);
        previousDiv = marginLeftDiv;
        if (i == popupItemDepth - 1) {
          gj(previousDiv).append(popupItems[index]);
        }
      }
      if (popupItemDepth == 0) {
        gj(menuItem).append(popupItems[index]);
      }
      gj(breadcrumbPopup).append(menuItem);
    }    
  }
};

/*
 * Remove last characters of item until a condition happen
 */
UIWikiPortlet.prototype.shortenUntil = function(item, condition) {
  var isShortent = false;
  while (!condition() && gj(item).html().length > 3) {
    gj(item).html(gj(item).html().substring(0, gj(item).html().length - 1));
    isShortent = true;
  }
  if (isShortent) {
    if(gj(item).html().length > 6) {
      gj(item).html(gj(item).html().substring(0, gj(item).html().length - 3));
    }
    gj(item).html(gj(item).html() + ' ... ');
  }
};

UIWikiPortlet.prototype.getBreadcrumbPopup = function() {
  var breadcrumb = document.getElementById("UIWikiBreadCrumb");
  var breadcrumbPopup = gj(breadcrumb).find('div.BreadcumPopup')[0];
  return breadcrumbPopup;
};

UIWikiPortlet.prototype.showBreadcrumbPopup = function(evt) {
  var breadcrumbPopup = eXo.wiki.UIWikiPortlet.getBreadcrumbPopup();
  var ellipsis = evt.target || evt.srcElement;
  var isRTL = eXo.core.I18n.isRT();
  var offsetLeft = eXo.core.Browser.findPosX(ellipsis, isRTL) - 20;
  var offsetTop = gj(ellipsis).offset().top + 20;
  gj(breadcrumbPopup).css({
    'z-index': '100',
    left: offsetLeft + 'px',
    top: offsetTop + 'px'
  })
  gj(breadcrumbPopup).show();
};

UIWikiPortlet.prototype.createURLHistory = function (uicomponentId, isShow) {
  if(isShow || isShow === 'true'){
    setTimeout("eXo.wiki.UIWikiPortlet.urlHistory('"+uicomponentId+"')", 500);
  }
};

UIWikiPortlet.prototype.urlHistory = function (uicomponentId) {
  var component = document.getElementById(uicomponentId);
  if(component) {
    var local = String(window.location);
    if(local.indexOf('#') < 0 || local.indexOf('#') === (local.length-1)) {
      window.location = local.replace('#', '') + '#ShowHistory';
    }
  }
};

UIWikiPortlet.prototype.makeRenderingErrorsExpandable = function (uicomponentId) {
  var uicomponent = document.getElementById(uicomponentId);
  if(uicomponent) {
    var renderingErrors = gj(uicomponent).find('div.xwikirenderingerror');
    for (i=0;i<renderingErrors.length;i++) {
    var renderingError = renderingErrors[i];
    var descriptionError = renderingError.nextSibling;
    if (gj(descriptionError).html() !== "" && gj(descriptionError).hasClass('xwikirenderingerrordescription')) {
      gj(renderingError).css('cursor','pointer');
      gj(renderingError).click(function(){
        gj(this.nextSibling).toggleClass('hidden');
        });
      }
    }
  }
};

UIWikiPortlet.prototype.decorateSpecialLink = function(uicomponentId) {
  var uicomponent = document.getElementById(uicomponentId);
  var invalidChars = gj(uicomponent).find('div.InvalidChars')[0];
  var invalidCharsMsg = gj(invalidChars).text();  
  if (uicomponent) {
    var linkSpans = gj(uicomponent).find('span.wikicreatelink');
    for (i = 0; i < linkSpans.length; i++) {
      var linkSpan = linkSpans[i];
      var pageLink = linkSpan.childNodes[0];
      if (typeof(pageLink) != "undefined" && gj(pageLink).attr('href') == "javascript:void(0);") {
        gj(pageLink).click(function(event) {
            alert(invalidCharsMsg);
            return;
        });
      }
    }
  }
};

UIWikiPortlet.prototype.keepSessionAlive = function(isKeepSessionAlive) {
  if (isKeepSessionAlive == true) {
    eXo.session.itvInit();
  } else {
    eXo.session.destroyItv();
    eXo.session.initialized = false;
    eXo.session.openUrl = null;
  }
};

UIWikiPortlet.prototype.initMacros = function() {
  eXo.wiki.UIRelated.initMacros();
  eXo.wiki.UITreeExplorer.initMacros();
};

UIWikiPortlet.prototype.decorateInput = function(input, defaultValue, defaultCondition) {
  if (gj(input).val() == defaultValue && defaultCondition )
    gj(input).css('color', '#9A9A9A');
  input.form.onsubmit = function() {
    return false;
  };
  gj(input).focus(function() {
    if (gj(this).val() == defaultValue && defaultCondition)
      gj(this).val('');
    gj(this).css('color', 'black');
  });
  gj(input).blur(function() {
    if (gj(this).val() == '') {
      gj(this).val(defaultValue);
      gj(this).css('color', '#9A9A9A');
    }
  });
};

UIWikiPortlet.prototype.getKeynum = function(event) {
  var keynum = false ;
  if(window.event) { /* IE */
    keynum = window.event.keyCode;
    event = window.event ;
  } else if(event.which) { /* Netscape/Firefox/Opera */
    keynum = event.which ;
  }
  if(keynum == 0) {
    keynum = event.keyCode ;
  }
  return keynum ;
};

eXo.wiki.UIWikiPortlet = new UIWikiPortlet();

/** ******************* Other functions ***************** */

String.prototype.trim = function() {
  return this.replace(/^\s+|\s+$/g, '');
};

String.prototype.replaceAll = function(oldText, newText) {
  return this.replace(new RegExp(oldText, "g"), newText);
}