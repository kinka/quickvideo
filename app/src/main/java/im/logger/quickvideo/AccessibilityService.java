package im.logger.quickvideo;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static im.logger.quickvideo.Home.PREFS_NAME;

public class AccessibilityService extends BaseAccessibilityService {

    public final static String MM_PNAME = "com.tencent.mm";
    private int hasVideo = 0;
    private boolean onekey = false;
    private boolean searching = false;
    private String nickname = "Kinka";
    boolean notTargetUI = false;

    private final static String Tag = "kk";
    @Override
    public void onAccessibilityEvent(final AccessibilityEvent event) {
        int eventType = event.getEventType();

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();

        switch (eventType) {
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                if (!onekey) break;

                if (rootNode == null) break;
                if (!rootNode.getPackageName().toString().equals(MM_PNAME)) break;

                if (notTargetUI) {
                    if (findButton(rootNode, "搜索", true) == null) {
                        performBackClick();
                        break;
                    }
                    else {
                        findEditText(rootNode, nickname);
                        notTargetUI = false;
                        break;
                    }
                }

                if (searching) {
                    List<AccessibilityNodeInfo> nodeList = rootNode.findAccessibilityNodeInfosByText(nickname);
                    if (nodeList.size() > 1) { // 出现了搜索结果
//                        Toast.makeText(getApplicationContext(), nodeList.size() + " size", Toast.LENGTH_SHORT).show();
                        AccessibilityNodeInfo target = nodeList.get(0);
                        target.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        searching = false;
                    }
                }

                if (hasVideo > 1) {
                    int count = 0;
                    List<AccessibilityNodeInfo> nodeList = rootNode.findAccessibilityNodeInfosByText("聊天结束");
                    count += nodeList.size();
                    nodeList = rootNode.findAccessibilityNodeInfosByText("聊天已取消");
                    count += nodeList.size();
                    nodeList = rootNode.findAccessibilityNodeInfosByText("对方拒绝了你");
                    count += nodeList.size();
                    if (count > 0) {
                        Toast.makeText(getApplicationContext(), "视频结束", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (hasVideo == 0) findButton(rootNode, "更多功能按钮，已折叠", true);
                    findMoreButton(rootNode);
                }
                break;

            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                String pkgName = event.getPackageName().toString();
                String clsName = event.getClassName().toString();
//                Toast.makeText(getApplicationContext(), clsName + "_" + pkgName, Toast.LENGTH_SHORT).show();

                if (pkgName.startsWith(MM_PNAME)) { // 微信的界面
                    Object[] vs = getValue();
                    boolean fromQuickVideo = (Boolean) vs[0];
                    nickname = (String) vs[1];

                    if (!onekey && fromQuickVideo) {
                        onekey = true;
                        setValue(false);

                        if (findButton(rootNode, "搜索", true) == null) {
                            notTargetUI = true;
                            performBackClick();
                        }
                    }

                    if (!searching && clsName.contains("com.tencent.mm.plugin.search.ui.FTSMainUI")) { // 搜索界面
                        findEditText(rootNode, nickname);
                        searching = true;
                    }
                } else {
                    hasVideo = 0;
                    onekey = false;
                }
                break;
        }
    }

    private void setValue(boolean v) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("onekey", v);

        editor.commit();
    }

    private Object[] getValue() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        Object[] res = new Object[]{
                settings.getBoolean("onekey", false),
                settings.getString("nickname", "")
        };

        return res;
    }

    private boolean findEditText(AccessibilityNodeInfo rootNode, String content) {
        int count = rootNode.getChildCount();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo nodeInfo = rootNode.getChild(i);
            if (nodeInfo == null) {
                continue;
            }
            CharSequence desc = nodeInfo.getContentDescription();
            String strDesc = null;
            if (desc != null) {
                strDesc = desc.toString();
                Log.i(Tag, strDesc);
            }


            if ("android.widget.EditText".equals(nodeInfo.getClassName())) {
                Bundle arguments = new Bundle();
                arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                        AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
                arguments.putBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN, true);
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                ClipData clip = ClipData.newPlainText("label", content);
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboardManager.setPrimaryClip(clip);
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                return true;
            }
            if (findEditText(nodeInfo, content)) {
                return true;
            }
        }
        return false;
    }

    ArrayList clsList = new ArrayList();
    private AccessibilityNodeInfo findButton(AccessibilityNodeInfo rootNode, String which, boolean shouldClick) {
        if (clsList.size() == 0) {
            clsList.add("android.widget.ImageButton");
            clsList.add("android.widget.RelativeLayout");
            clsList.add("android.widget.TextView");
            clsList.add("android.widget.EditText");
        }

        int count = rootNode.getChildCount();
        for (int i=0; i<count; i++) {
            AccessibilityNodeInfo nodeInfo = rootNode.getChild(i);
            if (nodeInfo == null) continue;

            CharSequence rawDesc = nodeInfo.getContentDescription();
            String desc = null;
            if (rawDesc != null) {
                desc = rawDesc.toString();
                Log.i(Tag, String.format("desc: %s=>%s", desc, nodeInfo.getClassName()));

                String clsName = nodeInfo.getClassName().toString();

                boolean shouldCheck = clsList.indexOf(clsName) > -1;
                if (shouldCheck && desc.contains(which)) {
//                    Toast.makeText(getApplicationContext(), desc, Toast.LENGTH_SHORT).show();
                    if (shouldClick) nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return nodeInfo;
                }
            }

            if (findButton(nodeInfo, which, shouldClick) != null) return nodeInfo;
        }

        return null;
    }

    private boolean findMoreButton(AccessibilityNodeInfo rootNode) {
        List<AccessibilityNodeInfo> nodeList = rootNode.findAccessibilityNodeInfosByText("视频聊天");
        if (nodeList.size() > 0) {
            AccessibilityNodeInfo videoChat = nodeList.get(0);
            AccessibilityNodeInfo parent = videoChat.getParent();
            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            hasVideo += 1;
            return true;
        }

        return false;
    }
}
