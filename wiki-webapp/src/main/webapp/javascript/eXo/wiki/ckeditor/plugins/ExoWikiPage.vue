<template>
  <div class="wiki-include-page" v-html="pageContent"></div>
</template>

<script>
export default {
  props: {
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
  mounted() {
    this.getPageContent();
  },
  methods: {
    getPageContent() {
      if (this.pageName) {
        this.pageName = this.pageName.replace(/[&/\\#[\]+%@|'`"^:;*?=<>{}]+/g, '_');
        const self = this;
        let url = `/${eXo.env.portal.containerName}/${eXo.env.portal.rest}/wiki`;
        if (eXo.env.portal.spaceId) {
          url += `/group/spaces/spaces/${eXo.env.portal.spaceGroup}/pages/${this.pageName}`;
        } else if (eXo.env.server.portalBaseURL.includes('/wiki/user/')) {
          url += `/user/spaces/${eXo.env.portal.userName}/pages/${this.pageName}`;
        } else {
          url += `/portal/spaces/global/pages/${this.pageName}`;
        }
        fetch(url)
          .then(response => response.text())
          .then(text => (new window.DOMParser()).parseFromString(text, 'text/xml'))
          .then(xml => xml.documentElement.getElementsByTagName('content')[0].childNodes[0].nodeValue)
          .then(data => self.pageContent = data)
          .catch(error => console.error(`Error while fetching content of page ${self.pageName}`, error));
      }
    }
  }
};
</script>