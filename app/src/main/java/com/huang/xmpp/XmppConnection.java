package com.huang.xmpp;

import android.graphics.drawable.Drawable;
import android.util.Log;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.filter.ToMatchesFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterGroup;
import org.jivesoftware.smack.sasl.core.SCRAMSHA1Mechanism;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.muc.HostedRoom;
import org.jivesoftware.smackx.muc.MucEnterConfiguration;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.offline.OfflineMessageManager;
import org.jivesoftware.smackx.search.ReportedData;
import org.jivesoftware.smackx.search.UserSearchManager;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.impl.DomainpartJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * XmppConnection 工具类
 */
public class XmppConnection {
    private static String TAG = "XmppConnection";
    private int SERVER_PORT = Constant.OPENFIRE_PORT;
    private String SERVER_HOST = Constant.OPENFIRE_IP;
    private String SERVER_NAME = Constant.OPENFIRE_NAME;
    private AbstractXMPPConnection connection = null;
    private static XmppConnection xmppConnection = new XmppConnection();
    private XMConnectionListener connectionListener;

    /**
     * 单例模式
     *
     * @return XmppConnection
     */
    public synchronized static XmppConnection getInstance() {
        return xmppConnection;
    }

    static{
        ProviderManager.addExtensionProvider(Received.NAME, Received.NAME_SPACE, new ReceivedProvider());
    }
    /**
     * 创建连接
     */
    public AbstractXMPPConnection getConnection() {
        if (connection == null) {
            // 开线程打开连接，避免在主线程里面执行HTTP请求
            // Caused by: android.os.NetworkOnMainThreadException
            new Thread(new Runnable() {
                @Override
                public void run() {
                    openConnection();
                }
            }).start();
        }
        return connection;
    }

    /**
     * 判断是否已连接
     */
    public boolean checkConnection() {
        return null != connection && connection.isConnected();
    }

    /**
     * 打开连接
     */
    public boolean openConnection() {
        try {
            Log.i("wangqx","openConnection");
            if (null == connection || !connection.isAuthenticated()) {
                SmackConfiguration.DEBUG = true;
                XMPPTCPConnectionConfiguration.Builder config = XMPPTCPConnectionConfiguration.builder();
                //设置openfire主机IP
                config.setHostAddress(InetAddress.getByName(SERVER_HOST));
                //设置openfire服务器名称
                config.setXmppDomain(SERVER_NAME);

                //设置端口号：默认5222
                config.setPort(SERVER_PORT);
                //禁用SSL连接
                config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
                //设置Debug
                config.enableDefaultDebugger();
                //设置离线状态
                config.setSendPresence(false);
                //设置开启压缩，可以节省流量
                config.setCompressionEnabled(true);
                config.setResource("TestXMPP");
                //需要经过同意才可以添加好友
                Roster.setDefaultSubscriptionMode(Roster.SubscriptionMode.manual);
                Roster.setRosterLoadedAtLoginDefault(true);
                // 将相应机制隐掉
                //SASLAuthentication.blacklistSASLMechanism("SCRAM-SHA-1");
                //SASLAuthentication.blacklistSASLMechanism("DIGEST-MD5");

                connection = new XMPPTCPConnection(config.build());
                // 添加连接监听
                connectionListener = new XMConnectionListener();
                connection.addConnectionListener(connectionListener);
                connection.connect();// 连接到服务器
                return true;
            }
        } catch (XMPPException | SmackException | IOException | InterruptedException xe) {
            xe.printStackTrace();
            Log.i("wangqx","xxxe="+xe);
            connection = null;
        }
        return false;
    }
    StanzaListener stanzaListener = new StanzaListener(){
        @Override
        public void processStanza(Stanza packet) throws SmackException.NotConnectedException, InterruptedException, SmackException.NotLoggedInException
        {
            Log.i("wangqx","processStanza");
        }
    };

    // 群聊的聊天室列表
    public static List<MultiUserChat> multiUserChatList = new ArrayList<>();

    // 群聊，加入聊天室，并且监听聊天室消息
    private void initGroupChatManager()
    {
        Log.i("XMConnectionListener","initGroupChatManager");
        for (String hostedRoomStr : Arrays.asList(Constant.roomNameList))
        {
            MultiUserChat multiUserChat = XmppConnection.getInstance().joinMultiUserChat("100001" + hostedRoomStr, hostedRoomStr);
            if (multiUserChat == null)
            {
                return;
            }
            multiUserChatList.add(multiUserChat);
        }
        for (MultiUserChat multiUserChat : multiUserChatList)
        {
            multiUserChat.addMessageListener(new XMChatMessageListener());
        }
    }

    /**
     * 关闭连接
     */
    public void closeConnection() {
        if (connection != null) {
            // 移除连接监听
            connection.removeConnectionListener(connectionListener);
            if (connection.isConnected())
                connection.disconnect();
            connection = null;
        }
        Log.i("XmppConnection", "关闭连接");
    }

    /**
     * 判断连接是否通过了身份验证
     * 即是否已登录
     *
     * @return
     */
    public boolean isAuthenticated() {
        return connection != null && connection.isConnected() && connection.isAuthenticated();
    }

    /**
     * 登录
     *
     * @param account  登录帐号
     * @param password 登录密码
     * @return true登录成功
     */
    public boolean login(String account, String password) {
        //SCRAMSHA1Mechanism scramsha1Mechanism = new SCRAMSHA1Mechanism();
        //SASLAuthentication.registerSASLMechanism(scramsha1Mechanism);
        try {
            if (getConnection() == null)
                return false;
            Log.i("wangqx", "login  account="+account);
            Log.i("wangqx", "login  password="+password);
            connectionListener.setAccountAndPassword(account, password);
            getConnection().login(account, password);
    //        connection.addAsyncStanzaListener(stanzaListener,new ToMatchesFilter(connection.getUser().asEntityBareJid(),true));
            Log.v("wangqx","登录成功 connection.isAuthenticated()="+connection.isAuthenticated());
            // 更改在线状态
            setPresence(0);
            return true;
        } catch (XMPPException | IOException | SmackException | InterruptedException xe) {
            Log.i("wangqx","192 xe="+xe);
            xe.printStackTrace();

        }
        return false;
    }

    /**
     * 注册
     *
     * @param account  注册帐号
     * @param password 注册密码
     * @return 1、注册成功 0、注册失败
     */
    public String register(String account, String password) {
        if (getConnection() == null)
            return "0";
        try {
            AccountManager.getInstance(connection).sensitiveOperationOverInsecureConnection(true);
            AccountManager.getInstance(connection).createAccount(Localpart.from(account), password);
        } catch (XmppStringprepException | InterruptedException | XMPPException | SmackException e) {
            e.printStackTrace();
            return "0";
        }
        return "1";
    }

    /**
     * 更改用户状态
     */
    public void setPresence(int code) {
        XMPPConnection con = getConnection();
        if (con == null)
            return;
        Presence presence;
        try {
            switch (code) {
                case 0:
                    presence = new Presence(Presence.Type.available);
                    con.sendStanza(presence);
                    Log.v("state", "设置在线");
                    break;
                case 1:
                    presence = new Presence(Presence.Type.available);
                    presence.setMode(Presence.Mode.chat);
                    con.sendStanza(presence);
                    Log.v("state", "设置Q我吧");
                    break;
                case 2:
                    presence = new Presence(Presence.Type.available);
                    presence.setMode(Presence.Mode.dnd);
                    con.sendStanza(presence);
                    Log.v("state", "设置忙碌");
                    break;
                case 3:
                    presence = new Presence(Presence.Type.available);
                    presence.setMode(Presence.Mode.away);
                    con.sendStanza(presence);
                    Log.v("state", "设置离开");
                    break;
                case 4:
//                    Roster roster = con.getRoster();
//                    Collection<RosterEntry> entries = roster.getEntries();
//                    for (RosterEntry entry : entries) {
//                        presence = new Presence(Presence.Type.unavailable);
//                        presence.setPacketID(Packet.ID_NOT_AVAILABLE);
//                        presence.setFrom(con.getUser());
//                        presence.setTo(entry.getUser());
//                        con.sendPacket(presence);
//                        Log.v("state", presence.toXML());
//                    }
//                    // 向同一用户的其他客户端发送隐身状态
//                    presence = new Presence(Presence.Type.unavailable);
//                    presence.setPacketID(Packet.ID_NOT_AVAILABLE);
//                    presence.setFrom(con.getUser());
//                    presence.setTo(StringUtils.parseBareAddress(con.getUser()));
//                    con.sendStanza(presence);
//                    Log.v("state", "设置隐身");
//                    break;
                case 5:
                    presence = new Presence(Presence.Type.unavailable);
                    con.sendStanza(presence);
                    Log.v("state", "设置离线");
                    break;
                default:
                    break;
            }
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取所有组
     *
     * @return 所有组集合
     */
    public List<RosterGroup> getGroups() {
        if (getConnection() == null)
            return null;
        List<RosterGroup> groupList = new ArrayList<>();
        Collection<RosterGroup> rosterGroup = Roster.getInstanceFor(connection).getGroups();
        for (RosterGroup aRosterGroup : rosterGroup) {
            groupList.add(aRosterGroup);
        }
        return groupList;
    }

    /**
     * 获取某个组里面的所有好友
     *
     * @param groupName 组名
     * @return List<RosterEntry>
     */
    public List<RosterEntry> getEntriesByGroup(String groupName) {
        if (getConnection() == null)
            return null;
        List<RosterEntry> EntriesList = new ArrayList<>();
        RosterGroup rosterGroup = Roster.getInstanceFor(connection).getGroup(groupName);
        Collection<RosterEntry> rosterEntry = rosterGroup.getEntries();
        for (RosterEntry aRosterEntry : rosterEntry) {
            EntriesList.add(aRosterEntry);
        }
        return EntriesList;
    }

    /**
     * 获取所有好友信息
     *
     * @return List<RosterEntry>
     */
    public List<RosterEntry> getAllEntries() {
        Log.i("getAllEntries","ssssssssssss");
        if (getConnection() == null)
            return null;

        Log.i("getAllEntries","ssssssssssss"+Roster.getInstanceFor(connection).getUnfiledEntryCount());
        Log.i("getAllEntries","ssssssssssss"+Roster.getInstanceFor(connection).getGroupCount());
        Log.i("getAllEntries","ssssssssssss"+Roster.getInstanceFor(connection).getEntryCount());


        List<RosterEntry> Enlist = new ArrayList<>();
        Collection<RosterEntry> rosterEntry = Roster.getInstanceFor(connection).getEntries();
        Log.i("getAllEntries","rosterEntry.size()="+rosterEntry.size());
        for (RosterEntry aRosterEntry : rosterEntry) {
            Log.i("getAllEntries","aRosterEntry.getJid()="+aRosterEntry.getJid());
            Enlist.add(aRosterEntry);
        }

        return Enlist;
    }

    /**
     * 获取用户VCard信息
     *
     * @param user user
     * @return VCard
     */
    public VCard getUserVCard(String user) {
        if (getConnection() == null)
            return null;
        VCard vcard = new VCard();
        try {
            vcard = VCardManager.getInstanceFor(getConnection()).loadVCard(JidCreate.entityBareFrom(user));
        } catch (XmppStringprepException | SmackException | InterruptedException | XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        }

        return vcard;
    }

    /**
     * 获取用户头像信息
     *
     * @param user user
     * @return Drawable
     */
    public Drawable getUserImage(String user) {
        if (getConnection() == null)
            return null;
        ByteArrayInputStream bais = null;
        try {
            VCard vcard = new VCard();
            // 加入这句代码，解决No VCard for
            ProviderManager.addIQProvider("vCard", "vcard-temp",
                    new org.jivesoftware.smackx.vcardtemp.provider.VCardProvider());
            if (user == null || user.equals("") || user.trim().length() <= 0) {
                return null;
            }
            try {
                VCardManager.getInstanceFor(getConnection()).loadVCard(
                        JidCreate.entityBareFrom(user + "@" + getConnection().getXMPPServiceDomain()));
            } catch (XmppStringprepException | SmackException | InterruptedException | XMPPException.XMPPErrorException e) {
                e.printStackTrace();
            }

            if (vcard.getAvatar() == null)
                return null;
            bais = new ByteArrayInputStream(vcard.getAvatar());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return FormatTools.getInstance().InputStream2Drawable(bais);
    }

    /**
     * 添加一个分组
     *
     * @param groupName groupName
     * @return boolean
     */
    public boolean addGroup(String groupName) {
        if (getConnection() == null)
            return false;
        try {
            Roster.getInstanceFor(connection).createGroup(groupName);
            Log.v("addGroup", groupName + "創建成功");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除分组
     *
     * @param groupName groupName
     * @return boolean
     */
    public boolean removeGroup(String groupName) {
        return true;
    }

    /**
     * 添加好友 无分组
     *
     * @param userName userName
     * @param name     name
     * @return boolean
     */
    public boolean addUser(String userName, String name) {
        if (getConnection() == null)
            return false;
        try {
            Roster.getInstanceFor(connection).createEntry(JidCreate.entityBareFrom(userName), name, null);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 添加好友 有分组
     *
     * @param userName  userName
     * @param name      name
     * @param groupName groupName
     * @return boolean
     */
    public boolean addUser(String userName, String name, String groupName) {
        if (getConnection() == null)
            return false;
        try {
            Presence subscription = new Presence(Presence.Type.subscribed);
            subscription.setTo(JidCreate.entityBareFrom(userName));
            userName += "@" + getConnection().getXMPPServiceDomain();
            getConnection().sendStanza(subscription);
            Roster.getInstanceFor(connection).createEntry(JidCreate.entityBareFrom(userName), name,
                    new String[]{groupName});
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除好友
     *
     * @param userName userName
     * @return boolean
     */
    public boolean removeUser(String userName) {
        if (getConnection() == null)
            return false;
        try {
            RosterEntry entry = null;
            if (userName.contains("@"))
                entry = Roster.getInstanceFor(connection).getEntry(JidCreate.entityBareFrom(userName));
            else
                entry = Roster.getInstanceFor(connection).getEntry(JidCreate.entityBareFrom(
                        userName + "@" + getConnection().getXMPPServiceDomain()));
            if (entry == null)
                entry = Roster.getInstanceFor(connection).getEntry(JidCreate.entityBareFrom(userName));
            Roster.getInstanceFor(connection).removeEntry(entry);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 查询用户
     *
     * @param userName userName 用户账号，@前面的部分 如1001@192.168.10.1 userName为1001
     * @return List<HashMap<String, String>>
     */
    public List<HashMap<String, String>> searchUsers(String userName) {
        Log.i(TAG, "searchUsers: userName="+userName);
        if (getConnection() == null)
            return null;
        Log.i(TAG, "searchUsers: userName="+userName);

        HashMap<String, String> user;
        List<HashMap<String, String>> results = new ArrayList<>();
        try {
            UserSearchManager usm = new UserSearchManager(getConnection());

            Log.i(TAG, "searchUsers: 域名："+JidCreate.domainBareFrom("vjud."+getConnection().getXMPPServiceDomain()).toString());
            Form searchForm = usm.getSearchForm(JidCreate.domainBareFrom("vjud."+getConnection().getXMPPServiceDomain()));

            if (searchForm == null)
                return null;

            Form answerForm = searchForm.createAnswerForm();
            answerForm.setAnswer("user", userName);
            //answerForm.setAnswer("Name", true);
            //answerForm.setAnswer("search", userName);
            ReportedData data = usm.getSearchResults(answerForm, JidCreate.domainBareFrom("vjud." + getConnection().getXMPPServiceDomain()));
            List<ReportedData.Row> rowList = data.getRows();
            for (ReportedData.Row row : rowList) {
                user = new HashMap<>();
                user.put("userAccount", row.getValues("jid").toString());
                Log.i(TAG, "search jid=" + row.getValues("jid").toString());//账号 100001
                Log.i(TAG, "search Username=" + row.getValues("Username").toString());//账号 100001
                Log.i(TAG, "search Name=" + row.getValues("Name").toString());//昵称
                Log.i(TAG, "search Email=" + row.getValues("Email").toString());
                results.add(user);
                // 若存在，则有返回,UserName一定非空，其他两个若是有设，一定非空
            }

        } catch (SmackException | InterruptedException | XmppStringprepException | XMPPException e) {
            Log.e(TAG, "searchUsers: e="+e);
            e.printStackTrace();
        }
        return results;
    }


    /**
     * 查询用户
     *
     * @param userName userName
     * @return List<HashMap<String, String>>
     */
    public List<HashMap<String, String>> searchUsers2(String userName) {
        Log.i(TAG, "searchUsers: userName="+userName);
        if (getConnection() == null)
            return null;
        Log.i(TAG, "searchUsers: userName="+userName);

        HashMap<String, String> user;
        List<HashMap<String, String>> results = new ArrayList<>();
        try {
            UserSearchManager usm = new UserSearchManager(getConnection());

            Log.i(TAG, "searchUsers: 域名："+JidCreate.domainBareFrom("search."+getConnection().getXMPPServiceDomain()).toString());
            Form searchForm = usm.getSearchForm(JidCreate.domainBareFrom("search."+getConnection().getXMPPServiceDomain()));

            if (searchForm == null)
                return null;

            Form answerForm = searchForm.createAnswerForm();
            answerForm.setAnswer("userAccount", true);
            answerForm.setAnswer("userPhote", userName);
            ReportedData data = usm.getSearchResults(answerForm, JidCreate.domainBareFrom("search." + getConnection().getXMPPServiceDomain()));

            List<ReportedData.Row> rowList = data.getRows();
            for (ReportedData.Row row : rowList) {
                user = new HashMap<>();
                user.put("userAccount", row.getValues("userAccount").toString());
                user.put("userPhote", row.getValues("userPhote").toString());
                results.add(user);
                // 若存在，则有返回,UserName一定非空，其他两个若是有设，一定非空
            }

        } catch (SmackException | InterruptedException | XmppStringprepException | XMPPException e) {
            Log.e(TAG, "searchUsers: e="+e);
            e.printStackTrace();
        }
        return results;
    }


    /**
     * 修改心情
     *
     * @param status
     */
    public void changeStateMessage(String status) {
        if (getConnection() == null)
            return;
        Presence presence = new Presence(Presence.Type.available);
        presence.setStatus(status);
        try {
            getConnection().sendStanza(presence);
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 修改用户头像
     *
     * @param file file
     */
    public boolean changeImage(File file) {
        if (getConnection() == null)
            return false;
        try {
            VCard vcard = new VCard();
            vcard.load(getConnection());

            byte[] bytes;

            bytes = getFileBytes(file);
            String encodedImage = StringUtils.encodeHex(bytes);
            vcard.setAvatar(bytes, encodedImage);
            vcard.setEncodedImage(encodedImage);
            vcard.setField("PHOTO", "<TYPE>image/jpg</TYPE><BINVAL>"
                    + encodedImage + "</BINVAL>", true);

            ByteArrayInputStream bais = new ByteArrayInputStream(
                    vcard.getAvatar());
            FormatTools.getInstance().InputStream2Bitmap(bais);

            vcard.save(getConnection());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 文件转字节
     *
     * @param file file
     * @return byte[]
     * @throws IOException
     */
    private byte[] getFileBytes(File file) throws IOException {
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            int bytes = (int) file.length();
            byte[] buffer = new byte[bytes];
            int readBytes = bis.read(buffer);
            if (readBytes != buffer.length) {
                throw new IOException("Entire file not read");
            }
            return buffer;
        } finally {
            if (bis != null) {
                bis.close();
            }
        }
    }

    /**
     * 删除当前用户
     *
     * @return true成功
     */
    public boolean deleteAccount() {
        if (getConnection() == null)
            return false;
        try {
            AccountManager.getInstance(connection).deleteAccount();
            return true;
        } catch (XMPPException | SmackException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 修改密码
     *
     * @return true成功
     */
    public boolean changePassword(String pwd) {
        if (getConnection() == null)
            return false;
        try {
            AccountManager.getInstance(connection).changePassword(pwd);
            return true;
        } catch (SmackException | InterruptedException | XMPPException.XMPPErrorException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 初始化会议室列表
     */
    public List<HostedRoom> getHostRooms() {
        if (getConnection() == null)
            return null;
        Collection<HostedRoom> hostrooms;
        List<HostedRoom> roominfos = new ArrayList<>();
        try {
            hostrooms = MultiUserChatManager.getInstanceFor(getConnection()).getHostedRooms(
                    JidCreate.domainBareFrom(getConnection().getXMPPServiceDomain()));
            for (HostedRoom entry : hostrooms) {
                roominfos.add(entry);
                Log.i("room", "名字：" + entry.getName() + " - ID:" + entry.getJid());
            }
            Log.i("room", "服务会议数量:" + roominfos.size());
        } catch (XMPPException | XmppStringprepException | InterruptedException | SmackException e) {
            e.printStackTrace();
            return null;
        }
        return roominfos;
    }

    /**
     * 创建房间
     *
     * @param roomName 房间名称
     */
    public MultiUserChat createRoom(String roomName, String password) {
        if (getConnection() == null)
            return null;

        MultiUserChat muc = null;
        try {
            // 创建一个MultiUserChat
            muc = MultiUserChatManager.getInstanceFor(getConnection()).getMultiUserChat(
                    JidCreate.entityBareFrom(roomName + "@conference." + getConnection().getXMPPServiceDomain()));
            // 创建聊天室
            muc.create(Resourcepart.from(roomName));
            // 获得聊天室的配置表单
            Form form = muc.getConfigurationForm();
            // 根据原始表单创建一个要提交的新表单。
            Form submitForm = form.createAnswerForm();
            // 向要提交的表单添加默认答复
            for (FormField formField : form.getFields()) {
                if (FormField.Type.hidden == formField.getType()
                        && formField.getVariable() != null) {
                    // 设置默认值作为答复
                    submitForm.setDefaultAnswer(formField.getVariable());
                }
            }
            // 设置聊天室的新拥有者
            List<String> owners = new ArrayList<>();
            owners.add(getConnection().getUser().asEntityBareJidString());// 用户JID
            submitForm.setAnswer("muc#roomconfig_roomowners", owners);
            // 设置聊天室是持久聊天室，即将要被保存下来
            submitForm.setAnswer("muc#roomconfig_persistentroom", true);
            // 房间仅对成员开放
            submitForm.setAnswer("muc#roomconfig_membersonly", false);
            // 允许占有者邀请其他人
            submitForm.setAnswer("muc#roomconfig_allowinvites", true);
            if (!password.equals("")) {
                // 进入是否需要密码
                submitForm.setAnswer("muc#roomconfig_passwordprotectedroom",
                        true);
                // 设置进入密码
                submitForm.setAnswer("muc#roomconfig_roomsecret", password);
            }
            // 能够发现占有者真实 JID 的角色
            // submitForm.setAnswer("muc#roomconfig_whois", "anyone");
            // 登录房间对话
            submitForm.setAnswer("muc#roomconfig_enablelogging", true);
            // 仅允许注册的昵称登录
            submitForm.setAnswer("x-muc#roomconfig_reservednick", true);
            // 允许使用者修改昵称
            submitForm.setAnswer("x-muc#roomconfig_canchangenick", false);
            // 允许用户注册房间
            submitForm.setAnswer("x-muc#roomconfig_registration", false);
            // 发送已完成的表单（有默认值）到服务器来配置聊天室
            muc.sendConfigurationForm(submitForm);
        } catch (XMPPException | XmppStringprepException | SmackException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        return muc;
    }

    /**
     * 加入会议室
     *
     * @param user      昵称
     * @param roomsName 会议室名
     */
    public MultiUserChat joinMultiUserChat(String user, String roomsName) {
        Log.i("wangqx","joinMultiUserChat");
        if (getConnection() == null)
            return null;
        try {
            // 使用XMPPConnection创建一个MultiUserChat窗口
            MultiUserChat multiUserChat = MultiUserChatManager.getInstanceFor(getConnection()).getMultiUserChat(
                    JidCreate.entityBareFrom(roomsName + "@conference." + getConnection().getXMPPServiceDomain()));

            Log.i("wangqx","user="+user);
            Log.i("wangqx","roomsName="+roomsName);
            Log.w("加入聊天室","multiUserChat="+multiUserChat.isJoined());
            // 用户加入聊天室
            multiUserChat.join(Resourcepart.from(user));
            MucEnterConfiguration.Builder builder = multiUserChat.getEnterConfigurationBuilder(Resourcepart.from(user));
            MucEnterConfiguration mucEnterConfiguration = builder.build();

          //  multiUserChat.join(mucEnterConfiguration);
        //    Log.i("MultiUserChat","joinMultiUserChat="+Resourcepart.from(user));
            Log.e("MultiUserChat","joinMultiUserChat ---");
            Log.i("MultiUserChat", "会议室【" + roomsName + "】加入成功........");
            return multiUserChat;
        } catch (XMPPException | XmppStringprepException | InterruptedException | SmackException e) {
            e.printStackTrace();
            Log.e("MultiUserChat", "会议室【" + roomsName + "】加入失败........");
            return null;
        }
    }

    /**
     * 发送群组聊天消息
     *
     * @param muc     muc
     * @param message 消息文本
     */
    public void sendGroupMessage(MultiUserChat muc, String message) {
        try {
            muc.sendMessage(message);
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 查询会议室成员名字
     *
     * @param muc
     */
    public List<String> findMulitUser(MultiUserChat muc) {
        if (getConnection() == null)
            return null;
        List<String> listUser = new ArrayList<>();
        List<EntityFullJid> it = muc.getOccupants();
        // 遍历出聊天室人员名称
        for (EntityFullJid entityFullJid : it) {
            // 聊天室成员名字
            String name = entityFullJid.toString();
            listUser.add(name);
        }
        return listUser;
    }

    /**
     * 创建聊天窗口
     *
     * @param JID JID
     * @return Chat
     */
    public Chat getFriendChat(String JID) {
        try {
            return ChatManager.getInstanceFor(XmppConnection.getInstance().getConnection())
                    .chatWith(JidCreate.entityBareFrom(JID));
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 发送单人聊天消息
     *
     * @param chat    chat
     * @param content 消息文本
     */
    public void sendSingleMessage(Chat chat, String content) {
        try {
            Message message = new Message();
            message.setBody(content);

            Received received = new Received();
            received.setId("CJq74-0");//回执的消息ID
            received.setStatus(1);//回执状态

            message.addExtension(received);

            chat.send(message);
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发消息
     *
     * @param chat    chat
     * @param muc     muc
     * @param message message
     */
    public void sendMessage(Chat chat, MultiUserChat muc, String message) {
        if (chat != null) {
            sendSingleMessage(chat, message);
        } else if (muc != null) {
            sendGroupMessage(muc, message);
        }
    }

    /**
     * 发送文件
     *
     * @param user
     * @param filePath
     */
    public void sendFile(String user, String filePath) {
        if (getConnection() == null)
            return;
        // 创建文件传输管理器
        FileTransferManager manager = FileTransferManager.getInstanceFor(getConnection());

        // 创建输出的文件传输
        OutgoingFileTransfer transfer = null;
        try {
            transfer = manager.createOutgoingFileTransfer(JidCreate.entityFullFrom(user));
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }

        // 发送文件
        try {
            if (transfer != null)
                transfer.sendFile(new File(filePath), "You won't believe this!");
        } catch (SmackException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取离线消息
     *
     * @return
     */
    public Map<String, List<HashMap<String, String>>> getHisMessage() {
        if (getConnection() == null)
            return null;
        Map<String, List<HashMap<String, String>>> offlineMsgs = null;

        try {
            OfflineMessageManager offlineManager = new OfflineMessageManager(getConnection());
            List<Message> messageList = offlineManager.getMessages();

            int count = offlineManager.getMessageCount();
            if (count <= 0)
                return null;
            offlineMsgs = new HashMap<>();

            for (Message message : messageList) {
                String fromUser = message.getFrom().toString();
                HashMap<String, String> history = new HashMap<>();
                history.put("useraccount", getConnection().getUser().asEntityBareJidString());
                history.put("friendaccount", fromUser);
                history.put("info", message.getBody());
                history.put("type", "left");
                if (offlineMsgs.containsKey(fromUser)) {
                    offlineMsgs.get(fromUser).add(history);
                } else {
                    List<HashMap<String, String>> temp = new ArrayList<HashMap<String, String>>();
                    temp.add(history);
                    offlineMsgs.put(fromUser, temp);
                }
            }
            offlineManager.deleteMessages();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return offlineMsgs;
    }

    /**
     * 判断OpenFire用户的状态 strUrl :
     * url格式 - http://my.openfire.com:9090/plugins/presence
     * /status?jid=user1@SERVER_NAME&type=xml
     * 返回值 : 0 - 用户不存在; 1 - 用户在线; 2 - 用户离线
     * 说明 ：必须要求 OpenFire加载 presence 插件，同时设置任何人都可以访问
     */
    public int IsUserOnLine(String user) {
        String url = "http://" + SERVER_HOST + ":9090/plugins/presence/status?" +
                "jid=" + user + "@" + SERVER_NAME + "&type=xml";
        int shOnLineState = 0; // 不存在
        try {
            URL oUrl = new URL(url);
            URLConnection oConn = oUrl.openConnection();
            if (oConn != null) {
                BufferedReader oIn = new BufferedReader(new InputStreamReader(
                        oConn.getInputStream()));
                String strFlag = oIn.readLine();
                oIn.close();
                System.out.println("strFlag" + strFlag);
                if (strFlag.contains("type=\"unavailable\"")) {
                    shOnLineState = 2;
                }
                if (strFlag.contains("type=\"error\"")) {
                    shOnLineState = 0;
                } else if (strFlag.contains("priority") || strFlag.contains("id=\"")) {
                    shOnLineState = 1;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return shOnLineState;
    }
}

