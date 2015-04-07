package io.Pushjet.api.PushjetApi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import io.Pushjet.api.MiscUtil;
import io.Pushjet.api.R;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;

public class PushjetService {
    private String name;
    private String token;
    private Date created;
    private String icon;
    private String secret;

    public PushjetService(String token, String name) {
        this(token, name, new Date());
    }

    public PushjetService(String token, String name, Date created) {
        this(token, name, "", created);
    }

    public PushjetService(String token, String name, String icon, Date created) {
        this(token, name, icon, "", created);
    }

    public PushjetService(String token, String name, String icon, String secret, Date created) {
        this.token = token;
        this.name = name;
        this.created = created;
        this.icon = icon;
        this.secret = secret;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String toString() {
        return "[" + getName() + "]" + getToken();
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public boolean canSend() {
        return !this.secret.equals("");
    }

    public boolean hasIcon() {
        return !this.icon.equals("");
    }

    public boolean iconCached(Context context) {
        if (!hasIcon())
            return false;
        try {
            context.openFileInput(MiscUtil.iconFilename(this.icon));
            return true;
        } catch (IOException ignore) {
            return false;
        }
    }

    public Bitmap getIconBitmap(Context context) throws IOException {
        FileInputStream in = null;
        try {
            in = context.openFileInput(MiscUtil.iconFilename(this.icon));
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            if (bitmap == null)
                throw new IOException("Decoding error");
            return bitmap;
        } finally {
            if (in != null) in.close();
        }
    }

    public Bitmap getIconBitmapOrDefault(Context context) {
        Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);
        if (hasIcon()) {
            try {
                icon = getIconBitmap(context);
            } catch (IOException ignore) {
            }
        }
        return icon;
    }

}
