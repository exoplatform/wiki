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

function UIWikiEditParagraph() {
};

UIWikiEditParagraph.prototype.init = function(pageContentAreaId, editActionId) {
  var pageContentArea = document.getElementById(pageContentAreaId);
  var editAction = document.getElementById(editActionId);
  var sections =  gj(pageContentArea).find('span.EditSection');
  for ( var index = 0; index < sections.length; index++) {
    var editLink =  gj(sections[index]).find('a')[0];
    var linkLabel =  gj(editLink).find('span')[0];
    gj(editLink).click((function(sectionIndex) {
      return function() {
        eXo.wiki.UIWikiAjaxRequest.makeNewHash('#EditPage&section=' + sectionIndex);
      };
    })(index + 1));
    gj(editLink).attr('href', 'javascript:void(0);');
    gj(linkLabel).html('');
    
    var headerContainer = sections[index].parentNode;
    var sectionContainer = gj(headerContainer).closest('.section-container')[0];
    gj(sectionContainer).mouseover(function(event) {
      eXo.wiki.UIWikiEditParagraph.highlightEditSection(this,event);
    });
    gj(sectionContainer).mouseout(function(event) {
      eXo.wiki.UIWikiEditParagraph.highlightEditSection(this,event);
    });
    gj(sectionContainer).focus(function(event) {
      eXo.wiki.UIWikiEditParagraph.highlightEditSection(this,event);
    });
    gj(sectionContainer).blur(function(event) {
      eXo.wiki.UIWikiEditParagraph.highlightEditSection(this,event);
    });
  }
};

UIWikiEditParagraph.prototype.highlightEditSection = function (container,event) {
  if (gj.browser.msie) {
    event.cancelBubble = true;
  } else {
    event.stopPropagation();
  }
  var section = gj(container).find('span.EditSection')[0];
  gj(section).toggle();
  gj(container).toggleClass("EditSectionHover");
};

eXo.wiki.UIWikiEditParagraph = new UIWikiEditParagraph();