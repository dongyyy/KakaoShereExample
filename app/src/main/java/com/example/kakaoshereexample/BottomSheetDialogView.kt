package com.example.kakaoshereexample

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.*
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog


abstract class BottomSheetDialogView(context: Context, useAnimation : Boolean = true, style : Int = R.style.AppTheme_BottomSheetDialog) : BottomSheetDialog(context, style) {

    abstract fun inflateView(inflater : LayoutInflater) : View

    var contentsView : View
    var needRequestLayout = true

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            if (useAnimation) {
                it.setWindowAnimations(R.style.BottomSheetDialogAnimation)
            }
            it.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        }

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        contentsView = inflateView(inflater)
        setContentView(contentsView)

        if (needRequestLayout) {
            contentsView.requestLayout()
            contentsView.getViewTreeObserver().addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    contentsView.getViewTreeObserver().removeOnGlobalLayoutListener(this)
                    try {
                        val d = this@BottomSheetDialogView as BottomSheetDialog
                        val bottomSheet = d.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                        val bottomSheetBehavior = BottomSheetBehavior.from<FrameLayout?>(bottomSheet!!)
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

                        val viewHeight = contentsView.height
                        if (viewHeight != -1) {
                            val coordinatorLayout = bottomSheet.getParent()
                            bottomSheet.getLayoutParams().height = viewHeight
                            bottomSheetBehavior.peekHeight = viewHeight
                            coordinatorLayout.getParent().requestLayout()
                        }
                    } catch (e : Exception) {
                        e.printStackTrace()
                    }
                }
            })
        }

        // 키보드가 뷰를 다 밀고 올라가야 하는 다이얼로그는 style 을 AppTheme_InputTypeBottomSheetDialog로 지정하기
        // <item name="android:windowFullscreen">true</item> 옵션이 있는경우 밀고 올라갈수가 없다.
        dismissWithAnimation = true
    }

    fun requestContentsViewLayout(height: Int) {
        try {
            val d = this@BottomSheetDialogView as BottomSheetDialog
            val bottomSheet = d.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            val bottomSheetBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from<FrameLayout?>(bottomSheet!!)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

            val viewHeight = contentsView.height + height
            if (viewHeight != -1) {
                bottomSheet.getLayoutParams().height = viewHeight
                bottomSheetBehavior.peekHeight = viewHeight
            }
        } catch (e : Exception) {
            e.printStackTrace()
        }
    }

    fun updateHeight(height: Int) {
        try {
            contentsView.post {
                val d = this@BottomSheetDialogView as BottomSheetDialog
                val bottomSheet = d.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                val bottomSheetBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from<FrameLayout?>(bottomSheet!!)

                var viewHeight = contentsView.height
                viewHeight = viewHeight + height

                bottomSheet.getLayoutParams().height = viewHeight
                bottomSheetBehavior.peekHeight = viewHeight
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                bottomSheet.requestLayout()
            }
        } catch (e : Exception) {
            e.printStackTrace()
        }
    }

    open fun showDialog(cancelable : Boolean = true, cancelListener : DialogInterface.OnCancelListener? = null) {
        setCanceledOnTouchOutside(cancelable)
        setCancelable(cancelable)

        if (cancelable) {
            setOnCancelListener(cancelListener)
        }

        show()
    }

}