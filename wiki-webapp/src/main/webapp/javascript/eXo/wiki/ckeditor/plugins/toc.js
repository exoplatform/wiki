import Plugin from '@ckeditor/ckeditor5-core/src/plugin';

import ButtonView from '@ckeditor/ckeditor5-ui/src/button/buttonview';

import { toWidget } from '@ckeditor/ckeditor5-widget/src/utils';

const defaultModelElement = 'paragraph';

export default class Toc extends Plugin {
  init() {

    const editor = this.editor;

    // Allow toc nodes.
    editor.model.schema.register('toc', {
      allowIn: '$root',
      isBlock: true,
      isObject: true
    });

    const options = editor.config.get( 'heading.options' );
    for ( const option of options ) {
      if (option.model.startsWith('heading')) {
        editor.model.schema.extend( option.model, {
          inheritAllFrom: '$block',
          allowAttributes: [ 'id' ]
        } );

        editor.conversion.elementToElement( option );
      }
    }

    editor.conversion.attributeToAttribute( { model: 'id', view: 'id' } );

    // Build converter from model to view for data and editing pipelines.
    editor.conversion.for('upcast').elementToElement({
      view: {
        name: 'div',
        classes: 'toc'
      },
      model: 'toc'
    });
    editor.conversion.for('dataDowncast').elementToElement({
      model: 'toc',
      view: (modelElement, viewWriter) => {
        return buildToc(editor.model, viewWriter);
      }
    });
    editor.conversion.for('editingDowncast').elementToElement({
      model: 'toc',
      view: (modelElement, viewWriter) => {
        const div = buildToc(editor.model, viewWriter);

        return toWidget( div, viewWriter, { label: 'widget label' } );
      }
    })
      .add(dispatcher => dispatcher.on('insert:heading1', (evt, data, conversionApi) => {
        if (conversionApi.consumable) {
          conversionApi.consumable.consume(data.item, evt.name);
        }

        updateTocs(editor.model, conversionApi);
      }))
      .add(dispatcher => dispatcher.on('insert:heading2', (evt, data, conversionApi) => {
        if (conversionApi.consumable) {
          conversionApi.consumable.consume(data.item, evt.name);
        }

        updateTocs(editor.model, conversionApi);
      }))
      .add(dispatcher => dispatcher.on('remove:heading1', (evt, data, conversionApi) => {
        if (conversionApi.consumable) {
          conversionApi.consumable.consume(data.item, evt.name);
        }

        updateTocs(editor.model, conversionApi);
      }))
      .add(dispatcher => dispatcher.on('remove:heading2', (evt, data, conversionApi) => {
        if (conversionApi.consumable) {
          conversionApi.consumable.consume(data.item, evt.name);
        }

        updateTocs(editor.model, conversionApi);
      }));

    editor.conversion.for( 'downcast' ).add( dispatcher => {
      // Headings are represented in the model as a "heading1" element.
      // Use the "low" listener priority to apply the changes after the headings feature.
      dispatcher.on( 'insert:heading1', ( evt, data, conversionApi ) => {
        const viewWriter = conversionApi.writer;

        const headingElement = conversionApi.mapper.toViewElement( data.item );
        if (headingElement && !headingElement.getAttribute('id')) {
          viewWriter.setAttribute( 'id', generateToken(), conversionApi.mapper.toViewElement( data.item ) );
        }
      }, { priority: 'low' } );
    } )
      .add( dispatcher => {
        // Headings are represented in the model as a "heading1" element.
        // Use the "low" listener priority to apply the changes after the headings feature.
        dispatcher.on( 'insert:heading2', ( evt, data, conversionApi ) => {
          const viewWriter = conversionApi.writer;

          const headingElement = conversionApi.mapper.toViewElement( data.item );
          if (headingElement && !headingElement.getAttribute('id')) {
            viewWriter.setAttribute( 'id', generateToken(), conversionApi.mapper.toViewElement( data.item ) );
          }
        }, { priority: 'low' } );
      } );

    function generateToken() {
      const maxToken = 10000;
      const tokenNumber = Math.round(Math.random(maxToken) * maxToken);
      return `H${tokenNumber}`;
    }

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
          const toc = writer.createElement('toc');
          editor.model.insertContent(toc);
        });
      });

      return tocButtonView;
    } );
  }

  afterInit() {
    // If the enter command is added to the editor, alter its behavior.
    // Enter at the end of a heading element should create a paragraph.
    const editor = this.editor;
    const enterCommand = editor.commands.get( 'enter' );
    const options = editor.config.get( 'heading.options' );

    if ( enterCommand ) {
      this.listenTo( enterCommand, 'afterExecute', ( evt, data ) => {
        const positionParent = editor.model.document.selection.getFirstPosition().parent;
        const isHeading = options.some( option => positionParent.is( option.model ) );

        if ( isHeading && positionParent.is( defaultModelElement ) && positionParent.childCount === 0 ) {
          data.writer.removeAttribute( 'id', positionParent);
        }
      } );
    }
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
    if (element.name === 'toc') {
      // TODO replace view element with the new toc
      conversionApi.mapper.toViewElement(element);
      //const viewElement = conversionApi.mapper.toViewElement(element);
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
  for (const heading of headings) {
    const headingLevel = heading.name.substr('heading'.length);

    if (headingLevel > currentLevel) {
      for (let i=currentLevel; i<headingLevel; i++) {
        const ulEntry = viewWriter.createContainerElement('ul');
        viewWriter.insert(viewWriter.createPositionAt(entryStack[entryStack.length - 1], 'end'), ulEntry);
        entryStack.push(ulEntry);
      }
    } else if (headingLevel < currentLevel) {
      entryStack.pop();
      entryStack.pop();
    } else {
      entryStack.pop();
    }
    currentLevel = headingLevel;

    const tocEntry = viewWriter.createContainerElement('li');
    viewWriter.insert(viewWriter.createPositionAt(entryStack[entryStack.length-1], 'end'), tocEntry);
    entryStack.push(tocEntry);

    const tocEntryLink = viewWriter.createContainerElement('a', { 'href': `#${heading.getAttribute('id')}` });
    viewWriter.insert(viewWriter.createPositionAt(tocEntry, 'end'), tocEntryLink);

    const children = heading.getChildren().next();
    const entryText = children.value ? children.value.data : '';
    const entry = viewWriter.createText(entryText);
    viewWriter.insert(viewWriter.createPositionAt(tocEntryLink, 'end'), entry);
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
    if (element.name.startsWith('heading')) {
      headings.push(element);
    }
  }

  return headings;
}