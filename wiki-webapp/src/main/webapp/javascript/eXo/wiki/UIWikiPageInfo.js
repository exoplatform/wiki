function UIWikiPageInfo() {
};

UIWikiPageInfo.prototype.init = function() {
  eXo.wiki.UIRelated.initMacros();
  eXo.wiki.UITreeExplorer.initMacros();
};

eXo.wiki.UIWikiPageInfo = new UIWikiPageInfo();
_module.UIWikiPageInfo = eXo.wiki.UIWikiPageInfo;