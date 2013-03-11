(function(base, uiForm, webuiExt, $) {

if (!eXo.wiki)
  eXo.wiki = {};

function UIRelated() {
};

UIRelated.prototype.initMacros = function() {
  var me = eXo.wiki.UIRelated;
  var relatedBlocks = $(".RelatedMacro");
  var editForm = document.getElementById('UIWikiPageEditForm');
  if (editForm != null) {
    var ifm = $(editForm).find('iframe.gwt-RichTextArea')[0];
    if (ifm != null) {
      var innerDoc = ifm.contentDocument || ifm.contentWindow.document;
      relatedBlocks = $.merge(relatedBlocks, $(innerDoc).find(".RelatedMacro"));
    }
  }
  
  for ( var i = 0; i < relatedBlocks.length; i++) {
    var relatedBlock = relatedBlocks[i];
    var infoElement = $(relatedBlock).find('input.info')[0];
    
    if (!infoElement) {
      continue;
    }
    
    var restUrl = infoElement.getAttribute("restUrl");
    var redirectTempl = infoElement.getAttribute("redirectUrl");
    if ($(relatedBlock).find("div.TreeNodeType").length > 0) {
      return;
    }
      
    $.ajax({
      async : false,
      url : restUrl,
      type : 'GET',
      data : '',
      success : function(data) {
        $(relatedBlock).empty();
        for ( var i = 0; i < data.jsonList.length; i++) {
          var relatedItem = data.jsonList[i];
          var href = redirectTempl + "&objectId=" + encodeURIComponent(relatedItem.identity);
          $(relatedBlock).append("<a href='" + href + "' class='relateItem'><i class='uiIconFile'></i>" + relatedItem.title + "</a>");
        }
        
        // Recalculate width height
        if (eXo.wiki.WikiLayout) {
	      eXo.wiki.WikiLayout.processWithHeight();
	    }
      }
    });
  }
};

eXo.wiki.UIRelated = new UIRelated();
return eXo.wiki.UIRelated;

})(base, uiForm, webuiExt, $);
