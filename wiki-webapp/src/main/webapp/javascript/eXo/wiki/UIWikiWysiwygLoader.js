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
      window.require.config({
        paths: {
          wikiCkeditor: '/wiki/javascript/eXo/wiki/ckeditor/wikiCkeditor.bundle'
        }
      });

      window.require(['wikiCkeditor'], function(wikiCkeditor) {
        wikiCkeditor.default.createEditor();
      });
    },
  }
};

})();