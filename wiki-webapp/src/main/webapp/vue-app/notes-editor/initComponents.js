import NotesManagement from './components/NotesManagement.vue';

const components = {
  'notes-management': NotesManagement,
};

for (const key in components) {
  Vue.component(key, components[key]);
}