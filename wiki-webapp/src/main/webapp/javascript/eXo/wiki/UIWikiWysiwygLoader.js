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
          jquery: '/wiki/webjars/jquery/1.11.1/jquery',
          ckeditor: '/wiki/webjars/application-ckeditor-webjar/1.25/ckeditor',
          resource: '/wiki/webjars/application-ckeditor-webjar/1.25/plugins/xwiki-resource/resourcePicker.bundle.min',
          resourcePicker: '/wiki/webjars/application-ckeditor-webjar/1.25/plugins/xwiki-resource/resourcePicker.bundle.min',
          entityResourcePicker: '/wiki/webjars/application-ckeditor-webjar/1.25/plugins/xwiki-resource/resourcePicker.bundle.min',
          entityResourceSuggester: '/wiki/webjars/application-ckeditor-webjar/1.25/plugins/xwiki-resource/resourcePicker.bundle.min',
          entityResourceDisplayer: '/wiki/webjars/application-ckeditor-webjar/1.25/plugins/xwiki-resource/resourcePicker.bundle.min',
          modal: '/wiki/webjars/application-ckeditor-webjar/1.25/plugins/xwiki-dialog/modal.min',
          l10n: '/wiki/webjars/application-ckeditor-webjar/1.25/plugins/xwiki-localization/l10n.min',
          macroWizard: '/wiki/webjars/application-ckeditor-webjar/1.25/plugins/xwiki-macro/macroWizard.bundle.min',
          bootstrap: '/wiki/webjars/bootstrap/3.3.7/js/bootstrap',
          'bootstrap3-typeahead': '/wiki/webjars/bootstrap-3-typeahead/4.0.2/bootstrap3-typeahead'
        },
        shim: {
          ckeditor: {
            exports: 'CKEDITOR',
            // This includes dependencies of the plugins bundled with the CKEditor code.
            deps: ['jquery', 'resource', 'resourcePicker', 'macroWizard']
          }
        },
        config: {
          l10n: {
            url: eXo.env.portal.context + '/' + eXo.env.portal.rest + '/i18n/bundle/locale.portlet.wiki.WikiPortlet-' + eXo.env.portal.language + '.json'
          }
        }
      });

      window.CKEDITOR_BASEPATH = "/wiki/webjars/application-ckeditor-webjar/1.25/";

      window.require(['ckeditor'], function(ckeditor) {
        var language = ($('html').attr('xml:lang') || '').toLowerCase().replace('_', '-');

        var isMacroOutput = function(element) {
          return element.getAscendant && element.getAscendant(function(ancestor) {
            var previousSibling = ancestor.previous;
            // Look for the first (closest) sibling macro marker comment, knowing that macro markers cannot be nested.
            while (previousSibling && (previousSibling.type !== ckeditor.NODE_COMMENT ||
              (previousSibling.value !== 'stopmacro' && previousSibling.value.substr(0, 11) !== 'startmacro:'))) {
              previousSibling = previousSibling.previous;
            }
            return previousSibling && previousSibling.value !== 'stopmacro';
          });
        };

        // See http://docs.ckeditor.com/#!/guide/dev_allowed_content_rules
        var allowedContentBySyntax = {
          'xwiki/2.1': {
            '$1': {
              elements: {
                // Elements required because the editor input is a full HTML page.
                html: true, head: true, link: true, body: true,
                // Headings
                h1: true, h2: true, h3: true, h4: true, h5: true, h6: true,
                // Lists
                dl: true, ol: true, ul: true,
                // Tables
                table: true, tr: true, th: true, td: true,
                // Formatting
                span: true, strong: true, em: true, ins: true, del: true, sub: true, sup: true, tt: true, pre: true,
                // Others
                div: true, hr: true, p: true, a: true, img: true, blockquote: true
              },
              // The elements above can have any attribute, through the parameter (%%) syntax.
              attributes: '*',
              styles: '*',
              classes: '*'
            },
            '$2': {
              // The XWiki syntax doesn't support parameters for the following elements.
              elements: {br: true, dd: true, dt: true, li: true, tbody: true}
            },
            '$3': {
              // Wiki syntax macros can output any HTML.
              match: isMacroOutput,
              attributes: '*',
              styles: '*',
              classes: '*'
            }
          }
        };
        allowedContentBySyntax['xwiki/2.0'] = allowedContentBySyntax['xwiki/2.1'];

        // Extend the default CKEditor configuration
        var getConfig = function(element) {
          var config = {
            allowedContent: allowedContentBySyntax['xwiki/2.0'],
            coreStyles_strike: {
              element: 'del',
              overrides: ['s', 'strike']
            },
            coreStyles_underline: {
              element: 'ins',
              overrides: 'u'
            },
            filebrowserUploadUrl: '/portal', //getUploadURL(sourceDocument, 'filebrowser'),
            // This is used in CKEditor.FileUploader so we must keep them in sync.
            fileTools_defaultFileName: '__fileCreatedFromDataURI__',
            height: 380,
            // CKEditor uses '-' (dash) as locale separator (between the language code and the country code).
            language: language,
            removeButtons: 'Find,Anchor,Paste,PasteFromWord,PasteText,Blockquote,Language',
            removePlugins: 'bidi,save',
            toolbarGroups: [
              {name: 'basicstyles', groups: ['basicstyles', 'cleanup']},
              {name: 'paragraph',   groups: ['list', 'indent', 'blocks', 'align']},
              {name: 'clipboard',   groups: ['clipboard', 'undo']},
              {name: 'editing',     groups: ['find', 'selection', 'spellchecker']},
              {name: 'forms'},
              '/',
              {name: 'links'},
              {name: 'insert'},
              {name: 'styles'},
              {name: 'colors'},
              {name: 'document',    groups: ['mode', 'document', 'doctools']},
              {name: 'tools'},
              {name: 'others'},
              {name: 'about'}
            ],
            uploadUrl: '/portal/upload',
            'xwiki-link': {
              labelGenerator: '/portal' //sourceDocument.getURL('get', 'sheet=CKEditor.LinkLabelGenerator&amp;outputSyntax=plain')
            },
            'xwiki-resource': {
              dispatcher: '/portal' //sourceDocument.getURL('get', 'sheet=CKEditor.ResourceDispatcher&amp;outputSyntax=plain')
            },
            'xwiki-source': {
              htmlConverter: '/rest/wiki/content'
            },
            customConfig: ''
          };

          return config;
        };

        var setTranslations = function(language) {
          var url = eXo.env.portal.context + '/' + eXo.env.portal.rest + '/i18n/bundle/locale.portlet.wiki.WikiPortlet-' + language + '.json';
          fetch(url, { credentials: 'include' })
            .then(function (resp) {
              return resp.json();
            }).then(function(translations) {
              var translationsArray = [];
              for (var translation in translations) {
                var translationPrefix = 'ckeditor.plugin.';
                if (translation.startsWith(translationPrefix)) {
                  var pluginTranslationName = translation.substring(translationPrefix.length);
                  var pluginName = pluginTranslationName.substring(0, pluginTranslationName.indexOf('.'));
                  var translationName = pluginTranslationName.substring(pluginName.length + 1);
                  translationsArray[pluginName] = translationsArray[pluginName] || {};
                  translationsArray[pluginName][translationName] = translations[translation];
                }
              }
              for (var pluginName in translationsArray) {
                ckeditor.plugins.setLang(pluginName, language, translationsArray[pluginName]);
              }
            });
        }

        var oldReplace = ckeditor.replace;
        ckeditor.replace = function(element, config) {
          // Take into account the configuration options specified on the target element.
          var extendedConfig = ckeditor.tools.extend(getConfig(element), config || {}, true);
          return oldReplace.call(this, element, extendedConfig);
        };

        setTranslations(language);
        ckeditor.replace('UIWikiRichTextArea_TextArea');
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