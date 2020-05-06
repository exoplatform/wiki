import Plugin from '@ckeditor/ckeditor5-core/src/plugin';
import ViewPosition from '@ckeditor/ckeditor5-engine/src/view/position';

import ButtonView from '@ckeditor/ckeditor5-ui/src/button/buttonview';

import { toWidget } from '@ckeditor/ckeditor5-widget/src/utils';

/**
 * CKEditor plugin which displays the list of children pages of the current page
 */
export default class ChildrenPages extends Plugin {
  init() {
    const editor = this.editor;

    console.log('ChildrenPages initialized');

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
        const childrenContainer = viewWriter.createContainerElement('p', { 'class': 'wiki-children-pages' });
        const childrenComponent = viewWriter.createContainerElement('exo-wiki-children-pages');
        viewWriter.insert(ViewPosition.createAt(childrenContainer, 'end'), childrenComponent);

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
          console.log('insert Children');
          const children = writer.createElement('childrenPages');
          editor.model.insertContent(children);
        });
      });

      return childrenButtonView;
    } );
  }


}

/**
 * Build Children Pages
 *
 * @param {Model} model CKEditor model
 * @param {DowncastWriter} viewWriter CKEditor downcast writer
 * @return {Array} Generated ToC
 */
/*
function buildChildrenPages(model, viewWriter) {
  const childrenContainer = viewWriter.createContainerElement('div', { 'class': 'childrenPages' });

  const data = fetchChildrenPages();

  const ulEntry = viewWriter.createContainerElement('ul');
  viewWriter.insert(ViewPosition.createAt(childrenContainer, 'end'), ulEntry);

  for (const childrenPage of data) {
    const childrenEntry = viewWriter.createContainerElement('li');
    viewWriter.insert(ViewPosition.createAt(ulEntry, 'end'), childrenEntry);

    const pathSeparator = '%2F';
    const pageName = childrenPage.path.substr(childrenPage.path.lastIndexOf(pathSeparator) + pathSeparator.length);
    const pageLink = viewWriter.createContainerElement('a', { 'href': eXo.wiki.UITreeExplorer.baseLink + pageName });
    viewWriter.insert(ViewPosition.createAt(childrenEntry, 'end'), pageLink);

    const entryText = childrenPage.name;
    const entry = viewWriter.createText(entryText);
    viewWriter.insert(ViewPosition.createAt(pageLink, 'end'), entry);
  }

  return childrenContainer;
}
*/

/**
 * Fetch children pages
 *
 * @return {Array} List of children pages
 */
/*
function fetchChildrenPages() {
  const HTTP_OK = 200;

  let result = [];
  const xhr = new XMLHttpRequest();
  xhr.onreadystatechange = function() {
    if (xhr.status === HTTP_OK) {
      result = JSON.parse(xhr.responseText);
    }
  };

  // TODO Manage url for users wikis
  let url = null;
  const pageName = eXo.env.server.portalBaseURL.substr(eXo.env.server.portalBaseURL.lastIndexOf('/') + 1);
  if(eXo.env.portal.spaceName) {
    url = `/rest/wiki/tree/CHILDREN?path=group/spaces/${eXo.env.portal.spaceGroup}/${pageName}&depth=1`;
  } else {
    url = `/rest/wiki/tree/CHILDREN?path=portal/${eXo.env.portal.portalName}/${pageName}&depth=1`;
  }
  xhr.open('GET', url, false);
  xhr.send();

  if(result && result.jsonList) {
    result = result.jsonList;
  }

  return result;
}
*/