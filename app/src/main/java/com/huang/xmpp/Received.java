package com.huang.xmpp;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.util.StringUtils;

/**
 * XMPP 所需的服务IP和端口号,域名
 * Created by Administrator on 2017/11/7.
 */

public class Received  implements ExtensionElement
{

    public static final String NAME = "received";
    public static final String NAME_SPACE = "urn:xmpp:receipts";

    private String id = "";
    private Integer status = 0;

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the status
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(Integer status) {
        this.status = status;
    }

    @Override
    public String getElementName() {
        return "received";
    }

    @Override
    public String getNamespace() {
        return "urn:xmpp:receipts";
    }

    @Override
    public String toXML() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<received xmlns=\"urn:xmpp:receipts\"");
        buffer.append(" id=\"").append(StringUtils.escapeForXML(id)).append("\"");
        buffer.append(" status=\"").append(status).append("\"");
        buffer.append("/>");
        return buffer.toString();
    }
}
