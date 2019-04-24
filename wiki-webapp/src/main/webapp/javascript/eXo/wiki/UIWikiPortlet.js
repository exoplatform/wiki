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

function UIWikiPortlet() {
};

$(document).ready(function(){
    var breadCrumb = $('div.UIWikiBreadCrumb')[0];
    var selected = $(breadCrumb).find('a.Selected')[0];
    if(selected) {
      document.title = $(selected).text();
    }
    
    // Using scrolling when clicking to anchor link
    $(".wikilink > a").click(function(e) 
    { 
      var href = $(this).attr('href');
      if (href.lastIndexOf("#", 0) == 0) {
        e.preventDefault();
        var destLink = $("#"+href.substring(1).replace(/[^\w\s]/gi, '\\$&'));
        $('.uiRightContainerArea').scrollTop(0);
        $('.uiRightContainerArea').animate({scrollTop: destLink.offset().top}, 50);
      }
    });
});

var isMovePage=false;

UIWikiPortlet.prototype.init = function(portletId, linkId) {
  var me = eXo.wiki.UIWikiPortlet;
  me.wikiportlet = document.getElementById(portletId);
  me.changeModeLink = document.getElementById(linkId);
  
  $(window).ready(function(){
    me.changeMode();
    
    // Init tooltip
    $("*[rel='tooltip']").tooltip();
    
    // Init page tree macro
    me.initMacros();
  });

  $(window).bind('beforeunload', function() {
    me.changeMode();
  });
  
  $(me.wikiportlet).mouseup(me.onMouseUp);
  $(me.wikiportlet).keyup(me.onKeyUp);
  
  $("*[rel='tooltip']").tooltip();
}

UIWikiPortlet.prototype.onMouseUp = function(evt) {
  var me = eXo.wiki.UIWikiPortlet;
  var evt = evt || window.event;
  var target = evt.target || evt.srcElement;
  if (evt.button == 2)
    return;
  var searchPopup = $(me.wikiportlet).find('div.SearchPopup')[0];
  if (searchPopup)
    $(searchPopup).hide();
  var breadCrumbPopup = eXo.wiki.UIWikiPortlet.getBreadcrumbPopup();
  if (breadCrumbPopup) {
    $(breadCrumbPopup).hide();
  }
  /*if (target.tagName == "A" || (target.tagName == "INPUT" && target.type == "button") || target.tagName == "SELECT"
      || target.tagName == "DIV" && target.className.indexOf("RefreshModeTarget") > 0) {
    eXo.wiki.UIWikiPortlet.changeMode();
  }*/
}

UIWikiPortlet.prototype.onKeyUp = function(evt) {
  var evt = evt || window.event;
  var target = evt.target || evt.srcElement;
  if (target.tagName == "INPUT" && target.type == "text")
    if (evt.keyCode == 13)
      eXo.wiki.UIWikiPortlet.changeMode();
}

UIWikiPortlet.prototype.changeMode = function() {
  // setTimeout("eXo.wiki.UIWikiPortlet.timeChangeMode()", 200);
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
      $(elm).click(eXo.wiki.UIWikiPortlet.cancel);
  }
  if (!e)
    e = window.event;
  e.cancelBubble = true;
  var parent = $(elevent).closest('div');
  var popup = $(parent).find('div.UIPopupCategory')[0];
  if ($(popup).css('display') == 'none') {
    $(popup).show();
  } else {
    $(popup).hide();
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
  var breadcrumb = $(component).find('div.BreadcumbsInfoBar')[0];
  var breadcrumbPopup = $(component).find('div.SubBlock')[0];
  var itemArray = $(breadcrumb).find('a');
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
  var content = $(lastItem).html();
  while (breadcrumb.offsetWidth > shortenFractor * breadcrumb.parentNode.offsetWidth) {
    if (itemArray.length > 0) {
      var arrayLength = itemArray.length;
      var item = itemArray.splice(itemArray.length-1,1)[0];
      popupItems.push(item);
      if (firstTime) {
        firstTime = false;
        var newItem = $(item).clone()[0];
        $(newItem).html(' ... ');
        if (isLink) {
          $(newItem).attr('href','#');
          $(newItem).mouseover(me.showBreadcrumbPopup);
        }
        $(item).replaceWith(newItem);
      } else {
        var leftBlock = $(item).prev('div')[0];
        $(leftBlock).remove();
        $(item).remove();
      }
    } else {
      break;
    }
  }

  if (content.length != $(lastItem).html().length) {
    $(lastItem).html('<span title="' + content + '">' + $(lastItem).html() + '...' + '</span>');
  }
  me.createPopup(popupItems, isLink, breadcrumbPopup);
};

UIWikiPortlet.prototype.createPopup = function(popupItems, isLink, breadcrumbPopup){
  if (isLink) {
    var popupItemDepth = -1;
    for (var index = popupItems.length - 1; index >= 0; index--) {
      $(popupItems[index]).attr('class','ItemIcon MenuIcon');
      popupItemDepth++;
      var menuItem = $('<div/>', {
        'class': 'MenuItem'
      });
      var previousDiv = menuItem;
      for (var i = 0; i < popupItemDepth; i++) {
        var marginLeftDiv = $('<div/>', {
          'class': 'MarginLeftDiv'
        });
        $(previousDiv).append(marginLeftDiv);
        previousDiv = marginLeftDiv;
        if (i == popupItemDepth - 1) {
          $(previousDiv).append(popupItems[index]);
        }
      }
      if (popupItemDepth == 0) {
        $(menuItem).append(popupItems[index]);
      }
      $(breadcrumbPopup).append(menuItem);
    }    
  }
};

/*
 * Remove last characters of item until a condition happen
 */
UIWikiPortlet.prototype.shortenUntil = function(item, condition) {
  var isShortent = false;
  while (!condition() && $(item).html().length > 3) {
    $(item).html($(item).html().substring(0, $(item).html().length - 1));
    isShortent = true;
  }
  if (isShortent) {
    if($(item).html().length > 6) {
      $(item).html($(item).html().substring(0, $(item).html().length - 3));
    }
    $(item).html($(item).html() + ' ... ');
  }
};

UIWikiPortlet.prototype.getBreadcrumbPopup = function() {
  var breadcrumb = document.getElementById("UIWikiBreadCrumb");
  var breadcrumbPopup = $(breadcrumb).find('div.BreadcumPopup')[0];
  return breadcrumbPopup;
};

UIWikiPortlet.prototype.showBreadcrumbPopup = function(evt) {
  var breadcrumbPopup = eXo.wiki.UIWikiPortlet.getBreadcrumbPopup();
  var ellipsis = evt.target || evt.srcElement;
  var isRTL = eXo.core.I18n.isRT();
  var offsetLeft = eXo.core.Browser.findPosX(ellipsis, isRTL) - 20;
  var offsetTop = $(ellipsis).offset().top + 20;
  $(breadcrumbPopup).css({
    'z-index': '100',
    left: offsetLeft + 'px',
    top: offsetTop + 'px'
  })
  $(breadcrumbPopup).show();
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
    var renderingErrors = $(uicomponent).find('div.xwikirenderingerror');
    for (i=0;i<renderingErrors.length;i++) {
    var renderingError = renderingErrors[i];
    var descriptionError = renderingError.nextSibling;
    if ($(descriptionError).html() !== "" && $(descriptionError).hasClass('xwikirenderingerrordescription')) {
      $(renderingError).css('cursor','pointer');
      $(renderingError).click(function(){
        $(this.nextSibling).toggleClass('hidden');
        });
      }
    }
  }
};

UIWikiPortlet.prototype.decorateSpecialLink = function(uicomponentId) {
  var uicomponent = document.getElementById(uicomponentId);
  var invalidChars = $(uicomponent).find('div.InvalidChars')[0];
  var invalidCharsMsg = $(invalidChars).text();  
  if (uicomponent) {
    var linkSpans = $(uicomponent).find('span.wikicreatelink');
    for (i = 0; i < linkSpans.length; i++) {
      var linkSpan = linkSpans[i];
      var pageLink = linkSpan.childNodes[0];
      if (typeof(pageLink) != "undefined" && $(pageLink).attr('href') == "javascript:void(0);") {
        $(pageLink).click(function(event) {
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
  input.form.onsubmit = function() {
    return false;
  };
  $(input).attr("placeholder", defaultValue);
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
UIWikiPortlet.prototype.ajaxRedirect = function(url) {
  url =  url.replace(/&amp;/g, "&") ;
  window.location.href = url ;
}

UIWikiPortlet.prototype.fixImageUrl = function(lastUpdated) {
  var content = $(".uiWikiContentDisplay", document.body)[0];
  if (content) {
    $("img", content).each(function(index, elem) {
      var src = elem.getAttribute("src");
      var fragmentParser = src.split("#");
      var fragment = fragmentParser[1];
      var queryParser = fragmentParser[0].split("?");
      var baseURL = queryParser[0];
      var queryString = queryParser[1];
      var queryStringBuilder = "";
      if (queryString && queryString.length !=0) {
        var params = queryString.split("&");
        for (i =0; i< params.length; i++) {
          if( params[i].split("=")[0] != "lastUpdated" ) {
            queryStringBuilder +=  params[i] + "&";
          }
        }
      }
      var newQueryString = "?" + queryStringBuilder + "lastUpdate=" + lastUpdated; 
      if (fragment) {
        src = baseURL + newQueryString + "#" + fragment;
      } else if (src.indexOf("#")) {
        src = baseURL + newQueryString + "#";
      } else {
        src = baseURL + newQueryString;
      }
      elem.setAttribute("src", src);
      });
    }
},

eXo.wiki.UIWikiPortlet = new UIWikiPortlet();
return eXo.wiki.UIWikiPortlet;

})(base, uiForm, webuiExt, $);
