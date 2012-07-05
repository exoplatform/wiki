function UIRelated() {
};

UIRelated.prototype.initMacros = function() {
	var me = eXo.wiki.UIRelated;
	var relatedBlocks = gj(".RelatedMacro");
	var editForm = document.getElementById('UIWikiPageEditForm');
	if (editForm != null) {
		var ifm = gj(editForm).find('iframe.gwt-RichTextArea')[0];
		if (ifm != null) {
		var innerDoc = ifm.contentDocument || ifm.contentWindow.document;
		relatedBlocks = gj.merge(relatedBlocks, gj(innerDoc).find(
				".RelatedMacro"));
		}
	}
	for ( var i = 0; i < relatedBlocks.length; i++) {
		var relatedBlock = relatedBlocks[i];
		var infoElement = gj(relatedBlock).find('input.info')[0];
		var restUrl = infoElement.getAttribute("restUrl");
		var redirectTempl = infoElement.getAttribute("redirectUrl");
    if (gj(relatedBlock).find("div.TreeNodeType").length > 0)
      return;
		gj.ajax({
			async : false,
			url : restUrl,
			type : 'GET',
			data : '',
			success : function(data) {
				var docFrag = me.initRelatedDOM(data.jsonList, redirectTempl);
				relatedBlock.appendChild(docFrag);
			}
		});

	}
};

UIRelated.prototype.initRelatedDOM = function(relatedList, redirectUrl) {
  var docFrag = document.createDocumentFragment();
  for ( var i = 0; i < relatedList.length; i++) {
    var relatedItem = relatedList[i];
    var nodeGroupDiv = document.createElement("div");
    var nodeDiv = document.createElement("div");
    nodeGroupDiv.className = "Page TreeNodeType Node";

    var labelDiv = document.createElement("div");
    labelDiv.className = "NodeLabel";
    var a = document.createElement("a");
    if (redirectUrl && relatedItem.identity != null) {
      var relatedLink = redirectUrl + "&objectId="
          + encodeURIComponent(relatedItem.identity);
      a.href = relatedLink;
    }
    if (relatedItem.title) {
      a.setAttribute("title", relatedItem.title);
      a.appendChild(document.createTextNode(relatedItem.title));
    }
    labelDiv.appendChild(a);
    nodeDiv.appendChild(labelDiv);
    nodeGroupDiv.appendChild(nodeDiv);
    docFrag.appendChild(nodeGroupDiv);
  }
  return docFrag;
};

eXo.wiki.UIRelated = new UIRelated();