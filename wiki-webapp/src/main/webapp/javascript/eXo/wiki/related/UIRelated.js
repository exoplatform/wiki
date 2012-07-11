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
        for ( var i = 0; i < data.jsonList.length; i++) {
          var relatedItem = data.jsonList[i];
          gj(relatedBlock).append(gj('<div/>', {
            'class' : 'Page TreeNodeType Node'
            }).append(gj('<div/>')
               .append(gj('<div/>', {
                 'class' : 'NodeLabel'
               }).append( gj('<a/>', {
                  title : relatedItem.title,
                  href : redirectTempl + "&objectId=" + encodeURIComponent(relatedItem.identity),
                  text: relatedItem.title
                  }))
                )
              )
            )
          }
        }
      }
    );
  }
};

eXo.wiki.UIRelated = new UIRelated();