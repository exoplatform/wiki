(function(base, uiForm, webuiExt, $) {
function UIWikiPageInfo() {
};

UIWikiPageInfo.prototype.init = function() {
  eXo.wiki.UIRelated.initMacros();
  eXo.wiki.UITreeExplorer.initMacros();
};

eXo.wiki.UIWikiPageInfo = new UIWikiPageInfo();

})(base, uiForm, webuiExt, $);
