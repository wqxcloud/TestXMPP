package com.huang.testxmpp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.huang.xmpp.Constant;
import com.huang.xmpp.XMChatMessageListener;
import com.huang.xmpp.XmppConnection;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jxmpp.jid.parts.Localpart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 登录页面
 * Created by Administrator on 2017/11/13.
 */

public class LoginActivity extends BaseActivity implements View.OnClickListener {
    private static String TAG = "LoginActivity";
    @BindView(R.id.login_name_et)
    EditText nameEt;
    @BindView(R.id.login_password_et)
    EditText passwordEt;
    @BindView(R.id.login_bt)
    Button loginBt;
    @BindView(R.id.login_register)
    Button login_register;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        init();
        nameEt.setText(Constant.ACCOUNT);
        passwordEt.setText(Constant.PASSWORD);
    }

    private void init() {
        // 创建数据库
        createDatabase();

        openConnection();
    }

    // 聊天监听类
    private void initChatManager() {
        // 单聊
        ChatManager cm = ChatManager.getInstanceFor(XmppConnection.getInstance().getConnection());
        cm.addIncomingListener(new XMChatMessageListener());
        cm.addOutgoingListener(new XMChatMessageListener());
    }

    // 群聊的聊天室列表
    public static List<MultiUserChat> multiUserChatList = new ArrayList<>();

    // 群聊，加入聊天室，并且监听聊天室消息
    private void initGroupChatManager() {
        for (String hostedRoomStr : Arrays.asList(Constant.roomNameList)) {
            MultiUserChat multiUserChat = XmppConnection.getInstance().joinMultiUserChat("哦哦等等", hostedRoomStr);
            if (multiUserChat == null) {
                return;
            }
            //multiUserChatList.add(multiUserChat);
        }
        for (MultiUserChat multiUserChat : multiUserChatList) {
            multiUserChat.addMessageListener(new XMChatMessageListener());
        }
    }

    // 创建数据库,创建表
    private void createDatabase() {
    }

    private void openConnection() {
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 1:
                        loginBt.setVisibility(View.VISIBLE);
                        initChatManager();// 聊天监听类
                        break;
                }
            }
        };

        // 打开连接
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!XmppConnection.getInstance().checkConnection()) {
                    if (XmppConnection.getInstance().openConnection()) {
                        Message message = new Message();
                        message.what = 1;
                        handler.sendMessage(message);
                    }
                }
            }
        }).start();
    }

    @OnClick({R.id.login_bt, R.id.login_register})
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.login_bt:
                String nameStr = nameEt.getText().toString();
                String passwordStr = passwordEt.getText().toString();
                if (!TextUtils.isEmpty(nameStr) && !TextUtils.isEmpty(passwordStr)) {
                    loginBt.setText("登录中……");
                    dealWithLogin(nameStr, passwordStr);
                }
                break;
            case R.id.login_register:
                String account = nameEt.getText().toString();
                String pwd = passwordEt.getText().toString();
                if(TextUtils.isEmpty(account) || TextUtils.isEmpty(pwd)){
                    Toast.makeText(LoginActivity.this,"账号密码不能为空",Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    XMPPConnection xmppConnection = XmppConnection.getInstance().getConnection();
                    if(xmppConnection != null && xmppConnection.isConnected()){

                        Log.i(TAG, "onClick: "+xmppConnection.isConnected());
                        Log.i(TAG, "onClick: "+xmppConnection.isSecureConnection());

                        AccountManager accountManager = AccountManager.getInstance(XmppConnection.getInstance().getConnection());
                        Log.i(TAG, "onClick: "+accountManager.supportsAccountCreation());
                        if(accountManager.supportsAccountCreation()){
                            accountManager.sensitiveOperationOverInsecureConnection(true);
                            Log.e(TAG, "onClick: account:"+account);
                            Log.e(TAG, "onClick: pwd:"+pwd);
                            accountManager.createAccount(Localpart.from(account), pwd);
                        }
                    }
                    Toast.makeText(LoginActivity.this,"注册成功",Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.i(TAG, "onClick: "+e);
                    Toast.makeText(LoginActivity.this,"注册失败:"+e.getMessage(),Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void dealWithLogin(final String account, final String password) {
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 1:
                        // 群聊，加入聊天室，并且监听聊天室消息
                        //initGroupChatManager();
                        loginBt.setText("登录成功");
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
//                        finish();
                        break;
                    case 2:
                        loginBt.setText("登录失败");
                        break;
                }
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (XmppConnection.getInstance().login(account, password)) {
                    Log.w("wangqx", "登录成功----");

                    Message message = new Message();
                    message.what = 1;
                    handler.sendMessage(message);
                } else {
                    Message message = new Message();
                    message.what = 2;
                    handler.sendMessage(message);
                }
            }
        }).start();
    }
}
