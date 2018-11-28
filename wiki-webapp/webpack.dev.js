const path = require('path');
const merge = require('webpack-merge');
const webpackCommonConfig = require('./webpack.common.js');

// change the server path to your server location path
const exoServerPath = "/home/thomas/exoplatform/bundles/plfent-5.2.x-wiki-editor-20181118.003100-23/platform-5.2.x-wiki-editor-SNAPSHOT/";

let config = merge(webpackCommonConfig, {
  output: {
    path: path.resolve(__dirname, exoServerPath + 'webapps/wiki/javascript/eXo/wiki/ckeditor'),
    filename: '[name].bundle.js',
    library: 'wikiCkeditor',
    libraryTarget: 'umd'
  },

  // Useful for debugging
  devtool: 'source-map'
});


module.exports = config;
