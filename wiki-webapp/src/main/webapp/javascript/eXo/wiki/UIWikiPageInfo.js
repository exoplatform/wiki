(function(base, uiForm, webuiExt, $) {
function UIWikiPageInfo() {
};

UIWikiPageInfo.prototype.init = function() {
  eXo.wiki.UIRelated.initMacros();
  eXo.wiki.UITreeExplorer.initMacros();
};

eXo.wiki.UIWikiPageInfo = new UIWikiPageInfo();
return eXo.wiki.UIWikiPageInfo;

})(base, uiForm, webuiExt, $);
