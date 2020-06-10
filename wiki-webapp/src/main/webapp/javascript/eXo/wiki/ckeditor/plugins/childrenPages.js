import Plugin from '@ckeditor/ckeditor5-core/src/plugin';

import ButtonView from '@ckeditor/ckeditor5-ui/src/button/buttonview';

import { toWidget } from '@ckeditor/ckeditor5-widget/src/utils';

/**
 * CKEditor plugin which displays the list of children pages of the current page.
 * It uses a Vue component which fetches pages and displays them.
 */
export default class ChildrenPages extends Plugin {
  init() {
    const editor = this.editor;

    // Allow children nodes.
    editor.model.schema.register('childrenPages', {
      allowIn: '$root',
      isBlock: true,
      isObject: true
    });

    // Build converter from model to view for data and editing pipelines.
    editor.conversion.for('upcast').elementToElement({
      view: 'exo-wiki-children-pages',
      model: 'childrenPages'
    });
    
    editor.conversion.for('downcast').elementToElement({
      model: 'childrenPages',
      view: (modelElement, viewWriter) => {
        const childrenContainer = viewWriter.createContainerElement('div', { 'class': 'wiki-children-pages' });
        const childrenComponent = viewWriter.createContainerElement('exo-wiki-children-pages');
        viewWriter.insert(viewWriter.createPositionAt(childrenContainer, 'end'), childrenComponent);

        return toWidget( childrenContainer, viewWriter );
      }
    });

    editor.ui.componentFactory.add('insertChildren', locale => {
      const childrenButtonView = new ButtonView(locale);

      childrenButtonView.set({
        label: 'Insert Children',
        class: 'uiIconEcmsRelationListMini',
        tooltip: true
      });

      // Callback executed once the button is clicked.
      childrenButtonView.on('execute', () => {
        editor.model.change( writer => {
          const children = writer.createElement('childrenPages');
          editor.model.insertContent(children);
        });
      });

      return childrenButtonView;
    } );
  }


}