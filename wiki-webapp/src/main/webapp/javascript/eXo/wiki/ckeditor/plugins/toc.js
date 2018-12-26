import Plugin from '@ckeditor/ckeditor5-core/src/plugin';
import ViewPosition from '@ckeditor/ckeditor5-engine/src/view/position';

import ButtonView from '@ckeditor/ckeditor5-ui/src/button/buttonview';

import { toWidget } from '@ckeditor/ckeditor5-widget/src/utils';
import { downcastElementToElement } from '@ckeditor/ckeditor5-engine/src/conversion/downcast-converters';
import { upcastElementToElement } from '@ckeditor/ckeditor5-engine/src/conversion/upcast-converters';

export default class Toc extends Plugin {
  init() {
    console.log('Toc was initialized');

    const editor = this.editor;

    // Allow toc nodes.
    editor.model.schema.register('toc', {
      allowIn: '$root',
      isBlock: true,
      isObject: true
    });

    // Build converter from model to view for data and editing pipelines.
    editor.conversion.for('upcast').add(upcastElementToElement({
      view: {
        name: 'div',
        classes: 'toc'
      },
      model: 'toc'
    }));
    editor.conversion.for('dataDowncast').add(downcastElementToElement({
      model: 'toc',
      view: (modelElement, viewWriter) => {
        return buildToc(editor.model, viewWriter);
      }
    }));
    editor.conversion.for('editingDowncast').add(downcastElementToElement({
      model: 'toc',
      view: (modelElement, viewWriter) => {
        const div = buildToc(editor.model, viewWriter);

        return toWidget( div, viewWriter, { label: 'widget label' } );
      }
    }))
      .add(dispatcher => dispatcher.on('insert:heading1', (evt, data, conversionApi) => {
        if(conversionApi.consumable) {
          conversionApi.consumable.consume(data.item, evt.name);
        }

        updateTocs(editor.model, conversionApi);
      }))
      .add(dispatcher => dispatcher.on('insert:heading2', (evt, data, conversionApi) => {
        if(conversionApi.consumable) {
          conversionApi.consumable.consume(data.item, evt.name);
        }

        updateTocs(editor.model, conversionApi);
      }))
      .add(dispatcher => dispatcher.on('remove:heading1', (evt, data, conversionApi) => {
        if(conversionApi.consumable) {
          conversionApi.consumable.consume(data.item, evt.name);
        }

        updateTocs(editor.model, conversionApi);
      }))
      .add(dispatcher => dispatcher.on('remove:heading2', (evt, data, conversionApi) => {
        if(conversionApi.consumable) {
          conversionApi.consumable.consume(data.item, evt.name);
        }

        updateTocs(editor.model, conversionApi);
      }));

    editor.ui.componentFactory.add('insertToc', locale => {
      const tocButtonView = new ButtonView(locale);

      tocButtonView.set({
        label: 'Insert ToC',
        class: 'uiIconPortlet',
        tooltip: true
      });

      // Callback executed once the button is clicked.
      tocButtonView.on('execute', () => {
        editor.model.change( writer => {
          console.log('insert ToC');
          const toc = writer.createElement('toc');
          editor.model.insertContent(toc);
        });
      });

      return tocButtonView;
    } );
  }


}

/**
 * Update existing ToCs
 *
 * @param {Model} model CKEditor model
 * @param {Object} conversionApi CKEditor conversionApi
 * @return {void}
 */
function updateTocs(model, conversionApi) {
  const mainRoot = model.document.getRoot();
  for (const element of mainRoot.getChildren()) {
    if(element.name === 'toc') {
      console.log('toc found');
      // TODO replace view element with the new toc
      const viewElement = conversionApi.mapper.toViewElement(element);
      console.log(`viewElement=${viewElement}`);
      //let documentFragment = conversionApi.writer.remove(viewElement);
      //conversionApi.writer.insert(documentFragment, buildToc(model, conversionApi.writer));
    }
  }
}

/**
 * Build ToC
 *
 * @param {Model} model CKEditor model
 * @param {DowncastWriter} viewWriter CKEditor downcast writer
 * @return {Array} Generated ToC
 */
function buildToc(model, viewWriter) {
  const toc = viewWriter.createContainerElement('div', { 'class': 'toc' });

  const headings = extractHeadings(model);

  let currentLevel = 0;
  const entryStack = new Array();
  entryStack.push(toc);
  for(const heading of headings) {
    const headingLevel = heading.name.substr('heading'.length);

    if(headingLevel > currentLevel) {
      for(let i=currentLevel; i<headingLevel; i++) {
        const ulEntry = viewWriter.createContainerElement('ul');
        viewWriter.insert(ViewPosition.createAt(entryStack[entryStack.length - 1], 'end'), ulEntry);
        entryStack.push(ulEntry);
      }
    } else if(headingLevel < currentLevel) {
      entryStack.pop();
      entryStack.pop();
    } else {
      entryStack.pop();
    }
    currentLevel = headingLevel;

    const tocEntry = viewWriter.createContainerElement('li');
    viewWriter.insert(ViewPosition.createAt(entryStack[entryStack.length-1], 'end'), tocEntry);
    entryStack.push(tocEntry);

    const entryText = heading.getChildren().next().value.data;
    const entry = viewWriter.createText(entryText);
    viewWriter.insert(ViewPosition.createAt(tocEntry, 'end'), entry);
  }

  return toc;
}

/**
 * Parse the model to extract headings
 *
 * @param {Model} model CKEditor model
 * @return {Array} List of headings model elements
 */
function extractHeadings(model) {
  const headings = [];

  const mainRoot = model.document.getRoot();
  for (const element of mainRoot.getChildren()) {
    if(element.name.startsWith('heading')) {
      headings.push(element);
    }
  }

  return headings;
}