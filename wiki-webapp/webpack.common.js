'use strict';

const { styles } = require( '@ckeditor/ckeditor5-dev-utils' );

module.exports = {
  entry: {
    wikiCkeditor: './src/main/webapp/javascript/eXo/wiki/ckeditor/wikiCkeditor.js'
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
      }
    ]
  },

  // By default webpack logs warnings if the bundle is bigger than 200kb.
  performance: { hints: false }
};