package com.haarman.listviewanimations.flipcard;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;

import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;

import com.haarman.listviewanimations.itemmanipulation.SwipeOnTouchListener;

public class FlipCardListViewTouchListener implements SwipeOnTouchListener {
	// Cached ViewConfiguration and system-wide constant values
	private int mSlop;
	private int mMinFlingVelocity;
	private int mMaxFlingVelocity;
	private long mAnimationTime;
	// Fixed properties
	private AbsListView mListView;
	private Callback mCallback;
	private int mViewWidth = 1; // 1 and not 0 to prevent dividing by zero

	// Transient properties
	private float mDownX;
	private float mDownY;
	private boolean mSwiping;
	private VelocityTracker mVelocityTracker;
	private int mDownPosition;
	private View mDownView;
	private boolean mPaused;
	private boolean mDisallowSwipe;

	private boolean mIsParentHorizontalScrollContainer;
	private int mResIdOfTouchChild;
	private boolean mTouchChildTouched;

	public interface Callback {

		void onViewSwiped(View flipView, int flipPosition, boolean flipFromRight);

		void onListScrolled();
	}

	public FlipCardListViewTouchListener(AbsListView listView, Callback callback) {
		ViewConfiguration vc = ViewConfiguration.get(listView.getContext());
		mSlop = vc.getScaledTouchSlop();
		mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
		mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
		mAnimationTime = listView.getContext().getResources()
				.getInteger(android.R.integer.config_shortAnimTime);
		mListView = listView;
		mCallback = callback;
	}

	public void setEnabled(boolean enabled) {
		mPaused = !enabled;
	}

	public AbsListView.OnScrollListener makeScrollListener() {
		return new AbsListView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView absListView,
					int scrollState) {
				setEnabled(scrollState != AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
				if (mPaused) {
					mCallback.onListScrolled();
				}
				if (scrollState != AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
					mDisallowSwipe = true;
				}

			}

			@Override
			public void onScroll(AbsListView absListView, int i, int i1, int i2) {
			}
		};
	}

	@Override
	public boolean onTouch(View view, MotionEvent motionEvent) {
		if (mViewWidth < 2) {
			mViewWidth = mListView.getWidth();
		}
		switch (motionEvent.getActionMasked()) {
		case MotionEvent.ACTION_DOWN: {
			mDisallowSwipe = false;
			if (mPaused) {
				return false;
			}

			// Find the child view that was touched (perform a hit test)
			Rect rect = new Rect();
			int childCount = mListView.getChildCount();
			int[] listViewCoords = new int[2];
			mListView.getLocationOnScreen(listViewCoords);
			int x = (int) motionEvent.getRawX() - listViewCoords[0];
			int y = (int) motionEvent.getRawY() - listViewCoords[1];
			View child;
			for (int i = 0; i < childCount; i++) {
				child = mListView.getChildAt(i);
				child.getHitRect(rect);
				if (rect.contains(x, y)) {
					mDownView = child;
					break;
				}
			}

			if (mDownView != null) {
				mDownX = motionEvent.getRawX();
				mDownY = motionEvent.getRawY();

				mTouchChildTouched = !mIsParentHorizontalScrollContainer
						&& (mResIdOfTouchChild == 0);

				if (mResIdOfTouchChild != 0) {
					mIsParentHorizontalScrollContainer = false;

					final View childView = mDownView
							.findViewById(mResIdOfTouchChild);
					if (childView != null) {
						final Rect childRect = getChildViewRect(mListView,
								childView);
						if (childRect.contains((int) mDownX, (int) mDownY)) {
							mTouchChildTouched = true;
							mListView.requestDisallowInterceptTouchEvent(true);
						}
					}
				}

				if (mIsParentHorizontalScrollContainer) {
					// Do it now and don't wait until the user moves more than
					// the slop factor.
					mTouchChildTouched = true;
					mListView.requestDisallowInterceptTouchEvent(true);
				}

				mDownY = motionEvent.getRawY();

				mDownPosition = mListView.getPositionForView(mDownView);

				mVelocityTracker = VelocityTracker.obtain();
				mVelocityTracker.addMovement(motionEvent);
			}
			view.onTouchEvent(motionEvent);
			return true;
		}

		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL: {
			mDisallowSwipe = false;
			if (mVelocityTracker == null) {
				break;
			}

			float deltaX = motionEvent.getRawX() - mDownX;
			mVelocityTracker.addMovement(motionEvent);
			mVelocityTracker.computeCurrentVelocity(1000);
			float velocityX = Math.abs(mVelocityTracker.getXVelocity());
			float velocityY = Math.abs(mVelocityTracker.getYVelocity());
			boolean flipped = false;
			boolean flipFromRight = false;
			final float absDeltaX = Math.abs(deltaX);
			if (absDeltaX > mViewWidth / 2) {
				flipped = true;
				flipFromRight = deltaX > 0;
			} else if (mMinFlingVelocity <= velocityX
					&& velocityX <= mMaxFlingVelocity && velocityY < velocityX
					&& absDeltaX > mSlop) {
				flipped = true;
				flipFromRight = mVelocityTracker.getXVelocity() > 0;
			}
			if (flipped) {
				final View downView = mDownView;
				final int downPosition = mDownPosition;
				mCallback.onViewSwiped(downView, downPosition, flipFromRight);
			} else {
				// cancel
				animate(mDownView).translationX(0).alpha(1)
						.setDuration(mAnimationTime).setListener(null);
			}

			mVelocityTracker.recycle();
			mVelocityTracker = null;
			mDownX = 0;
			mDownView = null;
			mDownPosition = ListView.INVALID_POSITION;
			mSwiping = false;
			return flipped;
		}

		case MotionEvent.ACTION_MOVE: {
			if (mVelocityTracker == null || mPaused) {
				break;
			}

			mVelocityTracker.addMovement(motionEvent);
			float deltaX = motionEvent.getRawX() - mDownX;
			float deltaY = motionEvent.getRawY() - mDownY;
			final float centerX = mDownView.getWidth() / 2.0f;
			final float centerY = mDownView.getHeight() / 2.0f;
			if (mTouchChildTouched && !mDisallowSwipe
					&& Math.abs(deltaX) > mSlop
					&& Math.abs(deltaX) > Math.abs(deltaY)) {
				mSwiping = true;
				mListView.requestDisallowInterceptTouchEvent(true);

				// Cancel ListView's touch (un-highlighting the item)
				MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
				cancelEvent
						.setAction(MotionEvent.ACTION_CANCEL
								| (motionEvent.getActionIndex() << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
				mListView.onTouchEvent(cancelEvent);
				cancelEvent.recycle();
			}

			if (mSwiping) {
				/*
				 * no idea how to implements rotationY with deltaY Ask for help
				 * from community
				 */

				return true;
			}
			return false;
		}

		}
		return false;
	}

	@Override
	public boolean isSwiping() {
		return mSwiping;
	}

	private Rect getChildViewRect(View parentView, View childView) {
		final Rect childRect = new Rect(childView.getLeft(),
				childView.getTop(), childView.getRight(), childView.getBottom());
		if (parentView == childView) {
			return childRect;

		}

		ViewGroup parent;
		while ((parent = (ViewGroup) childView.getParent()) != parentView) {
			childRect.offset(parent.getLeft(), parent.getTop());
			childView = parent;
		}

		return childRect;
	}

	void setIsParentHorizontalScrollContainer(
			boolean isParentHorizontalScrollContainer) {
		mIsParentHorizontalScrollContainer = (mResIdOfTouchChild == 0) ? isParentHorizontalScrollContainer
				: false;
	}

	void setTouchChild(int childResId) {
		mResIdOfTouchChild = childResId;
		if (childResId != 0) {
			setIsParentHorizontalScrollContainer(false);
		}
	}
}
