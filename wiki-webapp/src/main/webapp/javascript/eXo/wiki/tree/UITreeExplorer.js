
if(eXo.wiki.UITreeExplorer ==  null) {
  eXo.wiki.UITreeExplorer = {};
};

function UITreeExplorer() {};

UITreeExplorer.prototype.init = function(componentid, initParam, isFullRender, isRenderLink, baseLink) {
  var me = eXo.wiki.UITreeExplorer;
  var component = document.getElementById(componentid);
  this.isRenderLink = isRenderLink;
  this.baseLink = baseLink;
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
    if ($(pageTreeBlock).find("div.NodeGroup").length > 0)
      return;
    me.render(initParam, initNode, false);
  }
};

UITreeExplorer.prototype.collapseExpand = function(element) {
  var node = element.parentNode;
  var subGroup = $(node).find('div.NodeGroup')[0];
  if ($(element).hasClass('EmptyIcon'))
    return true;
  if (!subGroup) {
    $(element).addClass('CollapseIcon');
    return false;
  }
  $(subGroup).toggle();
  $(element).toggleClass('ExpandIcon','CollapseIcon');
  return true;
};

UITreeExplorer.prototype.onNodeClick = function(node, absPath) {
  var me = eXo.wiki.UITreeExplorer;
  var selectableObj = $(node).find('a');
  if (selectableObj.length > 0) {
    var component = $(node).closest(".UITreeExplorer");
    var selectedNode = $(component).find('div.Hover')[0];
    if (selectedNode)
      $(selectedNode).removeClass("Hover");
    if (!$(node).hasClass("Hover"))
      $(node).addClass("Hover");
    me.selectNode(node, absPath);
  }
};

UITreeExplorer.prototype.selectNode = function(node, nodePath) {
  var me = eXo.wiki.UITreeExplorer;
  var component = $(node).closest(".UITreeExplorer");
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
  var node = element.parentNode;
  var component = $(node).closest(".UITreeExplorer");
  var url = $(component).find('input.ChildrenURL')[0].value;
  if (isFullRender) {
    url = $(component).find('input.InitURL')[0].value;
  }
  var restURL = url + param;

  var childBlock = document.createElement("div");
  if (me.innerDoc) {
    childBlock = me.innerDoc.createElement("div");
    me.innerDoc = null;
  }
  $(childBlock).addClass('NodeGroup');
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
};

UITreeExplorer.prototype.renderTreeNodes = function(node, dataList) {
  var me = eXo.wiki.UITreeExplorer;
  var resultLength = dataList.jsonList.length;

  var str = "";
  for ( var i = 0; i < resultLength; i++) {
    str += me.buildNode(dataList.jsonList[i]);
  }
  $(node).html(str);
}

UITreeExplorer.prototype.buildHierachyNode = function(data){
  var me = eXo.wiki.UITreeExplorer; 
  var children = data.children; 
  var childBlock = "<div class=\"NodeGroup\">";
  for ( var i = 0; i < children.length; i++) {   
    childBlock += me.buildNode(children[i]);
  }
  childBlock += "</div>";
  return childBlock
}


UITreeExplorer.prototype.buildNode = function(data) {
  var me = eXo.wiki.UITreeExplorer;   
  var nodeName = data.name; 
  // Change Type for CSS
  var nodeType = data.nodeType;
  var nodeTypeCSS = nodeType.substring(0, 1).toUpperCase()
      + nodeType.substring(1).toLowerCase();
  var iconType = (data.expanded ==true)? "Collapse":"Expand" ;
  var lastNodeClass = "";
  var hoverClass = "";
  var excerptData = data.excerpt;
  var path = data.path.replaceAll("/", ".");
  var param = "?path=" + path;
  if (excerptData!=null) {
    param += "&excerpt=true";
  }
  if (data.extendParam)
    param += "&current=" + data.extendParam.replaceAll("/",".");
  if (data.lastNode == true) {
    lastNodeClass = "LastNode";
  }
  if (data.hasChild == false) {
    iconType = "Empty";
  }
  if (data.selected == true){
    hoverClass = "Hover";
  }
  var childNode = "";
  childNode += " <div  class=\"" + lastNodeClass + " Node\" >";
  childNode += "   <div class=\""+iconType+"Icon\" id=\"" + path + "\" onclick=\"event.cancelBubble=true;  if(eXo.wiki.UITreeExplorer.collapseExpand(this)) return;  eXo.wiki.UITreeExplorer.render('"+ param + "', this)\">";
  if (me.isRenderLink) {
    childNode += "    <div id=\"iconTreeExplorer\" onclick=\"event.cancelBubble=true\" class=\""+ nodeTypeCSS +" TreeNodeType Node "+ hoverClass +" \">";
  } else {
    childNode += "    <div id=\"iconTreeExplorer\"  onclick=\"event.cancelBubble=true; eXo.wiki.UITreeExplorer.onNodeClick(this,'"+path+"', false " + ")\""  + "class=\""+ nodeTypeCSS +" TreeNodeType Node "+ hoverClass +" \">";
  }  
  childNode += "      <div class='NodeLabel'>";
  
  if (data.selectable == true) {
    if (me.isRenderLink) {
      var index = path.lastIndexOf("%2F"); // Find the index of character "/"
      var pageId = path.substring(index + 3);
      var link = me.baseLink + pageId;
      childNode += "        <a title=\"" + nodeName + "\" href=\"" + link + "\">" + nodeName + "</a>";
    } else {
      childNode += "        <a title=\"" + nodeName + "\">" + nodeName + "</a>";
    }
  }
  else{
    childNode += "         <span style=\"cursor:auto\" title=\""+nodeName+"\">"+nodeName+"</span>";
  }
  if (excerptData != null) {
    childNode += excerptData;
  }
  childNode += "      </div>";
  childNode += "    </div>";
  childNode += "  </div>";
  if (data.children.length > 0) {
    childNode += me.buildHierachyNode(data);
  }
  childNode += " </div>"; 
  return childNode;
}


UITreeExplorer.prototype.cleanParam = function(data){
  return data.replace(/&amp;/g, "&");
}

eXo.wiki.UITreeExplorer = new UITreeExplorer();
_module.UITreeExplorer = eXo.wiki.UITreeExplorer;
