import NotesDisplay from './components/NotesDisplay.vue';

const components = {
  'notes-display': NotesDisplay,
};

for (const key in components) {
  Vue.component(key, components[key]);
}
