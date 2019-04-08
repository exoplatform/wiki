const path = require('path');
const merge = require('webpack-merge');
const webpackCommonConfig = require('./webpack.common.js');

// change the server path to your server location path
const exoServerPath = "/home/thomas/exoplatform/bundles/plf-community-tomcat-standalone-5.3.x-wiki-editor-20190408.121601-1/platform-community-5.3.x-wiki-editor-SNAPSHOT/";

let config = merge(webpackCommonConfig, {
  mode: 'development',
  output: {
    path: path.resolve(__dirname, exoServerPath + 'webapps/wiki/javascript/eXo/wiki/ckeditor'),
  },

  // Useful for debugging
  devtool: 'eval-source-map'
});


module.exports = config;
