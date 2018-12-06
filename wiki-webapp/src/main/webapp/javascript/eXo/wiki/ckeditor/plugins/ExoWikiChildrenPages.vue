<template>
  <div class="wiki-children-pages">
    <ul class="childrenPages">
      <li v-for="label in childrenPages"><a href="#">{{ label }}</a></li>
    </ul>
  </div>
</template>

<script>
import { getUserAvatar, getSpaceAvatar, getUserProfileLink, getSpaceProfileLink, escapeHtml } from '../chatServices';
import {chatConstants} from '../chatConstants';

export default {
  props: {
    /** Contact display name */
    depth: {
      type: Number,
      default: 1
    }
  },
  data : function() {
    return {
    };
  },
  computed: {
    childrenPages: function() {
      return ['Page 10', 'Page 11'];
    }
  },
  created() {
  },
  methods: {
  }
};
</script>