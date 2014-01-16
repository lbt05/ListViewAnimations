package com.haarman.listviewanimations.flipcard;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;

import java.util.List;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.BaseAdapter;

import com.haarman.listviewanimations.BaseAdapterDecorator;

public class FlipCardAdapter extends BaseAdapterDecorator implements ViewActionCallback {
  private final float ROTATION_DEPTH = 200.0f;
  private final int ROTATION_DURATION = 300;
  private FlipCardListViewTouchListener mFlipCardListViewTouchListener;
  private View flipedItemView;
  private int flipedItemPosition = -1;
  private int frontViewResourceID;
  private int backViewResourceID;
  private final int[] CLOCK_WISE_ROTATION_FRONT = {0, 90};
  private final int[] COUNTER_CLOCK_WISE_ROTATION_FRONT = {360, 270};
  private final int[] CLOCK_WISE_ROTATION_BACK = {270, 360};
  private final int[] COUNTER_CLOCK_WISE_ROTATION_BACK = {90, 0};
  private PreFlipAction actionBeforeFlip;
  private List<Integer> flipableItemViewTypes;

  public FlipCardAdapter(BaseAdapter baseAdapter, int frontViewResourceId, int backViewResourceId) {
    this(baseAdapter, frontViewResourceId, backViewResourceId, null);
  }

  public FlipCardAdapter(BaseAdapter baseAdapter, List<Integer> flipableItemViewTypes,
      int frontViewResourceId, int backViewResourceId) {
    this(baseAdapter, frontViewResourceId, backViewResourceId, null);
    this.flipableItemViewTypes = flipableItemViewTypes;
  }

  public FlipCardAdapter(BaseAdapter baseAdapter, int frontViewResourceId, int backViewResourceId,
      PreFlipAction preAction) {
    super(baseAdapter);
    this.frontViewResourceID = frontViewResourceId;
    this.backViewResourceID = backViewResourceId;
    this.actionBeforeFlip = preAction;
  }

  @Override
  public void onViewSwiped(View flipView, int flipPosition, boolean flipFromRight) {
    // TODO Callback API
    if (flipableItemViewTypes == null || flipableItemViewTypes.size() == 0
        || flipableItemViewTypes.contains(flipPosition)) {
      if (actionBeforeFlip != null) {
        actionBeforeFlip.prepareForFlip(flipView, flipPosition);
      }
      applyRotation(flipView, true, flipFromRight);
      boolean flipedFlag = flipPosition != flipedItemPosition;
      if (flipedItemView != null && flipedFlag) {
        applyRotation(flipedItemView, false, flipFromRight, ROTATION_DURATION * 2);
      } else if (flipedItemView != null) {
        applyRotation(flipedItemView, false, flipFromRight);
      }

      flipedItemPosition = flipedFlag ? flipPosition : -1;
      flipedItemView = flipedFlag ? flipView : null;
    }
  }

  @Override
  public void onViewSwiping(View flipView, int flipPosition, boolean flipFromRight, int progress) {
    // TODO Auto-generated method stub
  }

  @Override
  public void onListScrolled() {
    if (flipedItemView != null) {
      applyRotation(flipedItemView, false, true);
      flipedItemPosition = -1;
      flipedItemView = null;
    }
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {

    View v = super.getView(position, convertView, parent);
    View frontView = v.findViewById(frontViewResourceID);
    View backView = v.findViewById(backViewResourceID);
    if (position == flipedItemPosition) {
      if (backView != null) {
        backView.setVisibility(View.VISIBLE);
      }
      if (frontView != null) {
        frontView.setVisibility(View.GONE);
      }
    } else {
      if (backView != null) {
        backView.setVisibility(View.GONE);
      }
      if (frontView != null) {
        frontView.setVisibility(View.VISIBLE);
      }
    }
    return v;
  }

  @Override
  public void setAbsListView(AbsListView listView) {
    mFlipCardListViewTouchListener = new FlipCardListViewTouchListener(listView, this);
    mFlipCardListViewTouchListener
        .setIsParentHorizontalScrollContainer(isParentHorizontalScrollContainer());
    mFlipCardListViewTouchListener.setTouchChild(getTouchChild());
    super.setAbsListView(listView);
    listView.setOnTouchListener(mFlipCardListViewTouchListener);
    listView.setOnScrollListener(mFlipCardListViewTouchListener.makeScrollListener());
  }

  public void applyRotation(View v, boolean turnBack, boolean flipFromRight) {
    // Find the center of the container
    final float centerX = v.getWidth() / 2.0f;
    final float centerY = v.getHeight() / 2.0f;

    // Create a new 3D rotation with the supplied parameter
    // The animation listener is used to trigger the next animation
    int[] wise = getRotationWise(true, flipFromRight);
    final Rotate3dAnimation rotation =
        new Rotate3dAnimation(wise[0], wise[1], centerX, centerY, ROTATION_DEPTH, true);
    rotation.setDuration(ROTATION_DURATION);
    rotation.setFillAfter(false);
    rotation.setInterpolator(new AccelerateInterpolator());
    rotation.setAnimationListener(new DisplayNextView(turnBack, v, flipFromRight));

    v.startAnimation(rotation);
  }

  public void applyRotation(View v, boolean turnBack, boolean flipFromRight, int startOffset) {
    // Find the center of the container
    final float centerX = v.getWidth() / 2.0f;
    final float centerY = v.getHeight() / 2.0f;

    // Create a new 3D rotation with the supplied parameter
    // The animation listener is used to trigger the next animation
    int[] wise = getRotationWise(true, flipFromRight);
    final Rotate3dAnimation rotation =
        new Rotate3dAnimation(wise[0], wise[1], centerX, centerY, ROTATION_DEPTH, true);
    rotation.setDuration(ROTATION_DURATION);
    rotation.setStartOffset(startOffset);
    rotation.setFillAfter(false);
    rotation.setInterpolator(new AccelerateInterpolator());
    rotation.setAnimationListener(new DisplayNextView(turnBack, v, flipFromRight));

    v.startAnimation(rotation);
  }

  private final class DisplayNextView implements AnimationListener {
    private final boolean turnBack;
    private final View parentView;
    private final boolean flipFromRight;

    private DisplayNextView(boolean turnBack, View parent, boolean flipFromRight) {
      this.turnBack = turnBack;
      this.parentView = parent;
      this.flipFromRight = flipFromRight;
    }

    public void onAnimationStart(Animation animation) {}

    public void onAnimationEnd(Animation animation) {
      parentView.post(new SwapViews(parentView, turnBack, flipFromRight));
    }

    public void onAnimationRepeat(Animation animation) {}
  }

  private final class SwapViews implements Runnable {

    private final View parentView;
    private final View frontView;
    private final View backView;
    private final boolean turnBackFlag;
    private final boolean flipFromRight;

    public SwapViews(View parent, boolean turnBack, boolean flipFromRight) {
      this.turnBackFlag = turnBack;
      this.parentView = parent;
      frontView = parentView.findViewById(frontViewResourceID);
      backView = parentView.findViewById(backViewResourceID);
      this.flipFromRight = flipFromRight;
    }

    public void run() {
      final float centerX = parentView.getWidth() / 2.0f;
      final float centerY = parentView.getHeight() / 2.0f;
      Rotate3dAnimation rotation;

      if (turnBackFlag) {
        if (backView != null) {
          LayoutParams params = backView.getLayoutParams();
          params.height = parentView.getHeight();
          backView.setLayoutParams(params);
          backView.setVisibility(View.VISIBLE);
          backView.requestFocus();
        }
        if (frontView != null) {
          frontView.setVisibility(View.GONE);
        }
      } else {
        if (backView != null) {
          backView.setVisibility(View.GONE);
        }
        if (frontView != null) {
          frontView.setVisibility(View.VISIBLE);
          frontView.requestFocus();
        }

      }

      int[] wise = getRotationWise(false, flipFromRight);
      rotation = new Rotate3dAnimation(wise[0], wise[1], centerX, centerY, ROTATION_DEPTH, false);
      rotation.setDuration(ROTATION_DURATION);
      rotation.setFillAfter(true);
      rotation.setInterpolator(new DecelerateInterpolator());

      parentView.startAnimation(rotation);
    }
  }

  @Override
  public void notifyDataSetChanged() {
    super.notifyDataSetChanged();
    if (flipedItemView != null) {
      flipedItemPosition = -1;
      flipedItemView = null;
    }
  }

  private int[] getRotationWise(boolean frontView, boolean flipFromRight) {
    if (frontView && flipFromRight) {
      return CLOCK_WISE_ROTATION_FRONT;
    } else if (frontView && !flipFromRight) {
      return COUNTER_CLOCK_WISE_ROTATION_FRONT;
    } else if (!frontView && flipFromRight) {
      return CLOCK_WISE_ROTATION_BACK;
    } else {
      return COUNTER_CLOCK_WISE_ROTATION_BACK;
    }
  }

  @Override
  public void onViewActionCancel(View flipView) {
    // TODO Auto-generated method stub
    animate(flipView).translationX(0).alpha(1).setDuration(200).setListener(null);
  }
}
