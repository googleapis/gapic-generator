const path = require('path');

module.exports = {
    entry: './index.js',
    output: {
      library: "showcaseTest",
      filename: "./main.js"
    },
    node: {
      child_process: 'empty',
      fs: 'empty',
      crypto: 'empty',
    },
    resolve: {
      extensions: ['.js', '.json']
    },
    module: {
      rules: [
        {
          test: /node_modules[\\\/]@grpc[\\\/]grpc-js/,
          use: 'null-loader'
        },
        {
          test: /node_modules[\\\/]grpc/,
          use: 'null-loader'
        },
        {
          test: /node_modules[\\\/]retry-request/,
          use: 'null-loader'
        },
        {
          test: /node_modules[\\\/]https-proxy-agent/,
          use: 'null-loader'
        },
        {
          test: /node_modules[\\\/]gtoken/,
          use: 'null-loader'
        },
      ]
    },
    mode: 'production'
}
