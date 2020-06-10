import Plugin from '@ckeditor/ckeditor5-core/src/plugin';
import IncludePageFormView from './IncludePageView';

import ButtonView from '@ckeditor/ckeditor5-ui/src/button/buttonview';

import ContextualBalloon from '@ckeditor/ckeditor5-ui/src/panel/balloon/contextualballoon';

import { toWidget } from '@ckeditor/ckeditor5-widget/src/utils';

/**
 * CKEditor plugin which displays the list of children pages of the current page.
 * It uses a Vue component which fetches the page content and displays it.
 */
export default class IncludePage extends Plugin {
  init() {
    const editor = this.editor;

    ///// UI
    this.formView = this._createFormView();

    this._balloon = editor.plugins.get(ContextualBalloon);

    this._createToolbarIncludePageButton();
    /////

    editor.model.schema.register('includePage', {
      allowIn: '$root',
      isBlock: true,
      isObject: true,
      allowAttributes: [ 'pageName' ]
    });

    // Build converter from model to view for data and editing pipelines.
    editor.conversion.for('upcast').elementToElement({
      view: {
        name: 'div',
        classes: 'wiki-include-page'
      },
      model: ( viewElement, modelWriter ) => {
        return modelWriter.createElement( 'includePage', { pageName: viewElement.getChildren().next().value.getAttribute('page-name') } );
      }
    });

    editor.conversion.for('editingDowncast').elementToElement({
      model: 'includePage',
      view: (modelElement, viewWriter) => {
        const pageContainer = viewWriter.createContainerElement('div', { 'class' : 'wiki-include-page' });
        const labelContainer = viewWriter.createContainerElement('span', { 'class' : 'wiki-include-page-label' });
        const label = viewWriter.createText(`Included page : ${modelElement.getAttribute('pageName')}`);
        viewWriter.insert(viewWriter.createPositionAt(labelContainer, 'end'), label);
        viewWriter.insert(viewWriter.createPositionAt(pageContainer, 'end'), labelContainer);
        const pageComponent = viewWriter.createContainerElement('exo-wiki-include-page', { 'page-name': modelElement.getAttribute('pageName') });
        viewWriter.insert(viewWriter.createPositionAt(pageContainer, 'end'), pageComponent);

        return toWidget(pageContainer, viewWriter);
      }
    });
    editor.conversion.for('dataDowncast').elementToElement({
      model: 'includePage',
      view: (modelElement, viewWriter) => {
        const pageContainer = viewWriter.createContainerElement('div', { 'class' : 'wiki-include-page' });
        const pageComponent = viewWriter.createContainerElement('exo-wiki-include-page', { 'page-name': modelElement.getAttribute('pageName') });
        viewWriter.insert(viewWriter.createPositionAt(pageContainer, 'end'), pageComponent);

        return pageContainer;
      }
    });
  }


  ///////////////////////

  _createToolbarIncludePageButton() {
    const editor = this.editor;

    editor.ui.componentFactory.add('includePage', locale => {
      const includePageButtonView = new ButtonView(locale);

      includePageButtonView.set({
        isEnabled: true,
        label: 'Include Page',
        class: 'uiIconEcmsImportNode',
        tooltip: true
      });

      // Bind button to the command.
      //includePageButtonView.bind( 'isOn', 'isEnabled' ).to( includePageCommand, 'value', 'isEnabled' );

      // Callback executed once the button is clicked.
      /*
      includePageButtonView.on('execute', () => {
        editor.model.change( writer => {
          console.log('insert page');
          const page = writer.createElement('includePage');
          editor.model.insertContent(page);
        });
      });
      */

      // Show the panel on button click.
      this.listenTo(includePageButtonView, 'execute', () => this._showUI());

      return includePageButtonView;
    });
  }

  _createFormView() {
    const editor = this.editor;
    const formView = new IncludePageFormView(editor.locale);
    //const includePageCommand = editor.commands.get( 'includePage' );

    //formView.urlInputView.bind( 'value' ).to( includePageCommand, 'value' );

    // Execute link command after clicking the "Save" button.
    this.listenTo( formView, 'submit', () => {
      editor.model.change( writer => {
        //const page = writer.createElement('includePage');
        const page = writer.createElement('includePage', { 'pageName': formView.pageNameInputView.inputView.element.value });
        //page._setAttribute('pageName', formView.pageNameInputView.inputView.element.value);
        //writer.setAttribute('pageName', formView.pageNameInputView.inputView.element.value, page);
        editor.model.insertContent(page);
      });
      //editor.execute( 'link', formView.pageNameInputView.inputView.element.value );
      this._removeFormView();
    } );

    // Hide the panel after clicking the "Cancel" button.
    this.listenTo(formView, 'cancel', () => {
      this._removeFormView();
    });

    return formView;
  }

  _addFormView() {
    if (this._isFormInPanel) {
      return;
    }

    //const editor = this.editor;
    //const includePageCommand = editor.commands.get( 'includePage' );

    this._balloon.add({
      view: this.formView,
      position: this.getBalloonPositionData()
    });

    this.formView.pageNameInputView.select();

    // Make sure that each time the panel shows up, the URL field remains in sync with the value of
    // the command. If the user typed in the input, then canceled the balloon (`urlInputView#value` stays
    // unaltered) and re-opened it without changing the value of the link command (e.g. because they
    // clicked the same link), they would see the old value instead of the actual value of the command.
    // https://github.com/ckeditor/ckeditor5-link/issues/78
    // https://github.com/ckeditor/ckeditor5-link/issues/123
    this.formView.pageNameInputView.inputView.element.value = '';
  }

  /**
	 * Returns true when {@link #formView} is in the {@link #_balloon}.
	 *
	 * @readonly
	 * @protected
	 * @type {Boolean}
	 */
  get _isFormInPanel() {
    return this._balloon.hasView( this.formView );
  }

  getBalloonPositionData() {
    const view = this.editor.editing.view;
    const viewDocument = view.document;

    const target = view.domConverter.viewRangeToDom( viewDocument.selection.getFirstRange() );

    return { target };
  }

  _removeFormView() {
    if(this._isFormInPanel) {
      this._balloon.remove(this.formView);

      // Because the form has an input which has focus, the focus must be brought back
      // to the editor. Otherwise, it would be lost.
      this.editor.editing.view.focus();
    }
  }

  _showUI() {
    this._addFormView();

    // Begin responding to ui#update once the UI is added.
    //this._startUpdatingUI();
  }

  ///////////////////////

}