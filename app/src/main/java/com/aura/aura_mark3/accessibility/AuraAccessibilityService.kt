package com.aura.aura_mark3.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.content.*
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AuraAccessibilityService : AccessibilityService() {
    companion object {
        const val ACTION_PERFORM_CLICK = "com.aura.aura_mark3.PERFORM_CLICK"
        const val EXTRA_CLICK_LABEL = "click_label"

        const val ACTION_PERFORM_INPUT = "com.aura.aura_mark3.PERFORM_INPUT"
        const val EXTRA_INPUT_LABEL = "input_label"
        const val EXTRA_INPUT_TEXT = "input_text"

        const val ACTION_PERFORM_SCROLL = "com.aura.aura_mark3.PERFORM_SCROLL"
        const val EXTRA_SCROLL_LABEL = "scroll_label"
        const val EXTRA_SCROLL_DIRECTION = "scroll_direction"

        const val ACTION_ACTION_RESULT = "com.aura.aura_mark3.ACTION_RESULT"
        const val EXTRA_ACTION_RESULT_SUCCESS = "action_result_success"
        const val EXTRA_ACTION_RESULT_MESSAGE = "action_result_message"

        const val ACTION_PERFORM_GESTURE_AT_COORDS = "com.aura.aura_mark3.PERFORM_GESTURE_AT_CORDS"
        const val EXTRA_GESTURE_X = "gesture_x"
        const val EXTRA_GESTURE_Y = "gesture_y"

        const val ACTION_PERFORM_SCROLL_AT_COORDS = "com.aura.aura_mark3.PERFORM_SCROLL_AT_COORDS"
        const val ACTION_PERFORM_INPUT_AT_COORDS = "com.aura.aura_mark3.PERFORM_INPUT_AT_COORDS"
        const val ACTION_FOREGROUND_PACKAGE = "com.aura.aura_mark3.FOREGROUND_PACKAGE"
        const val EXTRA_FOREGROUND_PACKAGE = "foreground_package"
    }

    private fun sendActionResult(success: Boolean, message: String) {
        val intent = Intent(ACTION_ACTION_RESULT).apply {
            putExtra(EXTRA_ACTION_RESULT_SUCCESS, success)
            putExtra(EXTRA_ACTION_RESULT_MESSAGE, message)
        }
        sendBroadcast(intent)
    }

    private val actionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PERFORM_CLICK -> {
                    val label = intent.getStringExtra(EXTRA_CLICK_LABEL)
                    val root = rootInActiveWindow
                    if (!label.isNullOrBlank() && root != null) {
                        val target = findNodeByTextOrDesc(root, label)
                        if (target != null) {
                            Log.i("AURA_A11Y", "Clicked on '$label'")
                            performClick(target)
                            sendActionResult(true, "Clicked '$label'")
                        } else {
                            sendActionResult(false, "Label '$label' not found for click")
                        }
                    }
                }

                ACTION_PERFORM_INPUT -> {
                    val label = intent.getStringExtra(EXTRA_INPUT_LABEL)
                    val text = intent.getStringExtra(EXTRA_INPUT_TEXT)
                    val root = rootInActiveWindow
                    if (!label.isNullOrBlank() && !text.isNullOrBlank() && root != null) {
                        val target = findNodeByTextOrDesc(root, label)
                        if (target != null) {
                            performInput(target, text)
                            sendActionResult(true, "Entered '$text' into '$label'")
                        } else {
                            sendActionResult(false, "Label '$label' not found for input")
                        }
                    }
                }

                ACTION_PERFORM_SCROLL -> {
                    val label = intent.getStringExtra(EXTRA_SCROLL_LABEL)
                    val direction = intent.getStringExtra(EXTRA_SCROLL_DIRECTION)
                    val root = rootInActiveWindow
                    if (!label.isNullOrBlank() && !direction.isNullOrBlank() && root != null) {
                        val target = findNodeByTextOrDesc(root, label)
                        if (target != null) {
                            performScroll(target, direction)
                            sendActionResult(true, "Scrolled '$label' $direction")
                        } else {
                            sendActionResult(false, "Label '$label' not found for scroll")
                        }
                    }
                }

                ACTION_PERFORM_GESTURE_AT_COORDS -> {
                    val x = intent.getIntExtra(EXTRA_GESTURE_X, -1)
                    val y = intent.getIntExtra(EXTRA_GESTURE_Y, -1)
                    if (x >= 0 && y >= 0) performTapAt(x, y)
                }

                ACTION_PERFORM_SCROLL_AT_COORDS -> {
                    val x = intent.getIntExtra(EXTRA_GESTURE_X, -1)
                    val y = intent.getIntExtra(EXTRA_GESTURE_Y, -1)
                    val direction = intent.getStringExtra(EXTRA_SCROLL_DIRECTION) ?: "DOWN"
                    if (x >= 0 && y >= 0) performScrollGestureAt(x, y, direction)
                }

                ACTION_PERFORM_INPUT_AT_COORDS -> {
                    val x = intent.getIntExtra(EXTRA_GESTURE_X, -1)
                    val y = intent.getIntExtra(EXTRA_GESTURE_Y, -1)
                    val text = intent.getStringExtra(EXTRA_INPUT_TEXT) ?: ""
                    if (x >= 0 && y >= 0 && text.isNotBlank()) {
                        performTapAt(x, y)
                        Handler(mainLooper).postDelayed({
                            pasteText(text)
                        }, 300)
                    }
                }
            }
        }
    }

    private var lastForegroundPackage: String? = null

    @SuppressLint("ObsoleteSdkInt")
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i("AURA_A11Y", "AccessibilityService connected")

        // Create intent filter with all actions
        val filter = IntentFilter().apply {
            addAction(ACTION_PERFORM_CLICK)
            addAction(ACTION_PERFORM_INPUT)
            addAction(ACTION_PERFORM_SCROLL)
            addAction(ACTION_PERFORM_GESTURE_AT_COORDS)
            addAction(ACTION_PERFORM_SCROLL_AT_COORDS)
            addAction(ACTION_PERFORM_INPUT_AT_COORDS)
        }

        // Register action receiver with proper flags for Android 13+ security requirements
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+ (API 33+), explicitly specify RECEIVER_NOT_EXPORTED
            // since these broadcasts are only used internally within our app
            registerReceiver(actionReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            // For older Android versions, use the standard registration method
            // Note: RECEIVER_NOT_EXPORTED flag is not available/required on older versions
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(actionReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(actionReceiver)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val root = rootInActiveWindow
        // Broadcast foreground package name if changed
        event?.packageName?.toString()?.let { pkg ->
            if (pkg != lastForegroundPackage) {
                lastForegroundPackage = pkg
                val intent = Intent(ACTION_FOREGROUND_PACKAGE).apply {
                    putExtra(EXTRA_FOREGROUND_PACKAGE, pkg)
                }
                sendBroadcast(intent)
            }
        }
        if (root != null) {
            logNodeTree(root)
            val target = findNodeByTextOrDesc(root, "OK")
            target?.let {
                performClick(it)
            }
        }
    }

    private fun logNodeTree(node: AccessibilityNodeInfo, depth: Int = 0) {
        val indent = "  ".repeat(depth)
        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)
        Log.d("AURA_A11Y", "$indent- [${node.className}] text='${node.text}' desc='${node.contentDescription}' clickable=${node.isClickable} bounds=$rect")
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { logNodeTree(it, depth + 1) }
        }
    }

    private fun findNodeByTextOrDesc(node: AccessibilityNodeInfo, query: String): AccessibilityNodeInfo? {
        if (node.text?.toString()?.equals(query, true) == true || node.contentDescription?.toString()?.equals(query, true) == true) {
            return node
        }
        for (i in 0 until node.childCount) {
            val result = node.getChild(i)?.let { findNodeByTextOrDesc(it, query) }
            if (result != null) return result
        }
        return null
    }

    private fun performClick(node: AccessibilityNodeInfo) {
        if (node.isClickable) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            node.parent?.let { performClick(it) }
        }
    }

    private fun performInput(node: AccessibilityNodeInfo, text: String) {
        if (node.isEditable) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        } else {
            node.parent?.let { performInput(it, text) }
        }
    }

    private fun performScroll(node: AccessibilityNodeInfo, direction: String) {
        val action = when (direction.uppercase()) {
            "UP", "LEFT" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            "DOWN", "RIGHT" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            else -> null
        }
        if (action != null && node.isScrollable) {
            node.performAction(action)
        } else {
            node.parent?.let { performScroll(it, direction) }
        }
    }

    private fun performTapAt(x: Int, y: Int) {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun performScrollGestureAt(x: Int, y: Int, direction: String) {
        val (dx, dy) = when (direction.uppercase()) {
            "UP" -> 0 to -300
            "DOWN" -> 0 to 300
            "LEFT" -> -300 to 0
            "RIGHT" -> 300 to 0
            else -> 0 to 300
        }
        val path = Path().apply {
            moveTo(x.toFloat(), y.toFloat())
            lineTo((x + dx).toFloat(), (y + dy).toFloat())
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun pasteText(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("AURA_INPUT", text))
        rootInActiveWindow?.let { findAndPaste(it) }
    }

    private fun findAndPaste(node: AccessibilityNodeInfo) {
        if (node.isFocused && node.isEditable) {
            node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { findAndPaste(it) }
        }
    }

    override fun onInterrupt() {
        Log.w("AURA_A11Y", "AccessibilityService interrupted")
    }
}
