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
 */(function(base, uiForm, webuiExt, $) {

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
  this.mouseDown = false;
  this.leftHeight = 0;
};

WikiLayout.prototype.init = function(prtId, _userName) {
  $(window).ready(function(){
    var me = eXo.wiki.WikiLayout;
    me.initWikiLayout(prtId, _userName);
  });
}

WikiLayout.prototype.initHeightForPreview = function() {
  $(window).ready(function(){
    var me = eXo.wiki.WikiLayout;
    var mask = document.getElementById("UIWikiMaskWorkspace");
    if (mask) {
      var uiWikiPagePreview = $(mask).find("div.uiWikiPagePreview")[0];
      if (uiWikiPagePreview) {
      	h=document.documentElement.clientHeight - $('.wikiPreviewHeader').outerHeight();
		pdt=parseInt($('.uiWikiPagePreview').css('paddingTop'));
		pdb=parseInt($('.uiWikiPagePreview').css('paddingBottom'));
		h=h-pdt-pdb;
        $(uiWikiPagePreview).css("height", h + "px");
      }
    }
  });
}

WikiLayout.prototype.initWikiLayout = function(prtId, _userName) {
  var me = eXo.wiki.WikiLayout;
  if (_userName) {
    me.userName = _userName;
  }

  try{
    if(prtId.length > 0) me.portletId = prtId;
    var isIE = ($.browser.msie != undefined)
    var idPortal = (isIE) ? 'UIWorkingWorkspace' : 'UIPortalApplication';
    me.portal = document.getElementById(idPortal);
    var portlet = document.getElementById(me.portletId);
    me.wikiLayout = $(portlet).find('div.uiWikiMiddleArea')[0];
    me.resizeBar = $(me.wikiLayout).find('div.resizeBar')[0];
    me.colapseLeftContainerButton = $(me.wikiLayout).find('div.resizeButton')[0];
    
    if (me.resizeBar) {
      me.leftArea = $(me.resizeBar).prev('div')[0];
      me.rightArea = $(me.resizeBar).next('div')[0];
  
      $(me.resizeBar).mousedown(me.exeRowSplit);
      $(me.colapseLeftContainerButton).mousedown(function(e){
        e.stopPropagation();
      });
      $(me.colapseLeftContainerButton).unbind('click');
      $(me.colapseLeftContainerButton).bind('click', me.showHideSideBar);
    }

  } catch(e) {
   return;
  };
};

/**
 * @function   setCookie
 * @return     saved cookie with given name
 */
WikiLayout.prototype.setCookie = function (c_name, value, exdays) {
  var exdate = new Date();
  exdate.setDate(exdate.getDate() + exdays);
  var c_value = escape(value) + ((exdays == null) ? "" : "; expires=" + exdate.toUTCString());
  document.cookie = c_name + "=" + c_value;
}

/**
 * Switch the visible of the leftcontainer
 *
 * Function      showHideSideBar
 */
WikiLayout.prototype.showHideSideBar = function (e, savedValue) {
  var me = eXo.wiki.WikiLayout;
  
  if (me.resizeBar && me.leftArea) {
    var iElement = $(me.colapseLeftContainerButton).find("i:first")[0];
    
    $(me.leftArea).toggleClass('collapsed')
    if ($(me.leftArea).hasClass('collapsed')) {
      iElement.className = "uiIconMiniArrowRight";
    } else {
      iElement.className = "uiIconMiniArrowLeft";
    }
    if (!savedValue) {
      me.setCookie(me.userName + "_ShowLeftContainer", $(me.leftArea).hasClass('collapsed') ? 'false' : 'true', 20);
    }
  }
}
WikiLayout.prototype.exeRowSplit = function(e) {
  _e = (window.event) ? window.event : e;
  var me = eXo.wiki.WikiLayout;
  me.mouseDown = true;
  
  var portlet = document.getElementById(me.portletId);
  var wikiMiddleArea = $(portlet).find('div.uiWikiMiddleArea')[0];
  $(wikiMiddleArea).addClass("uiWikiPortletNoSelect");
  me.posX = _e.clientX;
  me.posY = _e.clientY;
  if (me.leftArea && me.rightArea
      && $(me.leftArea).css('display') != "none"
      && $(me.rightArea).css('display') != "none") {
    me.adjustHorizon();
  }
};

WikiLayout.prototype.adjustHorizon = function() {
  var me = eXo.wiki.WikiLayout;
  if (me.leftArea) {
    me.leftX = me.leftArea.offsetWidth;
  }

  if (me.rightArea) {
    me.rightX = me.rightArea.offsetWidth;
  }
  
  $(document).mousemove(me.adjustWidth);
  $(document).mouseup(me.clear);
};

WikiLayout.prototype.adjustWidth      = function(evt) {
  evt = (window.event) ? window.event : evt;
  var me = eXo.wiki.WikiLayout;
  if (!me.mouseDown) return;
  var portlet = document.getElementById(me.portletId);
  var wikiMiddleArea = $(portlet).find('div.uiWikiMiddleArea')[0];
  var allowedWidth = wikiMiddleArea.offsetWidth - 50; //Substract the padding
  var delta = evt.clientX - me.posX;
  var leftWidth = (me.leftX + delta);
  var rightWidth = (allowedWidth - leftWidth - me.resizeBar.offsetWidth); //Padding of wikiLayout and PageArea
  if (leftWidth < me.leftMinWidth){
  	leftWidth = me.leftMinWidth;
  	rightWidth = allowedWidth - leftWidth - me.resizeBar.offsetWidth;
  }

  if (rightWidth < me.rightMinWidth) {
  	leftWidth = allowedWidth - me.rightMinWidth -me.resizeBar.offsetWidth;
  	rightWidth = me.rightMinWidth;
  }
  if (rightWidth <= me.rightMinWidth + me.resizeBar.offsetWidth + 25) {
	  var pageTitleDiv = $(me.rightArea).find("div.uiWikiPageTitle")[0];
	  if (pageTitleDiv) {
		  $(pageTitleDiv).hide();
	  }
  } else {
  	var pageTitleDiv = $(me.rightArea).find("div.uiWikiPageTitle")[0];
  	if (pageTitleDiv) {
  		$(pageTitleDiv).show();
  	}
  }
  $(me.leftArea).width(leftWidth + "px");
  $(me.rightArea).width(rightWidth + "px");
};

WikiLayout.prototype.clear = function() {
  var me = eXo.wiki.WikiLayout;
  me.mouseDown = false;
  var portlet = document.getElementById(me.portletId);
  var wikiMiddleArea = $(portlet).find('div.uiWikiMiddleArea')[0];
  $(wikiMiddleArea).removeClass("uiWikiPortletNoSelect");
  $(document).off('mousemove mouseup');
  if(me.leftArea) {
    me.setCookie(me.userName + "_leftWidth", me.leftArea.offsetWidth, 1);
  }
};

eXo.wiki.WikiLayout = new WikiLayout();
return eXo.wiki.WikiLayout;

})(base, uiForm, webuiExt, $);
