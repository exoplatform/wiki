<template>
  <div class="wiki-include-page" v-html="pageContent"></div>
</template>

<script>
export default {
  props: {
    /** Included page name  */
    pageName: {
      type: String,
      default() {
        return '';
      }
    }
  },
  data() {
    return {
      pageContent: ''
    };
  },
  created() {
    this.getPageContent();
  },
  methods: {
    getPageContent() {
      if (this.pageName) {
        // TODO api call to get page content using pageName
        this.pageContent = `<h1>Content of page ${this.pageName}</h1>`;
      }
    }
  }
};
</script>