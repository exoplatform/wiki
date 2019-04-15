(function() {

  if (!eXo.wiki)
  eXo.wiki = {};
if (!eXo.wiki.Wysiwyg) {
  eXo.wiki.Wysiwyg = {
    /**
     * Loads the WYSIWYG code on demand.
     */
    load : function()
    {
      window.require(['SHARED/wikiCkeditor', 'SHARED/wikiPageContent'], function(wikiCkeditor) {
        wikiCkeditor.default.createEditor();
      });
    },
  }
};

})();