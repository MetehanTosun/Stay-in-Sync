module.exports = {
    resolve: {
      fallback: {
        "path": require.resolve("path-browserify"),
        "url": require.resolve("url/"),
        "util": require.resolve("util/"),
        "process": require.resolve("process/browser"),
        "stream": require.resolve("stream-browserify"),
        "http": require.resolve("stream-http"),
        "https": require.resolve("https-browserify"),
        "buffer": require.resolve("buffer/"),
        "fs": false,
        "os": false,
        "crypto": false
      }
    }
  };