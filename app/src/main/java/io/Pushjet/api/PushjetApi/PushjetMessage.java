package io.Pushjet.api.PushjetApi;

import java.util.Date;
import java.util.TimeZone;

public class PushjetMessage {
    private String title;
    private String message;
    private PushjetService service;
    private int level;
    private Date timestamp;
    private String link;
    private int id = -1;

    public PushjetMessage(PushjetService service, String message, String title) {
        this(service, message, title, 0);
    }

    public PushjetMessage(PushjetService service, String message, String title, int timestamp) {
        this(service, message, title, new Date((long) timestamp * 1000));
    }

    public PushjetMessage(PushjetService service, String message, String title, Date timestamp) {
        this.service = service;
        this.message = message;
        this.title = title;
        this.timestamp = timestamp;
        this.level = 0;
        this.link = "";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public PushjetService getService() {
        return service;
    }

    public void setService(PushjetService service) {
        this.service = service;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Date getLocalTimestamp() {
        return new Date(timestamp.getTime() + TimeZone.getDefault().getOffset(System.currentTimeMillis()));
    }

    public String getTitleOrName() {
        String ret = title;
        if (ret.equals(""))
            ret = service.getName();
        return ret;
    }

    public String toString() {
        return getTitleOrName() + ": " + getMessage();
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public boolean hasLink() {
        return !this.link.equals("");
    }
}
