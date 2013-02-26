(function(base, uiForm, webuiExt, $) {

if(eXo.wiki.UITreeExplorer ==  null) {
  eXo.wiki.UITreeExplorer = {};
};

function UITreeExplorer() {};

UITreeExplorer.prototype.init = function(componentid, initParam, isFullRender, isRenderLink, baseLink, retrictedLabel, restrictedTitle) {
	var me = eXo.wiki.UITreeExplorer;
  var component = document.getElementById(componentid);
  this.isRenderLink = isRenderLink;
  this.baseLink = baseLink;
  me.retrictedLabel = retrictedLabel;
  me.restrictedTitle = restrictedTitle;
  var initNode = $(component).find('input')[0];
  initParam = me.cleanParam(initParam);
  me.render(initParam, initNode, isFullRender);
};

UITreeExplorer.prototype.initMacros = function() {
  var me = eXo.wiki.UITreeExplorer;
  var pageTreeBlocks = $(".PageTreeMacro");
  var editForm = document.getElementById('UIWikiPageEditForm');
  if (editForm != null) {
    var ifm = $(editForm).find('iframe.gwt-RichTextArea')[0];
    if (ifm != null) {
      me.innerDoc = ifm.contentDocument || ifm.contentWindow.document;
      pageTreeBlocks = $.merge(pageTreeBlocks, $(me.innerDoc).find(".PageTreeMacro"));
    }
  }
  for ( var i = 0; i < pageTreeBlocks.length; i++) {
    var pageTreeBlock = pageTreeBlocks[i];
    var initNode = $(pageTreeBlock).find('input')[0];
    this.baseLink = $(pageTreeBlock).find('input.BaseURL')[0].value;
    var initParam = $(pageTreeBlock).find('input.InitParams')[0].value;
    initParam = me.cleanParam(initParam);
    this.isRenderLink = true;
    if ($(pageTreeBlock).find("ul.nodeGroup").length > 0)
      return;
    me.render(initParam, initNode, false);
  }
};

UITreeExplorer.prototype.collapseExpand = function(element) {
  if(element) {
	  var node = element.parentNode;
	  var subGroup = $(node).find('ul.nodeGroup')[0];
	  if ($(element).hasClass('EmptyIcon'))
	    return true;
	  if (!subGroup) {
	    $(element).addClass('uiIconCollapse');
	    return false;
	  }
	  $(subGroup).toggle();
	  $(element).toggleClass('uiIconExpand','uiIconCollapse');
	  return true;
	}
};

UITreeExplorer.prototype.onNodeClick = function(node, absPath) {
  var me = eXo.wiki.UITreeExplorer;
  var selectableObj = $(node).find('a');
  if (selectableObj.length > 0) {
    var component = $(node).closest(".uiTreeExplorer");
    var selectedNode = $(component).find('div.selected')[0];
    if (selectedNode)
      $(selectedNode).removeClass("selected");
    if (!$(node).hasClass("selected"))
      $(node).addClass("selected");
    me.selectNode(node, absPath);
  }
};

UITreeExplorer.prototype.selectNode = function(node, nodePath) {
  var me = eXo.wiki.UITreeExplorer;
  var component = $(node).closest(".uiTreeExplorer");
  var link = $(component).find('a.SelectNode')[0];

  var endParamIndex = link.href.lastIndexOf("')");
  var param = "&objectId";
  var modeIndex = link.href.indexOf(param);
  if (endParamIndex > 0) {
    if (modeIndex < 0)
      link.href = link.href.substring(0, endParamIndex) + param + "="
          + nodePath + "')";
    else
      link.href = link.href.substring(0, modeIndex) + param + "=" + nodePath
          + "')";
  } else {
    if (modeIndex < 0)
      link.href = link.href.substring(0, link.href.length) + param + "="
          + nodePath;
    else
      link.href = link.href.substring(0, modeIndex) + param + "=" + nodePath;
  }
  window.location = link.href;

};

UITreeExplorer.prototype.render = function(param, element, isFullRender) {
  var me = eXo.wiki.UITreeExplorer;
  if(element) {
	  var node = element.parentNode;
	  var component = $(node).closest(".uiTreeExplorer");
	  var url = $(component).find('input.ChildrenURL')[0].value;
	  if (isFullRender) {
	    url = $(component).find('input.InitURL')[0].value;
	  }
	  var restURL = url + param;
	
	  var childBlock = document.createElement("ul");
	  if (me.innerDoc) {
	    childBlock = me.innerDoc.createElement("ul");
	    me.innerDoc = null;
	  }
	  $(childBlock).addClass('nodeGroup');
	  $(childBlock).html(me.loading);
	  $(node).append(childBlock);
	  $.ajax({
	    async : false,
	    url : restURL,
	    type : 'GET',
	    data : '',
	    success : function(data) {
	      me.renderTreeNodes(childBlock, data);
	    }
	  });
	}
};

UITreeExplorer.prototype.renderTreeNodes = function(node, dataList) {
  var me = eXo.wiki.UITreeExplorer;
  var resultLength = dataList.jsonList.length;

  var str = "";
  for ( var i = 0; i < resultLength; i++) {
    str += me.buildNode(dataList.jsonList[i]);
  }
  $(node).html(str);
  if (this.isRenderLink) {
	var wikiHome = $(node).find("div.wikihome");
	if (wikiHome) {
	  var aElement = $(wikiHome).find("a:first")[0];
	  var homeUL = $(wikiHome).parents("ul.nodeGroup:first")[0];
	  var remain = $(homeUL).find("ul.nodeGroup:first")[0];
	  var container = $(homeUL).parents("div.uiTreeExplorer")[0];
	  var h5Container = $(container).prev();	  
	  $(h5Container).append(aElement);
	  if (homeUL) {
	    homeUL.remove();
	  }
	  $(container).append(remain);
	  if (eXo.wiki.WikiLayout) {
	    eXo.wiki.WikiLayout.processWithHeight();
	  }
	}
  }
  $("*[rel='tooltip']").tooltip();
}

UITreeExplorer.prototype.buildHierachyNode = function(data){
  var me = eXo.wiki.UITreeExplorer; 
  var children = data.children; 
  var childBlock = "<ul class=\"nodeGroup\">";
  for ( var i = 0; i < children.length; i++) {   
    childBlock += me.buildNode(children[i]);
  }
  childBlock += "</ul>";
  return childBlock
}


UITreeExplorer.prototype.buildNode = function(data) {
  var me = eXo.wiki.UITreeExplorer;   
  var nodeName = data.name; 
  // Change Type for CSS
  var nodeType = data.nodeType;
  var nodeTypeCSS = nodeType.toLowerCase();
  var iconType = (data.expanded ==true)? "uiIconCollapse":"uiIconExpand" ;
  var lastNodeClass = "";
  var hoverClass = "node";
  var excerptData = data.excerpt;
  var Re = new RegExp("\\/","g");
  var path = data.path.replace(Re, ".");
  var param = "?path=" + path;
  if (excerptData!=null) {
    param += "&excerpt=true";
  }
  if (data.extendParam)
    param += "&current=" + data.extendParam.replace(Re, ".");
  
  if (data.lastNode == true) {
    lastNodeClass = "lastNode node";
  }
  if (data.hasChild == false) {
    iconType = "uiIconEmpty";
  }
  if (data.selected == true){
    hoverClass = "selected";
  }
  var childNode = "";
  childNode += " <li  class=\"" + lastNodeClass + "\" >";
  childNode += "   <div class=\""+iconType+"\" id=\"" + path + "\" onclick=\"event.cancelBubble=true;  if(eXo.wiki.UITreeExplorer.collapseExpand(this)) return;  eXo.wiki.UITreeExplorer.render('"+ param + "', this)\">";
  if (me.isRenderLink) {
    if (data.retricted == true) {
      childNode += "    <div id=\"iconTreeExplorer\" onclick=\"event.cancelBubble=true\" class=\"uiIconWikiRestrictedFile treeNodeType node "+ hoverClass +" \">";
    } else {
      childNode += "    <div id=\"iconTreeExplorer\" onclick=\"event.cancelBubble=true\" class=\""+ nodeTypeCSS +" treeNodeType node "+ hoverClass +" \">";
    }
  } else {
    if (data.retricted == true) {
      childNode += "    <div id=\"iconTreeExplorer\"  onclick=\"event.cancelBubble=true; eXo.wiki.UITreeExplorer.onNodeClick(this,'"+path+"', false " + ")\""  + "class=\"uiIconWikiRestrictedFile treeNodeType node "+ hoverClass +" \">";
    } else {
      childNode += "    <div id=\"iconTreeExplorer\"  onclick=\"event.cancelBubble=true; eXo.wiki.UITreeExplorer.onNodeClick(this,'"+path+"', false " + ")\""  + "class=\""+ nodeTypeCSS +" treeNodeType node "+ hoverClass +" \">";
    }    
  }  
  
  if (data.selectable == true && data.retricted == false) {
    if (me.isRenderLink) {
      var index = path.lastIndexOf("%2F"); // Find the index of character "/"
      var pageId = path.substring(index + 3);
      var link = me.baseLink + pageId;
      childNode += "        <a href=\"" + link + "\">" + nodeName + "</a>";
    } else {
      childNode += "        <a>" + nodeName + "</a>";
    }
  } else {
    if (data.retricted == true) {
      nodeName = me.retrictedLabel;
      childNode += "         <span style=\"cursor:auto\" title=\"" + me.restrictedTitle + "\"><em>" + nodeName + "</em></span>";
    } else if (data.selectable == false) {
      childNode += "         <span style=\"cursor:auto\" title=\"" + nodeName + "\">" + nodeName + "</span>";
    }
  }
  
  if (excerptData != null) {
    childNode += excerptData;
  }
  childNode += "    </div>";
  childNode += "  </div>";
  if (data.children.length > 0) {
    childNode += me.buildHierachyNode(data);
  }
  childNode += " </li>"; 
  return childNode;
}

UITreeExplorer.prototype.cleanParam = function(data){
  return data.replace(/&amp;/g, "&");
}

eXo.wiki.UITreeExplorer = new UITreeExplorer();
return eXo.wiki.UITreeExplorer;

})(base, uiForm, webuiExt, $);
