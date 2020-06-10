/**
 * Copyright (C) 2011 eXo Platform SAS.
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

function UIWikiRichTextArea() {
};

UIWikiRichTextArea.prototype.init = function(hookId, inputURL, syntax, wiki, space, page) {  
  var me = eXo.wiki.UIWikiRichTextArea;
  me.hookId = hookId;
  me.inputURL = inputURL;
  me.syntax = syntax;
  me.wiki = wiki;
  me.space = space;
  me.page = page;
  
  eXo.wiki.Wysiwyg.load();
};

eXo.wiki.UIWikiRichTextArea = new UIWikiRichTextArea();
return eXo.wiki.UIWikiRichTextArea;

})(base, uiForm, webuiExt, $);
