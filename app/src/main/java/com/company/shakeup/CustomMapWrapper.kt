package com.company.shakeup
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

class CustomMapWrapper(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Block parent ScrollView from intercepting touches
        parent.requestDisallowInterceptTouchEvent(true)
        return super.onInterceptTouchEvent(ev)
    }
}