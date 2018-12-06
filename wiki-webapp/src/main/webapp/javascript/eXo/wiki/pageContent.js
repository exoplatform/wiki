import Vue from 'vue';
import ExoWikiChildrenPages from './ckeditor/plugins/ExoWikiChildrenPages.vue';

Vue.component('exo-wiki-children-pages', ExoWikiChildrenPages);

new Vue({
  el: '.uiWikiContentDisplay'
});