package com.huang.xmpp;

import org.jivesoftware.smack.packet.Element;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.bytestreams.ibb.provider.DataPacketProvider;
import org.xmlpull.v1.XmlPullParser;

/**
 * XMPP 所需的服务IP和端口号,域名
 * Created by Administrator on 2017/11/7.
 */

public class ReceivedProvider extends ExtensionElementProvider<Received>
{


    @Override
    public Received parse(XmlPullParser parser, int initialDepth) throws Exception
    {
        boolean done = false;
        Received received = new Received();
        while (!done) {
            int eventType = parser.next();
            String name = parser.getName();//XML Tab标签
            if (eventType == XmlPullParser.START_TAG) {
                if (name.equals("id")) {
                    received.setId(parser.nextText());
                }
                if (name.equals("status")) {
                    received.setStatus(Integer.parseInt(parser.nextText().trim()));
                }
            }
            if (eventType == XmlPullParser.END_TAG) {
                if (Received.NAME.equals(name)) {
                    done = true;
                }
            }
        }
        return received;
    }
}
