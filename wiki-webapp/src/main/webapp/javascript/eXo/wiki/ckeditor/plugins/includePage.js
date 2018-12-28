import Plugin from '@ckeditor/ckeditor5-core/src/plugin';
import ViewPosition from '@ckeditor/ckeditor5-engine/src/view/position';

import ButtonView from '@ckeditor/ckeditor5-ui/src/button/buttonview';

import { toWidget } from '@ckeditor/ckeditor5-widget/src/utils';
import { downcastElementToElement } from '@ckeditor/ckeditor5-engine/src/conversion/downcast-converters';
import { upcastElementToElement } from '@ckeditor/ckeditor5-engine/src/conversion/upcast-converters';

/**
 * CKEditor plugin which displays the list of children pages of the current page
 */
export default class IncludePage extends Plugin {
  init() {
    const editor = this.editor;

    console.log('IncludePage initialized');

    editor.model.schema.register('includePage', {
      allowIn: '$root',
      isBlock: true,
      isObject: true
    });

    // Build converter from model to view for data and editing pipelines.
    editor.conversion.for('upcast').add(upcastElementToElement({
      view: 'exo-wiki-include-page',
      model: 'includePage'
    }));
    
    editor.conversion.for('downcast').add(downcastElementToElement({
      model: 'includePage',
      view: (modelElement, viewWriter) => {
        const pageContainer = viewWriter.createContainerElement('div');
        const pageComponent = viewWriter.createContainerElement('exo-wiki-include-page', { 'page-name': 'test' });
        viewWriter.insert(ViewPosition.createAt(pageContainer, 'end'), pageComponent);

        return toWidget( pageContainer, viewWriter );
      }
    }));

    editor.ui.componentFactory.add('includePage', locale => {
      const includePageButtonView = new ButtonView(locale);

      includePageButtonView.set({
        label: 'Include Page',
        class: 'uiIconEcmsImportNode',
        tooltip: true
      });

      // Callback executed once the button is clicked.
      includePageButtonView.on('execute', () => {
        editor.model.change( writer => {
          console.log('insert page');
          const page = writer.createElement('includePage');
          editor.model.insertContent(page);
        });
      });

      return includePageButtonView;
    });
  }
}