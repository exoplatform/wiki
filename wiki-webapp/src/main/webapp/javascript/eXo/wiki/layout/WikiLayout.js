/**
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
if (!eXo.wiki) {
  eXo.wiki = {};
};
 

function WikiLayout() {
  this.posX = 0;
  this.posY = 0;
  this.portletId = 'uiWikiPortlet';
  this.wikiBodyClass = 'wiki-body';
  this.bodyClass = '';
  this.myBody;
  this.myHtml;
  this.min_height = 300;
  this.currWidth = 0;
  this.bottomPadding = 50;
  this.leftMinWidth  = 235;
  this.rightMinWidth = 250;
  this.userName      = "";
};

$(window).resize(function() {
  eXo.core.Browser.managerResize();
  if(this.currWidth != document.documentElement.clientWidth) {
    eXo.wiki.WikiLayout.processeWithHeight();
  }
  this.currWidth  = document.documentElement.clientWidth;
});

WikiLayout.prototype.getCookie = function (c_name) {
  var i, x, y, ARRcookies = document.cookie.split(";");
  for (i = 0; i < ARRcookies.length; i++) {
    x = ARRcookies[i].substr(0, ARRcookies[i].indexOf("="));
    y = ARRcookies[i].substr(ARRcookies[i].indexOf("=") + 1);
    x = x.replace(/^\s+|\s+$/g, "");
    if (x == c_name) {
      return unescape(y);
    }
  }
  return null;
}
/**
 * @function   setCookie
 * @return     saved cookie with given name
 * @author     vinh_nguyen@exoplatform.com
 */
WikiLayout.prototype.setCookie = function (c_name, value, exdays) {
  var exdate = new Date();
  exdate.setDate(exdate.getDate() + exdays);
  var c_value = escape(value) + ((exdays == null) ? "" : "; expires=" + exdate.toUTCString());
  document.cookie = c_name + "=" + c_value;
}

WikiLayout.prototype.init = function(prtId, _userName) {
	this.userName = _userName;
  try {
    if(String(typeof this.myBody) == "undefined" || !this.myBody) {
      this.myBody = $("body")[0];
      this.bodyClass = $(this.myBody).attr('class');
      this.myHtml = $("html")[0];
    }
  }catch(e){};
  
  try{
    if(prtId.length > 0) this.portletId = prtId;
    var isIE = ($.browser.msie != undefined)
    var idPortal = (isIE) ? 'UIWorkingWorkspace' : 'UIPortalApplication';
    this.portal = document.getElementById(idPortal);
    var portlet = document.getElementById(this.portletId);
    this.wikiLayout = $(portlet).find('div.uiWikiMiddleArea')[0];
    this.resizeBar = $(this.wikiLayout).find('div.resizeBar')[0];
    this.colapseLeftContainerButton = $(this.wikiLayout).find('div.resizeButton')[0];
    var showLeftContainer = eXo.wiki.WikiLayout.getCookie(this.userName + "_ShowLeftContainer") == 'true'?'none':'block';
    this.verticalLine = $(this.wikiLayout).find('div.VerticalLine')[0];
    if (this.resizeBar) {
      this.leftArea = $(this.resizeBar).prev('div')[0];
      this.rightArea = $(this.resizeBar).next('div')[0];
      var leftWidth = eXo.wiki.WikiLayout.getCookie(this.userName + "_leftWidth");
      if (this.leftArea && this.rightArea && (leftWidth != null) && (leftWidth != "") && (leftWidth * 1 > 0)) {
        $(this.leftArea).width(leftWidth + 'px');
      }	  
      $(this.resizeBar).mousedown(eXo.wiki.WikiLayout.exeRowSplit);
      $(this.colapseLeftContainerButton).click(eXo.wiki.WikiLayout.showHideSideBar);
    }
	if(this.wikiLayout) {
      this.processeWithHeight();
      eXo.core.Browser.addOnResizeCallback("WikiLayout", eXo.wiki.WikiLayout.processeWithHeight);
    }
	
	this.leftArea.style.display = showLeftContainer;
	this.showHideSideBar(null, true);
    
  }catch(e){
   return;
  };
};

WikiLayout.prototype.setClassBody = function(clazz) {
  if(this.myBody && this.myHtml) {
    if (String(clazz) != this.bodyClass) {
      $(this.myBody).attr('class', clazz + " " + this.bodyClass);
      $(this.myHtml).attr('class', clazz);
    } else {
      $(this.myBody).attr('class', clazz);
      $(this.myHtml).attr('class', '');
    }
  }
};

WikiLayout.prototype.processeWithHeight = function() {
  var WikiLayout = eXo.wiki.WikiLayout;
  if (WikiLayout.wikiLayout) {
    WikiLayout.setClassBody(WikiLayout.wikiBodyClass);
    WikiLayout.setHeightLayOut();
    WikiLayout.setWithLayOut();
  } else {
    WikiLayout.init('');
  }
};

WikiLayout.prototype.setWithLayOut = function() {
  var WikiLayout = eXo.wiki.WikiLayout;
  var maxWith = $(WikiLayout.wikiLayout).width();
  var lWith = 0;
  if (WikiLayout.leftArea && WikiLayout.resizeBar) {
    lWith = WikiLayout.leftArea.offsetWidth + WikiLayout.resizeBar.offsetWidth + 2;
  }
  if (WikiLayout.rightArea) {
    $(WikiLayout.rightArea).width((maxWith - lWith -50) + 'px'); //left and right padding
  }
};

WikiLayout.prototype.setHeightLayOut = function() {
  var WikiLayout = eXo.wiki.WikiLayout;
  var layout = eXo.wiki.WikiLayout.wikiLayout;
  var hdef = document.documentElement.clientHeight - layout.offsetTop;
  var hct = hdef * 1;
  $(layout).css('height', hdef + 'px');
  var delta = WikiLayout.heightDelta();
  if(delta > hdef) {
    WikiLayout.setClassBody(WikiLayout.bodyClass);
  }
  while ((delta = WikiLayout.heightDelta()) > 0 && hdef > delta) {
    hct = hdef - delta;
    $(layout).css('height', hct + "px");
    hdef = hdef - 2;
  }
  hct-=20; //Padding-bottom of wikiLayout
  if (WikiLayout.leftArea && WikiLayout.resizeBar) {
    $(WikiLayout.leftArea).height(hct + "px");
	var resideBarContent = $(WikiLayout.resizeBar).find("div.resizeBarContent:first")[0];
	if (resideBarContent) {
	  $(resideBarContent).height(hct + "px");
	}
  } else if (WikiLayout.verticalLine) {
    $(WikiLayout.verticalLine).height(hct + "px");
  }
  
  if (WikiLayout.rightArea) {
    $(WikiLayout.rightArea).height(hct + "px");
  }
  
  WikiLayout.setHeightRightContent();
};

WikiLayout.prototype.setHeightRightContent = function() {
  var WikiLayout = eXo.wiki.WikiLayout;
  if(!WikiLayout.wikiLayout) WikiLayout.init('');
  var pageArea =  $(WikiLayout.rightArea).find('div.UIWikiPageArea')[0];
  if(pageArea) {
    var bottomArea = $(WikiLayout.rightArea).find('div.UIWikiBottomArea')[0];
    var pageContainer = $(WikiLayout.rightArea).find('div.UIWikiPageContainer')[0];
    if(bottomArea) {
      var pageAreaHeight = (WikiLayout.rightArea.offsetHeight - bottomArea.offsetHeight - WikiLayout.bottomPadding);
      if ((pageAreaHeight > pageArea.offsetHeight) && (pageAreaHeight > WikiLayout.min_height)) {
        $(pageArea).height(pageAreaHeight + "px");
      } else if (pageArea.offsetHeight < WikiLayout.min_height) {
        $(pageArea).height(WikiLayout.min_height + "px");
      }
	  var pageContent = $(pageArea).find("div.uiWikiPageContentArea");
	  if (pageContent) $(pageContent).height(WikiLayout.rightArea.offsetHeight + "px");
    }
  }
};
/**
 * Function      showHideSideBar
 * @purpose      Switch the visible of the leftcontainer
 * @author       vinh_nguyen@exoplatform.com
 */
WikiLayout.prototype.showHideSideBar = function (e, savedValue) {
  var WikiLayout = eXo.wiki.WikiLayout;
  var portlet = document.getElementById(WikiLayout.portletId);
  var wikiMiddleArea = $(portlet).find('div.uiWikiMiddleArea')[0];
  var allowedWidth = wikiMiddleArea.offsetWidth - 50; //substract the left and right padding 
  var newValue = WikiLayout.leftArea.style.display;
  if (WikiLayout.resizeBar && WikiLayout.leftArea) {
    if (newValue == 'none') {
      WikiLayout.leftArea.style.display = 'block';
      $(WikiLayout.colapseLeftContainerButton).removeClass("showLeftContent");
      $(WikiLayout.resizeBar).removeClass("resizeNoneBorder");
	  $(wikiMiddleArea).removeClass("nonePaddingLeft");	  
      if (allowedWidth - WikiLayout.resizeBar.offsetWidth - 2 - WikiLayout.leftArea.offsetWidth < WikiLayout.rightMinWidth) {
        WikiLayout.leftArea.style.width = allowedWidth - WikiLayout.resizeBar.offsetWidth - 2 - WikiLayout.rightMinWidth + "px";
        eXo.wiki.WikiLayout.setCookie(eXo.wiki.WikiLayout.userName + "_leftWidth", eXo.wiki.WikiLayout.leftArea.offsetWidth, 20);
      }
      $(WikiLayout.rightArea).width(allowedWidth - WikiLayout.resizeBar.offsetWidth - WikiLayout.leftArea.offsetWidth + "px");
	  var iElement = $(WikiLayout.colapseLeftContainerButton).find("i:first")[0];
	  iElement.className = "uiIconMiniArrowLeft";
    } else {
      WikiLayout.leftArea.style.display = 'none';
      $(WikiLayout.colapseLeftContainerButton).addClass("showLeftContent");
	  var iElement = $(WikiLayout.colapseLeftContainerButton).find("i:first")[0];
	  iElement.className = "uiIconMiniArrowRight";
      $(WikiLayout.resizeBar).addClass("resizeNoneBorder");
	  $(wikiMiddleArea).addClass("nonePaddingLeft");
      $(WikiLayout.rightArea).width(allowedWidth - WikiLayout.resizeBar.offsetWidth + 25 + "px"); //right padding, leftpadding is removed
    }
    if (!savedValue) {
      eXo.wiki.WikiLayout.setCookie(WikiLayout.userName + "_ShowLeftContainer", WikiLayout.leftArea.style.display == 'block'?'true':'false', 20);
    }
  } else {
    $(WikiLayout.rightArea).width(wikiMiddleArea.offsetWidth - WikiLayout.resizeBar.offsetWidth - 2 + "px");
  }
}
WikiLayout.prototype.exeRowSplit = function(e) {
  _e = (window.event) ? window.event : e;
  var WikiLayout = eXo.wiki.WikiLayout;
  var portlet = document.getElementById(WikiLayout.portletId);
  var wikiMiddleArea = $(portlet).find('div.uiWikiMiddleArea')[0];
  $(wikiMiddleArea).addClass("uiWikiPortletNoSelect");
  WikiLayout.posX = _e.clientX;
  WikiLayout.posY = _e.clientY;
  if (WikiLayout.leftArea && WikiLayout.rightArea
      && $(WikiLayout.leftArea).css('display') != "none"
      && $(WikiLayout.rightArea).css('display') != "none") {
    WikiLayout.adjustHorizon();
  }
};

WikiLayout.prototype.adjustHorizon = function() {
  this.leftX = this.leftArea.offsetWidth;
  this.rightX = this.rightArea.offsetWidth;
  $(document).mousemove(eXo.wiki.WikiLayout.adjustWidth);
  $(document).mouseup(eXo.wiki.WikiLayout.clear);
  if (eXo.wiki.WikiLayout.resizeBar) $(eXo.wiki.WikiLayout.resizeBar).addClass("resizeBarDisplay");
};

WikiLayout.prototype.adjustWidth      = function(evt) {
  evt = (window.event) ? window.event : evt;
  var WikiLayout = eXo.wiki.WikiLayout;
  var portlet = document.getElementById(WikiLayout.portletId);
  var wikiMiddleArea = $(portlet).find('div.uiWikiMiddleArea')[0];
  var allowedWidth = wikiMiddleArea.offsetWidth - 50; //Substract the padding
  var delta = evt.clientX - WikiLayout.posX;
  var leftWidth = (WikiLayout.leftX + delta);
  var rightWidth = (allowedWidth - leftWidth - WikiLayout.resizeBar.offsetWidth - 2); //Padding of wikiLayout and PageArea
  if (leftWidth < WikiLayout.leftMinWidth){
  	leftWidth = WikiLayout.leftMinWidth;
  	rightWidth = allowedWidth - leftWidth -WikiLayout.resizeBar.offsetWidth - 2;
  }
  if (rightWidth < WikiLayout.rightMinWidth) {
  	leftWidth = allowedWidth - WikiLayout.rightMinWidth -WikiLayout.resizeBar.offsetWidth - 2;
  	rightWidth = WikiLayout.rightMinWidth;
  }
  $(WikiLayout.leftArea).width(leftWidth + "px");
  $(WikiLayout.rightArea).width(rightWidth + "px");
};

WikiLayout.prototype.clear = function() {
  var portlet = document.getElementById(eXo.wiki.WikiLayout.portletId);
  var wikiMiddleArea = $(portlet).find('div.uiWikiMiddleArea')[0];
  $(wikiMiddleArea).removeClass("uiWikiPortletNoSelect");
  if(eXo.wiki.WikiLayout.leftArea) {
    eXo.wiki.WikiLayout.setCookie(eXo.wiki.WikiLayout.userName + "_leftWidth", eXo.wiki.WikiLayout.leftArea.offsetWidth, 1);   
    $(document).off('mousemove');
    if (eXo.wiki.WikiLayout.resizeBar) $(eXo.wiki.WikiLayout.resizeBar).removeClass("resizeBarDisplay");
  }
};

WikiLayout.prototype.heightDelta = function() {
  return $(this.portal).height() - document.documentElement.clientHeight;
};

eXo.wiki.WikiLayout = new WikiLayout();
_module.WikiLayout = eXo.wiki.WikiLayout;
