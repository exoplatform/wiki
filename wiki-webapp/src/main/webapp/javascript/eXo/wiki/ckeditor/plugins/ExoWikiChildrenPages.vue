<template>
  <div class="wiki-children-pages">
    <ul class="childrenPages">
      <li v-for="label in childrenPages" :key="label">
        <a href="#">
          {{ label }}
        </a>
      </li>
    </ul>
  </div>
</template>

<script>
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
  methods: {
  }
};
</script>