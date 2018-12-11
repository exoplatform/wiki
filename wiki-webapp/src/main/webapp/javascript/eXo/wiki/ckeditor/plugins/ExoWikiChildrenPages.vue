<template>
  <ul class="wiki-children-pages">
    <li v-for="page in childrenPages" :key="page.name">
      <a :href="getChildrenPagePath(page.name)">
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
      const pageName = eXo.env.server.portalBaseURL.substr(eXo.env.server.portalBaseURL.lastIndexOf('/') + 1);
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
    getChildrenPagePath(pageName) {
      if (pageName) {
        return pageName.split(' ').join('_');
      }
      return '#';
    }
  }
};
</script>