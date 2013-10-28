package com.haarman.listviewanimations.flipcard;

import android.view.View;

public interface PreFlipAction {
	public void prepareForFlip(View flipView, int flipPosition);
}