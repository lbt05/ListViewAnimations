package com.haarman.listviewanimations.flipcard;

import android.view.View;

public interface ViewActionCallback {
	void onViewSwiped(View flipView, int flipPosition, boolean flipFromRight);

	void onViewSwiping(View flipView, int flipPosition, boolean flipFromRight,
			int progress);

	void onViewActionCancel(View flipView);

	void onListScrolled();
}
