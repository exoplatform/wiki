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

function UIDropdownMenu(){
};

UIDropdownMenu.prototype.init = function(componentid){
  var component = document.getElementById(String(componentid));
  gj(component).mouseover(eXo.wiki.UIDropdownMenu.hover);
  gj(component).mouseout(eXo.wiki.UIDropdownMenu.hover);
  gj(component).focus(eXo.wiki.UIDropdownMenu.hover);
  gj(component).blur(eXo.wiki.UIDropdownMenu.hover);
};

UIDropdownMenu.prototype.hover = function(event){
	var ev = window.event || event ;
  var evType = String(ev.type);
  var menu = gj(this).find('div.HoverMenu')[0];
  if (evType == "mouseover" || evType == "onfocus"){
    gj(menu).show();
  } else{
    gj(menu).hide();
  }  
};

eXo.wiki.UIDropdownMenu = new UIDropdownMenu();