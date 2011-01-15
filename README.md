gestures-lib
============

An Android library that is basically a replacement for Android's current
(GestureDetector)[http://developer.android.com/reference/android/view/GestureDetector.html].

The current version is flawed in that it does not fire any `scroll` events
once a `longpress` events has been triggered thus making it impossible to
implement functionality that depends on a combination of `scroll` and `longpress`
(e.g., a one-finger zoom control).

This library works around this issue.


Usage
-----

To use this library, simply clone the git repository:

    git clone git://github.com/inukshuk/gestures-lib.git

and include a reference to it in the `default.properties` of your Android
project:

    android.library.reference=<path/to>/gestures-lib

You can then use the GestureListener in one of your Views. For instance:

    import at.semicolon.android.gestures.GestureListener
  
    [...]
    
    public class MyView extends View {
    
      private MyGestureListener mGestureListener;
     
      [...]
    
      setOnTouchListener(mGestureListener);
    
      private class MyGestureListener extends GestureListener {
    
        public MyGestureListener(Context context) {
          super(context);
        }
      
        @Override
        protected boolean onDown(MotionEvent e) {

        }

        @Override
        protected void onUp(MotionEvent e){

        }

        @Override
        protected boolean onLongPress(float x, float y) {
      
        }

        @Override
        protected boolean onScroll(float x, float y, MotionEvent e, float dx, float dy) {
        
        }

        @Override
        protected boolean onFling(float x, float y, MotionEvent e, float vx, float vy) {
      
        }

        @Override
        protected void onSingleTap(float x, float y) {
      
        }

        @Override
        protected void onDoubleTap(float x, float y) {
      
        }
      
      }    
    }
