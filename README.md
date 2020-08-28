# GifFileDecoder-android
## Animated GIF decoder and player for Android

Android ported version of https://code.google.com/archive/p/giffiledecoder/

Google Code is closing, project moved to https://sourceforge.net/projects/giffiledecoder/

This project includes code to decode and play animated GIFs on Android.

Significant files:

GifFileDecoder.java - decoder, converts gif files to in-memory images. Implements streaming: only one or two frames are kept in memory, so long videos can be played without memory concerns. Code is implemented in Java, but appears to work about as fast as WebView.

GifImageView.java - custom ImageView running decoder from a thread.

## Tasks completed
### Port to android studio
### code clean & upgrade to sdk version 29