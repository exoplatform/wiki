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

function UIWikiEditParagraph() {
};

UIWikiEditParagraph.prototype.init = function(pageContentAreaId, editActionId) {
  var pageContentArea = document.getElementById(pageContentAreaId);
  var editAction = document.getElementById(editActionId);
  if (!pageContentArea) {
    return;
  }
  var sections =  $(pageContentArea).find('span.EditSection');
  for ( var index = 0; index < sections.length; index++) {
    var editLink =  $(sections[index]).find('a')[0];
    var linkLabel =  $(editLink).find('span')[0];
    $(editLink).click((function(sectionIndex) {
      return function() {
        eXo.wiki.UIWikiAjaxRequest.makeNewHash('#EditPage&section=' + sectionIndex);
      };
    })(index + 1));
    $(editLink).attr('href', 'javascript:void(0);');
    $(linkLabel).html('');
    
    var headerContainer = sections[index].parentNode;
    var sectionContainer = $(headerContainer).closest('.section-container')[0];
    $(sectionContainer).mouseover(function(event) {
      eXo.wiki.UIWikiEditParagraph.highlightEditSection(this,event);
    });
    $(sectionContainer).mouseout(function(event) {
      eXo.wiki.UIWikiEditParagraph.highlightEditSection(this,event);
    });
    $(sectionContainer).focus(function(event) {
      eXo.wiki.UIWikiEditParagraph.highlightEditSection(this,event);
    });
    $(sectionContainer).blur(function(event) {
      eXo.wiki.UIWikiEditParagraph.highlightEditSection(this,event);
    });
  }
};

UIWikiEditParagraph.prototype.highlightEditSection = function (container,event) {
  event.stopPropagation();
  var section = $(container).find('span.EditSection')[0];
  $(section).toggle();
  $(container).toggleClass("EditSectionHover");
};

eXo.wiki.UIWikiEditParagraph = new UIWikiEditParagraph();
return eXo.wiki.UIWikiEditParagraph;

})(base, uiForm, webuiExt, $);
