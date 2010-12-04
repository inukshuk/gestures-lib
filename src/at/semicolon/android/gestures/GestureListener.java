package at.semicolon.android.gestures;

import android.content.Context;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.View.OnTouchListener;

/**
 * Handles touch events associated with a ZoomView.
 * 
 * TODO Replace the OnTouchListener with GesturesDetector. See git commit
 * 2876243ef61489d93582bc161c1c261440c4c0af (goban) for an implementation that does
 * not work because of an API bug where onScroll is not triggered after
 * onLongPress.
 * 
 */

public abstract class GestureListener implements OnTouchListener {

	private enum GestureType {
		UNDEFINED, LONGPRESS, SCROLL, DOUBLE_TAP_PENDING
	}

	private GestureType mGestureType = GestureType.UNDEFINED;
	
	private float mX;
	private float mY;
	private float mDownX;
	private float mDownY;
	private MotionEvent mDownEvent;
	private boolean mConsumed;

	private final int mScaledTouchSlop;
	private final int mLongPressTimeout;
	private final int mDoubleTapTimeout;
	private final int mTapTimeout;
	private final int mScaledMaximumFlingVelocity;

	private VelocityTracker mVelocityTracker;

	private final Runnable mLongPressRunnable = new Runnable() {
		public void run() {
			mGestureType = GestureType.LONGPRESS;
			mConsumed = onLongPress(mDownEvent);
		}
	};

	private final Runnable mTapRunnable = new Runnable() {
		public void run() {
			mGestureType = GestureType.DOUBLE_TAP_PENDING;
			mConsumed = false;
			onSingleTap(mDownEvent);
		}
	};

	public GestureListener(Context context) {
		mLongPressTimeout = ViewConfiguration.getLongPressTimeout();
		mDoubleTapTimeout = ViewConfiguration.getDoubleTapTimeout();
		mTapTimeout = ViewConfiguration.getTapTimeout();
		mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		mScaledMaximumFlingVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
		mConsumed = false;
	}


	abstract protected boolean onDown(MotionEvent e);

	abstract protected boolean onLongPress(MotionEvent e);

	abstract protected boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy);

	abstract protected boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy);

	abstract protected void onSingleTap(MotionEvent e);

	abstract protected void onDoubleTap(MotionEvent e);

	
	// Inherited from OnTouchListener

	public boolean onTouch(View v, MotionEvent e) {
		final int action = e.getAction();
		final float x = e.getX();
		final float y = e.getY();

		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(e);

		switch (action) {
			case MotionEvent.ACTION_DOWN:
				
				if (onDown(e) == true) {
					mConsumed = false;
					return true;
				}
				
				v.postDelayed(mLongPressRunnable, mLongPressTimeout);
				
				mDownEvent = e;
				mX = mDownX = x;
				mY = mDownY = y;
				
				break;
				
			case MotionEvent.ACTION_MOVE:
				final float dx = (x - mX) / v.getWidth();
				final float dy = (y - mY) / v.getHeight();

				switch (mGestureType) {
					case SCROLL:
					case LONGPRESS:
						if (mConsumed = false) {
							mConsumed = onScroll(mDownEvent, e, dx, dy);							
						}
						break;
					case UNDEFINED:
						final float scrollX = mDownX - x;
						final float scrollY = mDownY - y;

						final float dist = (float) Math.sqrt(scrollX * scrollX + scrollY * scrollY);

						if (dist >= mScaledTouchSlop) {
							v.removeCallbacks(mLongPressRunnable);
							mGestureType = GestureType.SCROLL;
						}
						break;
				}

				mX = x;
				mY = y;
				
				break;
				
			case MotionEvent.ACTION_UP:
				
				if (mGestureType == GestureType.SCROLL) {
					mVelocityTracker.computeCurrentVelocity(1000, mScaledMaximumFlingVelocity);
					mConsumed = onFling(mDownEvent, e, mVelocityTracker.getXVelocity(), mVelocityTracker.getYVelocity());
				}
				else {
					v.postDelayed(mTapRunnable, mTapTimeout);
					mConsumed = onFling(mDownEvent, e, 0, 0);
				}
				
				mVelocityTracker.recycle();
				mVelocityTracker = null;
				
				v.removeCallbacks(mLongPressRunnable);
				mGestureType = GestureType.UNDEFINED;
				
				break;

			default:
				
				mVelocityTracker.recycle();
				mVelocityTracker = null;
				
				v.removeCallbacks(mLongPressRunnable);
				
				mGestureType = GestureType.UNDEFINED;
				
				break;
		}

		return true;
	}

}
