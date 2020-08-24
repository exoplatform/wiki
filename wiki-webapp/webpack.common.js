'use strict';

const TerserPlugin = require('terser-webpack-plugin');

const { styles } = require( '@ckeditor/ckeditor5-dev-utils' );

module.exports = {
  entry: {
    wikiCkeditor: './src/main/webapp/javascript/eXo/wiki/ckeditor/wikiCkeditor.js',
    pageContent: './src/main/webapp/javascript/eXo/wiki/pageContent.js',
    wikiSearchCard: './src/main/webapp/vue-app/wikiSearch/main.js'
  },
  output: {
    filename: '[name].bundle.js',
    libraryTarget: 'amd'
  },
  module: {
    rules: [
      {
        // Or /ckeditor5-[^/]+\/theme\/icons\/[^/]+\.svg$/ if you want to limit this loader
        // to CKEditor 5 icons only.
        test: /\.svg$/,

        use: [ 'raw-loader' ]
      },
      {
        // Or /ckeditor5-[^/]+\/theme\/[^/]+\.css$/ if you want to limit this loader
        // to CKEditor 5 theme only.
        test: /\.css$/,
        use: [
          {
            loader: 'style-loader',
            options: {
              singleton: true
            }
          },
          {
            loader: 'postcss-loader',
            options: styles.getPostCssConfig( {
              themeImporter: {
                themePath: require.resolve( '@ckeditor/ckeditor5-theme-lark' )
              },
              minify: true
            } )
          },
        ]
      },
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: [
          'babel-loader',
          'eslint-loader',
        ]
      },
      {
        test: /\.vue$/,
        use: [
          'vue-loader',
          'eslint-loader',
        ]
      }
    ]
  },
  optimization: {
    minimize: true,
    minimizer: [
      new TerserPlugin({
        terserOptions: { output: { ascii_only: true } }
      })
    ],
  },

  // By default webpack logs warnings if the bundle is bigger than 200kb.
  performance: { hints: false }
};