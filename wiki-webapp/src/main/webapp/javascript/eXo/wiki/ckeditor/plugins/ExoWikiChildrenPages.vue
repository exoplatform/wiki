<template>
  <ul>
    <li v-for="page in childrenPages" :key="page.name">
      <a :href="getChildrenPagePath(page)">
        {{ page.name }}
      </a>
    </li>
  </ul>
</template>

<script>
export default {
  props: {
    /** Number of page depth */
    depth: {
      type: Number,
      default: 1
    }
  },
  data : function() {
    return {
      childrenPages: []
    };
  },
  created() {
    this.getChildrenPages();
  },
  methods: {
    getChildrenPages() {
      // no children pages when creating a page
      if(window.location.hash === '#AddPage') {
        return [];
      }

      let pageName = 'WikiHome';
      if(!eXo.env.server.portalBaseURL.endsWith(`/${eXo.env.portal.selectedNodeUri}`)) {
        pageName = eXo.env.server.portalBaseURL.substr(eXo.env.server.portalBaseURL.lastIndexOf('/') + 1);
      }

      let url = '';
      if(eXo.env.portal.spaceName) {
        url = `/rest/wiki/tree/CHILDREN?path=group/spaces/${eXo.env.portal.spaceGroup}/${pageName}&depth=${this.depth}`;
      } else {
        url = `/rest/wiki/tree/CHILDREN?path=portal/${eXo.env.portal.portalName}/${pageName}&depth=${this.depth}`;
      }
      fetch(url, {credentials: 'include'}).then(resp => resp.json()).then(data => {
        if (data && data.jsonList) {
          this.childrenPages = data.jsonList;
        } 
      });
    },
    getChildrenPagePath(page) {
      if(page && page.path) {
        const index = page.path.lastIndexOf('%2F'); // Find the index of character "/"
        const pageId = page.path.substring(index + '%2F'.length);
        if (pageId) {
          let urlPath = window.location.pathname;
          const wikiIndex = urlPath.indexOf('/wiki/');
          if(wikiIndex >= 0) {
            urlPath = urlPath.substring(0, wikiIndex + '/wiki'.length);
          } else if(!urlPath.endsWith('/wiki')) {
            urlPath += '/wiki';
          }
          return urlPath += `/${pageId}`;
        }
      }
      return '#';
    }
  }
};
</script>