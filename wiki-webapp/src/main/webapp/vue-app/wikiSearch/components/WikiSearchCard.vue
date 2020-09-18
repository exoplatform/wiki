<template>
  <v-card
    class="wikiSearchCard d-flex flex-column border-radius box-shadow"
    flat
    min-height="227"
  >
    <v-card-text v-if="poster" class="px-2 pt-2 pb-0">
      <exo-user-avatar
        :username="posterUsername"
        :fullname="posterFullname"
        :title="posterFullname"
        avatar-class="border-color"
      >
        <template slot="subTitle">
          <date-format :value="createdDate" />
        </template>
      </exo-user-avatar>
    </v-card-text>
    <div class="mx-auto d-flex flex-grow-1 px-3 py-0 overflow-hidden">
      <div
        ref="excerptNode"
        :title="excerptText"
        class="text-wrap text-break caption flex-grow-1"
      >
      </div>
    </div>
    <v-list class="light-grey-background flex-grow-0 border-top-color no-border-radius pa-0">
      <v-list-item class="px-0 pt-1 pb-2" :href="wikiUrl">
        <v-list-item-icon class="mx-0 my-auto">
          <span class="uiIconWiki tertiary--text pl-1 pr-2 display-1"></span>
        </v-list-item-icon>
        <v-list-item-content>
          <v-list-item-title :title="wikiTitle">
            <a
              :title="wikiTitle"
              class="wikiTitle px-3 pt-2 pb-1 pl-0 text-left text-truncate"
              v-html="wikiTitle"
            >
            </a>
          </v-list-item-title>
          <v-list-item-subtitle>
            <template v-if="spaceDisplayName">
              {{ spaceDisplayName }}
            </template>
          </v-list-item-subtitle>
        </v-list-item-content>
      </v-list-item>
    </v-list>
  </v-card>
</template>

<script>
export default {
  props: {
    term: {
      type: String,
      default: null,
    },
    result: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    lineHeight: 22,
  }),
  computed: {
    wikiUrl() {
      return this.result && this.result.url;
    },
    excerpts() {
      return this.result && this.result.excerpt;
    },
    excerptHtml() {
      return this.excerpts && this.excerpts.concat('\r\n...');
    },
    excerptText() {
      return $('<div />').html(this.excerptHtml).text();
    },
    createdDate() {
      return this.result && this.result.createdDate;
    },
    wikiTitle() {
      return this.result && this.result.title || '';
    },
    poster() {
      return this.result && this.result.poster.profile;
    },
    posterFullname() {
      return this.poster && this.poster.fullname;
    },
    posterUsername() {
      return this.poster && this.poster.username;
    },
    wikiOwner() {
      return this.result && this.result.wikiOwner && this.result.wikiOwner.space || this.result.wikiOwner && this.result.wikiOwner.profile;
    },
    spaceDisplayName() {
      return this.wikiOwner && this.wikiOwner.displayName;
    },
  },
  mounted() {
    this.computeEllipsis();
  },
  methods: {
    computeEllipsis() {
      if (!this.excerptHtml || this.excerptHtml.length === 0) {
        return;
      }
      const excerptParent = this.$refs.excerptNode;
      if (!excerptParent) {
        return;
      }
      excerptParent.innerHTML = this.excerptHtml;

      let charsToDelete = 20;
      let excerptParentHeight = excerptParent.getBoundingClientRect().height || this.lineHeight;
      if (excerptParentHeight > this.maxEllipsisHeight) {
        while (excerptParentHeight > this.maxEllipsisHeight) {
          const newHtml = this.deleteLastChars(excerptParent.innerHTML.replace(/&[a-z]*;/, ''), charsToDelete);
          const oldLength = excerptParent.innerHTML.length;
          excerptParent.innerHTML = newHtml;
          if (excerptParent.innerHTML.length === oldLength) {
            charsToDelete = charsToDelete * 2;
          }
          excerptParentHeight = excerptParent.getBoundingClientRect().height || this.lineHeight;
        }
        excerptParent.innerHTML = this.deleteLastChars(excerptParent.innerHTML, 4);
        excerptParent.innerHTML = `${excerptParent.innerHTML}...`;
      }
    },
    deleteLastChars(html, charsToDelete) {
      if (html.slice(-1) === '>') {
        // Replace empty tags
        html = html.replace(/<[a-zA-Z 0-9 "'=]*><\/[a-zA-Z 0-9]*>$/g, '');
      }
      html = html.replace(/<br>(\.*)$/g, '');

      charsToDelete = charsToDelete || 1;

      let newHtml = '';
      if (html.slice(-1) === '>') {
        // Delete last inner html char
        html = html.replace(/(<br>)*$/g, '');
        newHtml = html.replace(new RegExp(`([^>]{${charsToDelete}})(</)([a-zA-Z 0-9]*)(>)$`), '$2$3');
        newHtml = $('<div />').html(newHtml).html().replace(/&[a-z]*;/, '');
        if (newHtml.length === html.length) {
          newHtml = html.replace(new RegExp('([^>]*)(</)([a-zA-Z 0-9]*)(>)$'), '$2$3');
        }
      } else {
        newHtml = html.substring(0, html.trimRight().length - charsToDelete);
      }
      return newHtml;
    }
  }
};
</script>
