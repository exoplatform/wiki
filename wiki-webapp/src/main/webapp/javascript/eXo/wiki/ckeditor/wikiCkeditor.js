import ClassicEditor from '@ckeditor/ckeditor5-editor-classic/src/classiceditor';
import Essentials from '@ckeditor/ckeditor5-essentials/src/essentials';
import Bold from '@ckeditor/ckeditor5-basic-styles/src/bold';
import Italic from '@ckeditor/ckeditor5-basic-styles/src/italic';
import Underline from '@ckeditor/ckeditor5-basic-styles/src/underline';
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
import SelfUpload from 'ckeditor5-self-image/src/selfupload';

function WikiCkeditor() {
};

WikiCkeditor.prototype.createEditor = function() {
  ClassicEditor
    .create( document.querySelector( '#UIWikiRichTextArea_TextArea' ), {
      plugins: [ Essentials, Paragraph, Bold, Italic, Underline, BlockQuote, Heading, Font, Highlight, Alignment, List, Link,
        Table, TableToolbar, Image, ImageToolbar, ImageStyle, ImageUpload, SelfUpload],
      toolbar: [ 'heading',
        'fontSize',
        'fontFamily',
        'highlight',
        '|',
        'alignment',
        'bold',
        'italic',
        'underline',
        'link',
        'bulletedList',
        'numberedList',
        'insertTable',
        'imageUpload',
        'blockQuote',
        'undo',
        'redo'
      ],
      fontSize: {
        options: [
          9,
          11,
          13,
          'default',
          17,
          19,
          21
        ]
      },
      heading: {
        options: [
          { model: 'paragraph', title: 'Paragraph', class: 'ck-heading_paragraph' },
          { model: 'heading1', view: 'h1', title: 'Heading 1', class: 'ck-heading_heading1' },
          { model: 'heading2', view: 'h2', title: 'Heading 2', class: 'ck-heading_heading2' },
          {
            model: 'note',
            view: {
              name: 'span',
              classes: 'box notemessage'
            },
            title: 'Note',
            class: 'notemessage'
          },
          {
            model: 'info',
            view: {
              name: 'span',
              classes: 'box infomessage'
            },
            title: 'Info',
            class: 'infomessage'
          },
          {
            model: 'success',
            view: {
              name: 'span',
              classes: 'box successmessage'
            },
            title: 'Success',
            class: 'successmessage'
          },
          {
            model: 'tip',
            view: {
              name: 'span',
              classes: 'box tipmessage'
            },
            title: 'Tip',
            class: 'tipmessage'
          },
          {
            model: 'warning',
            view: {
              name: 'span',
              classes: 'box warningmessage'
            },
            title: 'Warning',
            class: 'warningmessage'
          },
          {
            model: 'error',
            view: {
              name: 'span',
              classes: 'box errormessage'
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
        // TODO make it work in all cases (group and user navigation)
        uploadUrl: '/portal/rest/wiki/upload/' + eXo.env.portal.containerName + '/' + eXo.env.portal.portalName + eXo.env.server.portalBaseURL.substr(eXo.env.server.portalBaseURL.lastIndexOf('/'))
      },
      image: {
        toolbar: [ 'imageStyle:alignLeft', 'imageStyle:full', 'imageStyle:alignRight', '|', 'imageTextAlternative' ],
        styles: [ 'full', 'alignLeft', 'alignRight']
      }
    } )
    .then(editor => {
      console.log( 'Editor was initialized', editor );
      window.editor = editor;
    })
    .catch(error => {
      console.error( error.stack );
    });
};

let exportedWysiwyg = new WikiCkeditor();
export default exportedWysiwyg;