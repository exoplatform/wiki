import DecoupledEditor from '@ckeditor/ckeditor5-editor-decoupled/src/decouplededitor';
import Essentials from '@ckeditor/ckeditor5-essentials/src/essentials';
import Bold from '@ckeditor/ckeditor5-basic-styles/src/bold';
import Italic from '@ckeditor/ckeditor5-basic-styles/src/italic';
import Underline from '@ckeditor/ckeditor5-basic-styles/src/underline';
import Strikethrough from '@ckeditor/ckeditor5-basic-styles/src/strikethrough';
import CodeBlock from '@ckeditor/ckeditor5-code-block/src/codeblock';
import BlockQuote from '@ckeditor/ckeditor5-block-quote/src/blockquote';
import Heading from '@ckeditor/ckeditor5-heading/src/heading';
import Font from '@ckeditor/ckeditor5-font/src/font';
import Highlight from '@ckeditor/ckeditor5-highlight/src/highlight';
import Alignment from '@ckeditor/ckeditor5-alignment/src/alignment';
import Link from '@ckeditor/ckeditor5-link/src/link';
import List from '@ckeditor/ckeditor5-list/src/list';
import Paragraph from '@ckeditor/ckeditor5-paragraph/src/paragraph';
import Table from '@ckeditor/ckeditor5-table/src/table';
import TableToolbar from '@ckeditor/ckeditor5-table/src/tabletoolbar';
import Image from '@ckeditor/ckeditor5-image/src/image';
import ImageToolbar from '@ckeditor/ckeditor5-image/src/imagetoolbar';
import ImageStyle from '@ckeditor/ckeditor5-image/src/imagestyle';
import ImageUpload from '@ckeditor/ckeditor5-image/src/imageupload';
import SelfUpload from 'ckeditor5-self-image/src/selfimage';
import SpecialCharacters from '@ckeditor/ckeditor5-special-characters/src/specialcharacters';
import ChildrenPages from './plugins/childrenPages';
import Toc from './plugins/toc';
import Widget from '@ckeditor/ckeditor5-widget/src/widget';
import IncludePage from './plugins/includePage';

function WikiCkeditor() {
  // do nothing
}

function SpecialCharactersEmoji( editor ) {
  editor.plugins.get( 'SpecialCharacters' ).addItems( 'Emoji', [
    { title: 'Emoticon smile', character: '\u{1F642}' },
    { title: 'Emoticon unhappy', character: '\u{1F641}' },
    { title: 'Emoticon tongue', character: '\u{1F61B}' },
    { title: 'Emoticon grin', character: '\u{1F600}' },
    { title: 'Emoticon wink', character: '\u{1F609}' },
    { title: 'Thumb up', character: '\u{1F44D}' },
    { title: 'Thumb down', character: '\u{1F44E}' },
    { title: 'Information', character: 'ℹ️' },
    { title: 'Check mark', character: '✅️' },
    { title: 'No entry', character: '⛔' },
    { title: 'Warning', character: '⚠' },
    { title: 'Plus', character: '➕' },
    { title: 'Minus', character: '➖' },
    { title: 'Help', character: '❓' },
    { title: 'Light bulb', character: '\u{1F4A1}' },
    { title: 'Chequered flag', character: '\u{1F3C1}' },
    { title: 'Star', character: '⭐' }
  ] );
}

WikiCkeditor.prototype.createEditor = function() {

  let uploadUrl = `/${eXo.env.portal.containerName}/${eXo.env.portal.rest}/wiki/upload`;
  if(eXo.env.portal.spaceId) {
    uploadUrl += `/group//spaces/${eXo.env.portal.spaceGroup}`;
  } else if (eXo.env.server.portalBaseURL.includes('/wiki/user/')) {
    uploadUrl += `/user/${eXo.env.portal.userName}`;
  } else {
    uploadUrl += '/portal/global';
  }
  uploadUrl += `${eXo.env.server.portalBaseURL.substr(eXo.env.server.portalBaseURL.lastIndexOf('/'))}`;

  DecoupledEditor
    .create( document.querySelector( '.UIWikiRichTextEditor' ), {
      plugins: [ Essentials, Paragraph, Bold, Italic, Underline, Strikethrough, CodeBlock, BlockQuote, Heading, Font, Highlight, Alignment, List, Link,
        Table, TableToolbar, Image, ImageToolbar, ImageStyle, ImageUpload, SelfUpload, SpecialCharacters, SpecialCharactersEmoji,
        ChildrenPages, Toc, Widget, IncludePage],
      toolbar: [ 'heading',
        'specialCharacters',
        'fontFamily',
        'fontSize',
        'bold',
        'italic',
        'underline',
        'strikethrough',
        'codeBlock',
        'highlight',
        'alignment',
        'numberedList',
        'bulletedList',
        'blockQuote',
        'link',
        'insertTable',
        'imageUpload',
        'insertToc',
        'insertChildren',
        'includePage',
        'undo',
        'redo'
      ],
      fontSize: {
        options: [
          9,          // eslint-disable-line no-magic-numbers
          11,         // eslint-disable-line no-magic-numbers
          13,         // eslint-disable-line no-magic-numbers
          'default',
          17,         // eslint-disable-line no-magic-numbers
          19,         // eslint-disable-line no-magic-numbers
          21          // eslint-disable-line no-magic-numbers
        ]
      },
      heading: {
        options: [
          { model: 'paragraph', title: 'Paragraph', class: 'ck-heading_paragraph' },
          { model: 'heading1', view: 'h1', title: 'Heading 1', class: 'ck-heading_heading1' },
          { model: 'heading2', view: 'h2', title: 'Heading 2', class: 'ck-heading_heading2' },
          { model: 'heading3', view: 'h3', title: 'Heading 3', class: 'ck-heading_heading3' },
          { model: 'heading4', view: 'h4', title: 'Heading 4', class: 'ck-heading_heading4' },
          {
            model: 'note',
            view: {
              name: 'div',
              classes: ['box', 'notemessage']
            },
            title: 'Note',
            class: 'notemessage'
          },
          {
            model: 'info',
            view: {
              name: 'div',
              classes: ['box', 'infomessage']
            },
            title: 'Info',
            class: 'infomessage'
          },
          {
            model: 'success',
            view: {
              name: 'div',
              classes: ['box', 'successmessage']
            },
            title: 'Success',
            class: 'successmessage'
          },
          {
            model: 'tip',
            view: {
              name: 'div',
              classes: ['box', 'tipmessage']
            },
            title: 'Tip',
            class: 'tipmessage'
          },
          {
            model: 'warning',
            view: {
              name: 'div',
              classes: ['box', 'warningmessage']
            },
            title: 'Warning',
            class: 'warningmessage'
          },
          {
            model: 'error',
            view: {
              name: 'div',
              classes: ['box', 'errormessage']
            },
            title: 'Error',
            class: 'errormessage'
          }
        ]
      },
      table: {
        contentToolbar: [ 'tableColumn', 'tableRow', 'mergeTableCells' ]
      },
      selfUpload: {
        uploadUrl: uploadUrl
      },
      image: {
        toolbar: [ 'imageStyle:alignLeft', 'imageStyle:full', 'imageStyle:alignRight', '|', 'imageTextAlternative' ],
        styles: [ 'full', 'alignLeft', 'alignRight']
      }
    } )
    .then(editor => {
      // The toolbar needs to be explicitly appended.
      document.querySelector( '.UIWikiEditorToolbar' ).appendChild( editor.ui.view.toolbar.element );

      window.editor = editor;

      const textareaElement = document.querySelector('#UIWikiRichTextArea_TextArea');
      if(textareaElement) {
        let data = textareaElement.value;
        // replace br by new line character to render new line correctly in textearea (used only for code blocks)
        data = data.replace(/<br>/g, '\n');
        editor.setData(data);
      }

      editor.model.document.on('change:data', () => {
        let data = window.editor.getData();
        // replace new line characters by br to render new lines correctly in editor (used only for code blocks)
        data = data.replace(/\n/g, '<br>');
        document.querySelector('#UIWikiRichTextArea_TextArea').innerText = data;
      });
    })
    .catch(error => {
      console.error( error.stack );
    });
};

const exportedWysiwyg = new WikiCkeditor();
export default exportedWysiwyg;