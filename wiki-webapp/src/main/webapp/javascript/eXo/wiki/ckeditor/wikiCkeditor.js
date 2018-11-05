import ClassicEditor from '@ckeditor/ckeditor5-editor-classic/src/classiceditor';
import Essentials from '@ckeditor/ckeditor5-essentials/src/essentials';
import Bold from '@ckeditor/ckeditor5-basic-styles/src/bold';
import Italic from '@ckeditor/ckeditor5-basic-styles/src/italic';
import BlockQuote from '@ckeditor/ckeditor5-block-quote/src/blockquote';
import Heading from '@ckeditor/ckeditor5-heading/src/heading';
import Alignment from '@ckeditor/ckeditor5-alignment/src/alignment';
import Link from '@ckeditor/ckeditor5-link/src/link';
import List from '@ckeditor/ckeditor5-list/src/list';
import Paragraph from '@ckeditor/ckeditor5-paragraph/src/paragraph';
import Table from '@ckeditor/ckeditor5-table/src/table';
import TableToolbar from '@ckeditor/ckeditor5-table/src/tabletoolbar';

function WikiCkeditor() {
};

WikiCkeditor.prototype.createEditor = function() {
  ClassicEditor
    .create( document.querySelector( '#UIWikiRichTextArea_TextArea' ), {
      plugins: [ Essentials, Paragraph, Bold, Italic, BlockQuote, Heading, Alignment, List, Link, Table, TableToolbar],
      toolbar: [ 'heading',
        '|',
        'alignment',
        'bold',
        'italic',
        'link',
        'bulletedList',
        'numberedList',
        'insertTable',
        'blockQuote',
        'undo',
        'redo' ],
      table: {
        contentToolbar: [ 'tableColumn', 'tableRow', 'mergeTableCells' ]
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