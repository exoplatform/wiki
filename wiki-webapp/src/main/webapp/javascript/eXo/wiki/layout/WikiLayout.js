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
        $(uiWikiPagePreview).css("height", document.documentElement.clientHeight + "px");
      }
    }
  });
}

WikiLayout.prototype.initWikiLayout = function(prtId, _userName) {
  var me = eXo.wiki.WikiLayout;
  if (_userName) {
    me.userName = _userName;
  }
  
  try {
    if(String(typeof me.myBody) == "undefined" || !me.myBody) {
      me.myBody = $("body")[0];
      me.bodyClass = $(me.myBody).attr('class');
      me.myHtml = $("html")[0];
    }
  } catch(e){};

  try{
    if(prtId.length > 0) me.portletId = prtId;
    var isIE = ($.browser.msie != undefined)
    var idPortal = (isIE) ? 'UIWorkingWorkspace' : 'UIPortalApplication';
    me.portal = document.getElementById(idPortal);
    var portlet = document.getElementById(me.portletId);
    me.wikiLayout = $(portlet).find('div.uiWikiMiddleArea')[0];
    me.resizeBar = $(me.wikiLayout).find('div.resizeBar')[0];
    me.colapseLeftContainerButton = $(me.wikiLayout).find('div.resizeButton')[0];
    var showLeftContainer = me.getCookie(me.userName + "_ShowLeftContainer");
	if (showLeftContainer) {
      showLeftContainer = showLeftContainer=='true'?'none':'block';
	} else {
      showLeftContainer = 'none';
	}
    me.verticalLine = $(me.wikiLayout).find('div.VerticalLine')[0];
    if (me.resizeBar) {
      me.leftArea = $(me.resizeBar).prev('div')[0];
      me.rightArea = $(me.resizeBar).next('div')[0];

      var leftWidth = me.getCookie(me.userName + "_leftWidth");
      if (me.leftArea && me.rightArea && (leftWidth != null) && (leftWidth != "") && (leftWidth * 1 > 0)) {
        $(me.leftArea).width(leftWidth + 'px');
      }	  
      $(me.resizeBar).mousedown(me.exeRowSplit);
      $(me.colapseLeftContainerButton).unbind('click');
      $(me.colapseLeftContainerButton).bind('click', me.showHideSideBar);
    }

	if(me.wikiLayout) {
      me.processWithHeight();
      eXo.core.Browser.addOnResizeCallback("WikiLayout", me.processWithHeight);
    }

    if (me.leftArea) {
      me.leftArea.style.display = showLeftContainer;
	  me.showHideSideBar(null, true);
    }
  } catch(e) {
   return;
  };
};

$(window).resize(function() {
  var me = eXo.wiki.WikiLayout;
  eXo.core.Browser.managerResize();
  if(me.currWidth != document.documentElement.clientWidth) {
    me.processWithHeight();
  }
  me.currWidth  = document.documentElement.clientWidth;
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
 */
WikiLayout.prototype.setCookie = function (c_name, value, exdays) {
  var exdate = new Date();
  exdate.setDate(exdate.getDate() + exdays);
  var c_value = escape(value) + ((exdays == null) ? "" : "; expires=" + exdate.toUTCString());
  document.cookie = c_name + "=" + c_value;
}

WikiLayout.prototype.setClassBody = function(clazz) {
  var me = eXo.wiki.WikiLayout;
  if(me.myBody && me.myHtml) {
    if (String(clazz) != me.bodyClass) {
      $(me.myBody).attr('class', clazz + " " + me.bodyClass);
      $(me.myHtml).attr('class', clazz);
    } else {
      $(me.myBody).attr('class', clazz);
      $(me.myHtml).attr('class', '');
    }
  }
};

WikiLayout.prototype.processWithHeight = function(prtId, _userName) {
  var me = eXo.wiki.WikiLayout;
  if (me.wikiLayout) {
    me.setClassBody(me.wikiBodyClass);
    me.setHeightLayOut();
    me.setWidthLayOut();
  } else {
    me.init(prtId, _userName);
  }
};

WikiLayout.prototype.setWidthLayOut = function() {
  var me = eXo.wiki.WikiLayout;
  var maxWith = $(me.wikiLayout).width();
  var lWith = 0;
  if (me.leftArea && me.resizeBar) {
    lWith = me.leftArea.offsetWidth + me.resizeBar.offsetWidth;
  }
  if (me.rightArea) {
    $(me.rightArea).width((maxWith - lWith - 8) + 'px'); //left and right padding
  }
};

WikiLayout.prototype.setHeightLayOut = function() {
  var me = eXo.wiki.WikiLayout;
  var layout = me.wikiLayout;
  var leftNavigationDiv = $('#LeftNavigation')[0];
  var platformAdmintc = $("#PlatformAdminToolbarContainer")[0];
  var hdef = (leftNavigationDiv && platformAdmintc) ? 
		     leftNavigationDiv.clientHeight - layout.offsetTop + platformAdmintc.clientHeight :
		     document.documentElement.clientHeight - layout.offsetTop; 	 
  var hct = hdef * 1;
  $(layout).css('height', hdef + 'px');
  var delta = me.heightDelta();
  var uiRelatedPages = $(me.leftArea).find("div.uiRelatePages:first")[0];
  var uiRelatedPagesHeight = uiRelatedPages?uiRelatedPages.offsetHeight:0;

  if(delta > hdef) {
    me.setClassBody(me.bodyClass);
  }
  hct-=20; //Padding-bottom of wikiLayout

  if (me.leftArea && me.resizeBar) {
    $(me.leftArea).height(hct  + 2 + "px");
	  var resideBarContent = $(me.resizeBar).find("div.resizeBarContent:first")[0];
	  var titleHeader = $(me.leftArea).find(".titleWikiBox:first")[0];
	  var treeExplorer = $(me.leftArea).find("div.uiTreeExplorer:first")[0];
  
    if (treeExplorer) {
      $(treeExplorer).css("height", "");
      if ((treeExplorer.offsetHeight + 37 + uiRelatedPagesHeight + titleHeader.offsetHeight ) < hct) {
        //Padding top/bottom inside tree, margin top of RelatedPages box = 35px
        if (uiRelatedPagesHeight > 0) {
          $(treeExplorer).css("height", hct - titleHeader.offsetHeight - uiRelatedPagesHeight - 25 + "px"); 
        } else {
          $(treeExplorer).css("height", hct - titleHeader.offsetHeight - uiRelatedPagesHeight - 10 + "px"); 
        }
      }
    }

    if (resideBarContent) {
      $(resideBarContent).height(hct + "px");
    }
  } else if (me.verticalLine) {
    $(me.verticalLine).height(hct + "px");
  }

  if (me.rightArea) {
    $(me.rightArea).height(hct + "px");
  }
  me.setHeightRightContent();
};

WikiLayout.prototype.setHeightRightContent = function(prtId, _userName) {
  var me = eXo.wiki.WikiLayout;
  if (!me.wikiLayout) {
    me.init(prtId, _userName);
  }
  
  var pageArea = $(me.rightArea).find('div.UIWikiPageArea:first')[0];
  if (pageArea) {
    var bottomArea = $(me.rightArea).find('div.uiWikiBottomArea:first')[0];
    var pageContainer = $(me.rightArea).find('div.UIWikiPageContainer:first')[0];
    var bottomHeight = 0;
    if (bottomArea) {
      bottomHeight = bottomArea.offsetHeight;
      if ($(bottomArea).children().size() <= 0) { //initial padding-top 15px
        $(bottomArea).css("display", "none");
        bottomHeight = 0;
      }
    }      
      
    if (me.leftArea) {
      var pageContent = $(pageArea).find("div.uiWikiPageContentArea:first")[0];
      if (me.leftArea.offsetHeight > 0) {
        $(pageContent).css("height", "");
        var pageAreaHeight = (me.leftArea.offsetHeight - bottomHeight);
        var poffsetHeight = pageContent.offsetHeight ? pageContent.offsetHeight : 0;
        if (poffsetHeight + bottomHeight < me.leftArea.offsetHeight) {
          $(pageContent).height(pageAreaHeight - 9 + "px");
        }
        $(me.rightArea).height(me.leftArea.offsetHeight + 1 + "px");
      }
    }
    
    if (me.rightArea) {
      me.checkToShowGradientScrollInRightArea();
      $(me.rightArea).scroll(function() {
        me.checkToShowGradientScrollInRightArea();
      });
    }
    
    if (me.leftArea) {
      me.checkToShowGradientScrollInLeftArea();
      $(me.leftArea).scroll(function() {
        me.checkToShowGradientScrollInLeftArea();
      });
    }
  }
  
  var settingContainer = $(me.wikiLayout).find('div.UIWikiPageSettingContainer:first')[0];
  if (settingContainer) {
    var hdef = document.documentElement.clientHeight - me.wikiLayout.offsetTop;
    hdef -= 35; //Padding-bottom of wikiLayout
    
    var tabs = $(settingContainer).find("ul.nav-tabs:first")[0];
    var contents = $(settingContainer).find("div.tab-content:first")[0];
    if (contents && tabs) {
      $(contents).height(hdef - tabs.offsetHeight + "px");
    }
  }
};

WikiLayout.prototype.checkToShowGradientScrollInLeftArea = function() {
  var me = eXo.wiki.WikiLayout;
  if (!me.leftArea) {
    return;
  }
  
  var scrollTop = $(me.leftArea).find('.uiScrollTop')[0];
  var scrollBottom = $(me.leftArea).find('.uiScrollBottom')[0];
  
  if (!scrollTop || !scrollBottom) {
    return;
  }
  
  var uiTreeExplorer = $(me.leftArea).find("div.uiTreeExplorer:first")[0];
  var relatedPage = $(me.leftArea).find("div.uiRelatePages:first")[0];
  var relatedPageHeight = 0;
  if (relatedPage) {
    relatedPageHeight = relatedPage.offsetHeight;
  }
  
  if (uiTreeExplorer) {
    var isShowGradientScroll = uiTreeExplorer.offsetHeight + relatedPageHeight + 37 > me.leftArea.offsetHeight;
    if (isShowGradientScroll) {
      if (me.leftArea.scrollTop > 0) {
        $(scrollTop).css("display", "block");
        $(scrollTop).css("width", me.leftArea.offsetWidth + "px");
      } else {
        $(scrollTop).css("display", "none");
      }
    
      if (me.leftArea.scrollTop < uiTreeExplorer.offsetHeight + relatedPageHeight + 37 - me.leftArea.offsetHeight) {
        $(scrollBottom).css("display", "block");
        $(scrollBottom).css("top", (me.leftArea.offsetTop + me.leftArea.offsetHeight - scrollBottom.offsetHeight) + "px");
        $(scrollBottom).css("width", me.leftArea.offsetWidth + "px");
      } else {
        $(scrollBottom).css("display", "none");
      }
    } else {
      $(scrollTop).css("display", "none");
      $(scrollBottom).css("display", "none");
    }
  }
}

WikiLayout.prototype.checkToShowGradientScrollInRightArea = function() {
  var me = eXo.wiki.WikiLayout;
  if (!me.rightArea) {
    return;
  }
  
  var scrollTop = $(me.rightArea).find('.uiScrollTop')[0];
  var scrollBottom = $(me.rightArea).find('.uiScrollBottom')[0];
  
  if (!scrollTop || !scrollBottom) {
    return;
  }
  
  var pageArea = $(me.rightArea).find('div.UIWikiPageArea:first')[0];
  if (pageArea) {
    var pageContent = $(pageArea).find("div.uiWikiPageContentArea:first")[0];
    if (!pageContent) {
      return;
    }
    
    var isShowGradientScroll = pageContent.offsetHeight > me.rightArea.offsetHeight;
    if (isShowGradientScroll) {
      if (me.rightArea.scrollTop > 0) {
        $(scrollTop).css("display", "block");
      } else {
        $(scrollTop).css("display", "none");
      }
    
      if (me.rightArea.scrollTop < me.rightArea.offsetHeight - 10) {
        $(scrollBottom).css("display", "block");
        $(scrollBottom).css("top", (me.rightArea.offsetTop + me.rightArea.offsetHeight - scrollBottom.offsetHeight) + "px");
      } else {
        $(scrollBottom).css("display", "none");
      }
    } else {
      $(scrollTop).css("display", "none");
      $(scrollBottom).css("display", "none");
    }
  }
};

/**
 * Function      showHideSideBar
 * @purpose      Switch the visible of the leftcontainer
 */
WikiLayout.prototype.showHideSideBar = function (e, savedValue) {
  var me = eXo.wiki.WikiLayout;
  var portlet = document.getElementById(me.portletId);
  var wikiMiddleArea = $(portlet).find('div.uiWikiMiddleArea')[0];
  var allowedWidth = wikiMiddleArea.offsetWidth - 50; //substract the left and right padding 
  var newValue = me.leftArea.style.display;
  if (me.resizeBar && me.leftArea) {
    if (newValue == 'none') {
      me.leftArea.style.display = 'block';
      $(me.colapseLeftContainerButton).removeClass("showLeftContent");
      $(me.resizeBar).removeClass("resizeNoneBorder");
	  $(wikiMiddleArea).removeClass("nonePaddingLeft");	  
      if (allowedWidth - me.resizeBar.offsetWidth - me.leftArea.offsetWidth < me.rightMinWidth) {
        me.leftArea.style.width = allowedWidth - me.resizeBar.offsetWidth - me.rightMinWidth + "px";
        me.setCookie(me.userName + "_leftWidth", me.leftArea.offsetWidth, 20);
      }
      $(me.rightArea).width(allowedWidth - me.resizeBar.offsetWidth - me.leftArea.offsetWidth + "px");
	  var iElement = $(me.colapseLeftContainerButton).find("i:first")[0];
	  iElement.className = "uiIconMiniArrowLeft";
    } else {
      me.leftArea.style.display = 'none';
      $(me.colapseLeftContainerButton).addClass("showLeftContent");
	  var iElement = $(me.colapseLeftContainerButton).find("i:first")[0];
	  iElement.className = "uiIconMiniArrowRight";
      $(me.resizeBar).addClass("resizeNoneBorder");
	  $(wikiMiddleArea).addClass("nonePaddingLeft");
      $(me.rightArea).width(allowedWidth - me.resizeBar.offsetWidth + 25 + "px"); //right padding, leftpadding is removed
    }
    if (!savedValue) {
      me.setCookie(me.userName + "_ShowLeftContainer", me.leftArea.style.display == 'block'?'true':'false', 20);
    }
  } else {
    if (me.resizeBar) {
      $(me.rightArea).width(wikiMiddleArea.offsetWidth - me.resizeBar.offsetWidth + "px");
    }else {
	  $(me.rightArea).css("width", "100%");
	}
  }
  me.processWithHeight();
}
WikiLayout.prototype.exeRowSplit = function(e) {
  _e = (window.event) ? window.event : e;
  var me = eXo.wiki.WikiLayout;
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
  if (me.resizeBar) {
    $(me.resizeBar).addClass("resizeBarDisplay");
  }    
};

WikiLayout.prototype.adjustWidth      = function(evt) {
  evt = (window.event) ? window.event : evt;
  var me = eXo.wiki.WikiLayout;
  var portlet = document.getElementById(me.portletId);
  var wikiMiddleArea = $(portlet).find('div.uiWikiMiddleArea')[0];
  var allowedWidth = wikiMiddleArea.offsetWidth - 50; //Substract the padding
  var delta = evt.clientX - me.posX;
  var leftWidth = (me.leftX + delta);
  var rightWidth = (allowedWidth - leftWidth - me.resizeBar.offsetWidth); //Padding of wikiLayout and PageArea
  if (leftWidth < me.leftMinWidth){
  	leftWidth = me.leftMinWidth;
  	rightWidth = allowedWidth - leftWidth - WikiLayout.resizeBar.offsetWidth;
  }
  if (rightWidth < me.rightMinWidth) {
  	leftWidth = allowedWidth - me.rightMinWidth -me.resizeBar.offsetWidth;
  	rightWidth = me.rightMinWidth;
  }
  $(me.leftArea).width(leftWidth + "px");
  $(me.rightArea).width(rightWidth + "px");
  me.processWithHeight();
};

WikiLayout.prototype.clear = function() {
  var me = eXo.wiki.WikiLayout;
  var portlet = document.getElementById(me.portletId);
  var wikiMiddleArea = $(portlet).find('div.uiWikiMiddleArea')[0];
  $(wikiMiddleArea).removeClass("uiWikiPortletNoSelect");
  if(me.leftArea) {
    me.setCookie(me.userName + "_leftWidth", me.leftArea.offsetWidth, 1);   
    $(document).off('mousemove');
    if (me.resizeBar) {
      $(me.resizeBar).removeClass("resizeBarDisplay");
    }
  }
};

WikiLayout.prototype.heightDelta = function() {
  var me = eXo.wiki.WikiLayout;
  return $(me.portal).height() - document.documentElement.clientHeight;
};

eXo.wiki.WikiLayout = new WikiLayout();
return eXo.wiki.WikiLayout;

})(base, uiForm, webuiExt, $);