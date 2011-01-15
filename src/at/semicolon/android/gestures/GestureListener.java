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
		UNDEFINED, LONGPRESS, SCROLL, DOUBLE_TAP_PENDING, DOUBLE_TAP
	}

	private GestureType mGestureType = GestureType.UNDEFINED;

	private float mX;
	private float mY;
	private float mDownX;
	private float mDownY;
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
			mConsumed = onLongPress(mDownX, mDownY);
		}
	};

	private final Runnable mTapRunnable = new Runnable() {
		public void run() {
			mGestureType = GestureType.UNDEFINED;
			onSingleTap(mDownX, mDownY);
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

	abstract protected void onUp(MotionEvent e);

	abstract protected boolean onLongPress(float x, float y);

	abstract protected boolean onScroll(float x, float y, MotionEvent e, float dx, float dy);

	abstract protected boolean onFling(float x, float y, MotionEvent e, float vx, float vy);

	abstract protected void onSingleTap(float x, float y);

	abstract protected void onDoubleTap(float x, float y);


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

				mConsumed = onDown(e);

				// return if onDown consumes the event
				if (mConsumed) {
					mVelocityTracker.recycle();
					mVelocityTracker = null;
					mGestureType = GestureType.UNDEFINED;
					return true;
				}

				// check if this is a double tap
				if (mGestureType == GestureType.DOUBLE_TAP_PENDING) {
					v.removeCallbacks(mTapRunnable);
					mGestureType = GestureType.DOUBLE_TAP;
					mConsumed = false;
				}
				else {
					v.postDelayed(mLongPressRunnable, mLongPressTimeout);

					mX = mDownX = x;
					mY = mDownY = y;
				}
				break;

			case MotionEvent.ACTION_MOVE:
				final float dx = (x - mX) / v.getWidth();
				final float dy = (y - mY) / v.getHeight();

				switch (mGestureType) {
					case SCROLL:
					case LONGPRESS:
						if (mConsumed == false) {
							mConsumed = onScroll(mDownX, mDownY, e, dx, dy);							
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

				onUp(e);

				switch (mGestureType) {
					case SCROLL:
						if (mConsumed == false) {
							mVelocityTracker.computeCurrentVelocity(1000, mScaledMaximumFlingVelocity);
							mConsumed = onFling(mDownX, mDownY, e, mVelocityTracker.getXVelocity(), mVelocityTracker.getYVelocity());
						}
						mGestureType = GestureType.UNDEFINED;
						break;

					case DOUBLE_TAP:
						onDoubleTap(mDownX, mDownY);
						// fall through	
					case LONGPRESS:
						mGestureType = GestureType.UNDEFINED;
						mConsumed = false;
						break;
						
					default:
						v.postDelayed(mTapRunnable, mDoubleTapTimeout);
						mGestureType = GestureType.DOUBLE_TAP_PENDING;
						break;
				}

				mVelocityTracker.recycle();
				mVelocityTracker = null;
				
				v.removeCallbacks(mLongPressRunnable);

				break;

			default:

				// clean up
				mVelocityTracker.recycle();
				mVelocityTracker = null;
				
				v.removeCallbacks(mLongPressRunnable);
				v.removeCallbacks(mTapRunnable);

				mGestureType = GestureType.UNDEFINED;

				break;
		}

		return true;
	}

}
