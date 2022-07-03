package com.irlab.login.utils;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import com.irlab.base.R;

public class ButtonListenerUtil {
    static IEditTextChangeListener mChangeListener;

    public static void setChangeListener(IEditTextChangeListener changeListener) {
        mChangeListener = changeListener;
    }

    // 根据传来的EditText是否为空设置按钮可以被点击
    public static void buttonEnabled(Button button, EditText... editTexts) {
        for (EditText editText : editTexts) {
            String content = editText.getText().toString();
            if (content.equals("") || content.length() < 3 || content.length() > 8) {
                button.setEnabled(false);
                return;
            }
        }
        button.setEnabled(true);
    }

    public static void buttonChangeColor(Context context, Button button, EditText ... editTexts) {
        // 创建工具类对象 把要改变颜色的Button先传过去
        textChangeListener textChangeListener = new textChangeListener(button);

        textChangeListener.addAllEditText(editTexts);//把所有要监听的EditText都添加进去
        // 接口回调 在这里拿到boolean变量 根据isHasContent的值决定 Button应该设置什么颜色
        ButtonListenerUtil.setChangeListener(new IEditTextChangeListener() {
            @Override
            public void textChange(boolean isHasContent) {
                if (isHasContent) {
                    button.setEnabled(true);
                    button.setBackgroundResource(R.drawable.btn_normal);
                    button.setTextColor(context.getResources().getColor(R.color.loginButtonTextFouse));
                } else {
                    button.setEnabled(false);
                    button.setBackgroundResource(R.drawable.btn_not_focus);
                    button.setTextColor(context.getResources().getColor(R.color.loginButtonText));
                }
            }
        });
    }

    // 检测输入框是否都输入了内容 从而改变按钮的是否可点击
    public static class textChangeListener {
        private Button button;
        private EditText[] editTexts;

        public textChangeListener(Button button) {
            this.button = button;
        }

        public textChangeListener addAllEditText(EditText... editTexts) {
            this.editTexts = editTexts;
            initEditListener();
            return this;
        }

        private void initEditListener() {
            //调用了遍历 ediText的方法
            for (EditText editText : editTexts) {
                editText.addTextChangedListener(new textChange());
            }
        }

        // edit输入的变化来改变按钮的是否点击
        private class textChange implements TextWatcher {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (checkAllEdit()) {
                    //所有EditText有值了
                    mChangeListener.textChange(true);
                    button.setEnabled(true);
                } else {
                    //所有EditText值为空
                    button.setEnabled(false);
                    mChangeListener.textChange(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        }

        //检查所有的edit是否输入了数据
        private boolean checkAllEdit() {
            for (EditText editText : editTexts) {
                if (!TextUtils.isEmpty(editText.getText() + "")
                        && editText.getText().toString().length() >= 3
                        && editText.getText().toString().length() <= 8) {
                    continue;
                } else {
                    return false;
                }
            }
            return true;
        }
    }
}
