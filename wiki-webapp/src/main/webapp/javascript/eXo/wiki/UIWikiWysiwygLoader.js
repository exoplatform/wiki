(function() {

  if (!eXo.wiki)
  eXo.wiki = {};
if (!eXo.wiki.Wysiwyg) {
  eXo.wiki.Wysiwyg = {
    /**
     * Indicates the state of the WYSIWYG GWT module. Possible values are: 0 (uninitialized), 1 (loading), 2 (loaded).
     */
    readyState: 0,

    /**
     * The queue of functions to execute after the WYSIWYG module is loaded.
     */
    onModuleLoadQueue: [],

    /**
     * All the WYSIWYG editor instances, mapped to their hookId.
     */
    instances: {},

    /**
     * Loads the WYSIWYG code on demand.
     */
    load : function()
    {
      window.require.config({
        paths: {
          ckeditor: '/wiki/javascript/ckeditor'
        }
      });

      window.require(['ckeditor'], function(ckeditor) {
        ckeditor.create( document.querySelector( '#UIWikiRichTextArea_TextArea' ) )
          .then( editor => { window.editor = editor } );
      });
    },

    /**
     * Schedules a function to be executed after the WYSIWYG module is loaded. A call to this method forces the WYSIWYG
     * module to be loaded, unless the second parameter, {@code lazy}, is set to {@code true}.
     *
     * @param fCode a function
     * @param lazy {@code true} to prevent loading the WYSIWYG module at this point, {@code false} otherwise
     */
    onModuleLoad: function(fCode, lazy) {
        if (typeof fCode != 'function') {
            return;
        }
        switch (this.readyState) {
            // uninitialized
            case 0:
                if (!lazy) {
                    this.load();
                }
                // fall-through

            // loading
            case 1:
                this.onModuleLoadQueue.push(fCode);
                break;

            // loaded
            case 2:
                fCode();
                break;
        }
    },

    /**
     * Executes all the functions scheduled from on module load.
     */
    fireOnModuleLoad: function() {
        // The WYSIWYG module has been loaded successfully.
        this.readyState = 2;

        // Execute all the scheduled functions.
        for (var i = 0; i < this.onModuleLoadQueue.length; i++) {
            this.onModuleLoadQueue[i]();
        }

        // There's no need to schedule functions anymore. They will be execute immediately.
        this.onModuleLoadQueue = undefined;
    },

    /**
     * Try to wrap onScriptLoad in order to be notified when the WYSIWYG script is loaded.
     */
    maybeHookOnScriptLoad: function() {
        if (xwe && xwe.onScriptLoad) {
            var onScriptLoad = xwe.onScriptLoad;
            xwe.onScriptLoad = function() {
                eXo.wiki.Wysiwyg.hookGwtOnLoad();
                onScriptLoad();

                // Restore the default onScriptLoad function.
                if (xwe && xwe.onScriptLoad) {
                    xwe.onScriptLoad = onScriptLoad;
                }
                onScriptLoad = undefined;
            }

            // Prevent further calls to this method.
            this.maybeHookOnScriptLoad = function(){};
        }
    },

    /**
     * Wrap gwtOnLoad in order to be notified when the WYSIWYG module is loaded.
     */
    hookGwtOnLoad: function() {
        var iframe = document.getElementById('xwe');
        if (!iframe) {
          return;
        }

        var gwtOnLoad = iframe.contentWindow.gwtOnLoad;
        iframe.contentWindow.gwtOnLoad = function(errFn, modName, modBase) {
            gwtOnLoad(function() {
              eXo.wiki.Wysiwyg.fireOnModuleLoad = function() {};
                if (typeof errFn == 'function') {
                    errFn();
                }
            }, modName, modBase);
            eXo.wiki.Wysiwyg.fireOnModuleLoad();

            // Restore the default gwtOnLoad function.
            iframe.contentWindow.gwtOnLoad = gwtOnLoad;
            iframe = undefined;
            gwtOnLoad = undefined;
        }

        // Prevent further calls to this method.
        this.hookGwtOnLoad = function(){};
    },

    /**
     * @return the WYSIWYG editor instance associated with the given hookId
     */
    getInstance: function(hookId) {
        return this.instances[hookId];
    },

   /**
     * @return all the WYSIWYG editor instances
     */
    getInstances: function() {
        return this.instances;
    }
  }
};

// Enhance the WysiwygEditor class with custom events.
eXo.wiki.Wysiwyg.onModuleLoad(function() {
    // Declare the functions that will ensure the selection is preserved whenever we switch to fullscreen editing and back.
    WysiwygEditor.prototype._beforeToggleFullScreen = function(event) {
        if (event && event.memo && event.memo.target &&
        	event.memo.target.down('.gwt-RichTextArea') == this.getRichTextArea()) {
            // Save the current selection range.
            this._selectionRange = this.getSelectionRange();
            // Disable the rich text area.
            this.getCommandManager().execute('enable', 'false');
        }
    }
    WysiwygEditor.prototype._afterToggleFullScreen = function(event) {
        if (event && event.memo && event.memo.target &&
    		event.memo.target.down('.gwt-RichTextArea') == this.getRichTextArea()) {
            // Re-enable the rich text area.
            this.getCommandManager().execute('enable', 'true');
            // Restore the selection range.
            this.setSelectionRange(this._selectionRange);
            // We have to delay the focus because we are currently handling the native click event.
            setTimeout(function() {
                this.setFocus(true);
            }.bind(this), 10);
        }
    }
    var WysiwygEditorAspect = function() {
        WysiwygEditorAspect.base.constructor.apply(this, arguments);
        if (this.getRichTextArea()) {
            // Register action listeners.
            var onAction = function(actionName) {
                document.fire('xwiki:wysiwyg:' + actionName, {'instance': this});
            }
            var actionNames = ['loaded', 'showingSource', 'showSource', 'showingWysiwyg', 'showWysiwyg'];
            for(var i = 0; i < actionNames.length; i++) {
                this.addActionHandler(actionNames[i], onAction.bind(this));
            }

            // Preserve rich text area selection when switching to fullscreen editing and back. See XWIKI-6003.
            try {
	            document.observe('xwiki:fullscreen:enter', this._beforeToggleFullScreen.bindAsEventListener(this));
	            document.observe('xwiki:fullscreen:entered', this._afterToggleFullScreen.bindAsEventListener(this));
	            document.observe('xwiki:fullscreen:exit', this._beforeToggleFullScreen.bindAsEventListener(this));
	            document.observe('xwiki:fullscreen:exited', this._afterToggleFullScreen.bindAsEventListener(this));
            } catch (e) {
              console.log("e1: " + e.stack);
            }
	            // If the editor was successfully created then fire a custom event.
            try {
	            document.fire('xwiki:wysiwyg:created', {'instance': this});
            } catch (e) {
              console.log("e2: " + e.stack);
            }
            // Update the list of WYSIWYG editor instances.
            eXo.wiki.Wysiwyg.instances[this.getParameter('hookId')] = this;
        }
    }
    WysiwygEditorAspect.prototype = new WysiwygEditor;
    WysiwygEditorAspect.base = WysiwygEditor.prototype;
    WysiwygEditor = WysiwygEditorAspect;
}, true);

})();