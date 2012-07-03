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
  this.portletId = 'UIWikiPortlet';
  this.wikiBodyClass = 'wiki-body';
  this.bodyClass = '';
  this.myBody;
  this.myHtml;
  this.min_height = 300;
  this.currWidth = 0;
  this.bottomPadding = 50;
};

window.onresize = function() {
  eXo.core.Browser.managerResize();
  if(this.currWidth != document.documentElement.clientWidth) {
    eXo.wiki.WikiLayout.processeWithHeight();
  }
  this.currWidth  = document.documentElement.clientWidth;
};

WikiLayout.prototype.init = function(prtId) {
  try {
    if(String(typeof this.myBody) == "undefined" || !this.myBody) {
      this.myBody = document.getElementsByTagName("body")[0];
      this.bodyClass = String(this.myBody.className+'');
      this.myHtml = document.getElementsByTagName("html")[0];
    }
  }catch(e){};
  
  try{
    if(prtId.length > 0) this.portletId = prtId;
    var isIE = (gj.browser.msie != undefined)
    var idPortal = (isIE) ? 'UIWorkingWorkspace' : 'UIPortalApplication';
    this.portal = document.getElementById(idPortal);
    var portlet = document.getElementById(this.portletId);
    var wikiLayout = gj(portlet).find('div.UIWikiMiddleArea')[0];
    this.layoutContainer = gj(wikiLayout).find('div.WikiLayout')[0];
    this.spliter = gj(this.layoutContainer).find('div.Spliter')[0];
    this.verticalLine = gj(this.layoutContainer).find('div.VerticalLine')[0];
    if (this.spliter) {
      this.leftArea = gj(this.spliter).prev('div')[0];
      this.rightArea = gj(this.spliter).next('div')[0];
      var leftWidth = eXo.core.Browser.getCookie("leftWidth");
      if (this.leftArea && this.rightArea && (leftWidth != null) && (leftWidth != "") && (leftWidth * 1 > 0)) {
        this.leftArea.style.width = leftWidth + 'px';
      }
      this.spliter.onmousedown = eXo.wiki.WikiLayout.exeRowSplit;
    }
    if(this.layoutContainer) {
      this.processeWithHeight();
      eXo.core.Browser.addOnResizeCallback("WikiLayout", eXo.wiki.WikiLayout.processeWithHeight);
    }
  }catch(e){
   return;
  };
};

WikiLayout.prototype.setClassBody = function(clazz) {
  if(this.myBody && this.myHtml) {
    if(String(clazz) != this.bodyClass) {
      this.myBody.className = (clazz + " " + this.bodyClass);
      this.myHtml.className = clazz;
    } else {
      this.myBody.className = clazz;
      this.myHtml.className = "";
    }
  }
};

WikiLayout.prototype.processeWithHeight = function() {
  var WikiLayout = eXo.wiki.WikiLayout;
  if (WikiLayout.layoutContainer) {
    WikiLayout.setClassBody(WikiLayout.wikiBodyClass);
    WikiLayout.setHeightLayOut();
    WikiLayout.setWithLayOut();
  } else {
    WikiLayout.init('');
  }
};

WikiLayout.prototype.setWithLayOut = function() {
  var WikiLayout = eXo.wiki.WikiLayout;
  var maxWith = gj(WikiLayout.layoutContainer).width();
  var lWith = 0;
  if (WikiLayout.leftArea && WikiLayout.spliter) {
    lWith = WikiLayout.leftArea.offsetWidth + WikiLayout.spliter.offsetWidth;
  }
  if (WikiLayout.rightArea) {
    WikiLayout.rightArea.style.width = (maxWith - lWith) + 'px';
  }
};

WikiLayout.prototype.setHeightLayOut = function() {
  var WikiLayout = eXo.wiki.WikiLayout;
  var layout = eXo.wiki.WikiLayout.layoutContainer;
  var hdef = document.documentElement.clientHeight - layout.offsetTop;
  var hct = hdef * 1;
  gj(layout).css('height', hdef + 'px');
  var delta = WikiLayout.heightDelta();
  if(delta > hdef) {
    WikiLayout.setClassBody(WikiLayout.bodyClass);
  }
  while ((delta = WikiLayout.heightDelta()) > 0 && hdef > delta) {
    hct = hdef - delta;
    gj(layout).css('height', hct + "px");
    hdef = hdef - 2;
  }
  
  if (WikiLayout.leftArea && WikiLayout.spliter) {
    WikiLayout.leftArea.style.height = hct + "px";
    WikiLayout.spliter.style.height = hct + "px";
  } else if (WikiLayout.verticalLine) {
    WikiLayout.verticalLine.style.height = hct + "px";
  }
  
  if (WikiLayout.rightArea) {
    WikiLayout.rightArea.style.height = hct + "px";
  }
  
  WikiLayout.setHeightRightContent();
};

WikiLayout.prototype.setHeightRightContent = function() {
  var WikiLayout = eXo.wiki.WikiLayout;
  if(!WikiLayout.layoutContainer) WikiLayout.init('');
  var pageArea =  gj(WikiLayout.rightArea).find('div.UIWikiPageArea')[0];
  if(pageArea) {
    var bottomArea = gj(WikiLayout.rightArea).find('div.UIWikiBottomArea')[0];
    var pageContainer = gj(WikiLayout.rightArea).find('div.UIWikiPageContainer')[0];
    if(bottomArea) {
      var pageAreaHeight = (WikiLayout.rightArea.offsetHeight - bottomArea.offsetHeight - WikiLayout.bottomPadding);
      if ((pageAreaHeight > pageArea.offsetHeight) && (pageAreaHeight > WikiLayout.min_height)) {
        pageArea.style.height = pageAreaHeight + "px";
      } else if (pageArea.offsetHeight < WikiLayout.min_height) {
        pageArea.style.height = WikiLayout.min_height + "px";
      }
    }
  }
};


WikiLayout.prototype.exeRowSplit = function(e) {
  _e = (window.event) ? window.event : e;
  var WikiLayout = eXo.wiki.WikiLayout;
  WikiLayout.posX = _e.clientX;
  WikiLayout.posY = _e.clientY;
  if (WikiLayout.leftArea && WikiLayout.rightArea
      && WikiLayout.leftArea.style.display != "none"
      && WikiLayout.rightArea.style.display != "none") {
    WikiLayout.adjustHorizon();
  }
};

WikiLayout.prototype.adjustHorizon = function() {
  this.leftX = this.leftArea.offsetWidth;
  this.rightX = this.rightArea.offsetWidth;
  document.onmousemove = eXo.wiki.WikiLayout.adjustWidth;
  document.onmouseup = eXo.wiki.WikiLayout.clear;
};

WikiLayout.prototype.adjustWidth = function(evt) {
  evt = (window.event) ? window.event : evt;
  var WikiLayout = eXo.wiki.WikiLayout;
  var delta = evt.clientX - WikiLayout.posX;
  var leftWidth = (WikiLayout.leftX + delta);
  var rightWidth = (WikiLayout.rightX - delta);
  if (rightWidth <= 0 || leftWidth <= 0) {
    return;
  }
  
  WikiLayout.leftArea.style.width = leftWidth + "px";
  WikiLayout.rightArea.style.width = rightWidth + "px";
};

WikiLayout.prototype.clear = function() {
 if(eXo.wiki.WikiLayout.leftArea) {
   eXo.core.Browser.setCookie("leftWidth", eXo.wiki.WikiLayout.leftArea.offsetWidth, 1);
   document.onmousemove = null;
 }
};

WikiLayout.prototype.heightDelta = function() {
  return gj(this.portal).height() - document.documentElement.clientHeight;
};

eXo.wiki.WikiLayout = new WikiLayout();
