/**
 * Copyright (C) 2010 eXo Platform SAS.
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

function UIWikiPageTitleControlArea() {
};

UIWikiPageTitleControlArea.prototype.init = function(componentId, inputId, untitledLabel, isAddMode) {
  var component = document.getElementById(componentId);
  if (!component) {
    return;
  }
  var input = $(component).find('#' + inputId)[0];
  if (input) {
    eXo.wiki.UIWikiPortlet.decorateInput(input, untitledLabel, isAddMode);
  }
};

eXo.wiki.UIWikiPageTitleControlArea = new UIWikiPageTitleControlArea();
return eXo.wiki.UIWikiPageTitleControlArea;

})(base, uiForm, webuiExt, $);
