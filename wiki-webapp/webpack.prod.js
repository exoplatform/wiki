const path = require('path');
const merge = require('webpack-merge');
const webpackCommonConfig = require('./webpack.common.js');

let config = merge(webpackCommonConfig, {
  output: {
    path: path.resolve(__dirname, './target/wiki/javascript/eXo/wiki/ckeditor'),
    filename: '[name].bundle.js',
    library: '[name]',
    libraryTarget: 'amd'
  }
});

module.exports = config;