package io.Pushjet.api;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import io.Pushjet.api.PushjetApi.PushjetException;
import io.Pushjet.api.PushjetApi.PushjetMessage;
import io.Pushjet.api.PushjetApi.PushjetService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DatabaseHandler extends SQLiteOpenHelper {
    private String TABLE_LISTEN = "listen";
    private String KEY_LISTEN_TOKEN = "service";
    private String KEY_LISTEN_SECRET = "secret";
    private String KEY_LISTEN_NAME = "name";
    private String KEY_LISTEN_ICON = "icon";
    private String KEY_LISTEN_TIMESTAMP = "timestamp";
    private String[] TABLE_LISTEN_KEYS = new String[]{KEY_LISTEN_TOKEN, KEY_LISTEN_SECRET, KEY_LISTEN_NAME, KEY_LISTEN_ICON, KEY_LISTEN_TIMESTAMP};

    private String TABLE_MESSAGE = "messages";
    private String KEY_MESSAGE_ID = "id";
    private String KEY_MESSAGE_SERVICE = "service";
    private String KEY_MESSAGE_TEXT = "text";
    private String KEY_MESSAGE_TITLE = "title";
    private String KEY_MESSAGE_LEVEL = "level";
    private String KEY_MESSAGE_TIMESTAMP = "timestamp";
    private String KEY_MESSAGE_LINK = "link";
    private String[] TABLE_MESSAGE_KEYS = new String[]{KEY_MESSAGE_ID, KEY_MESSAGE_SERVICE, KEY_MESSAGE_TEXT, KEY_MESSAGE_TITLE, KEY_MESSAGE_LEVEL, KEY_MESSAGE_TIMESTAMP, KEY_MESSAGE_LINK};

    public DatabaseHandler(Context context) {
        super(context, "Pushjet", null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE_LISTEN = "CREATE TABLE `" + TABLE_LISTEN + "` (" +
                "`" + KEY_LISTEN_TOKEN + "` VARCHAR PRIMARY KEY, " +
                "`" + KEY_LISTEN_SECRET + "` VARCHAR, " +
                "`" + KEY_LISTEN_NAME + "` VARCHAR," +
                "`" + KEY_LISTEN_ICON + "` VARCHAR," +
                "`" + KEY_LISTEN_TIMESTAMP + "` INTEGER)";

        String CREATE_TABLE_MESSAGE = "CREATE TABLE `" + TABLE_MESSAGE + "` (" +
                "`" + KEY_MESSAGE_ID + "` INTEGER PRIMARY KEY AUTOINCREMENT," +
                "`" + KEY_MESSAGE_SERVICE + "` VARCHAR, " +
                "`" + KEY_MESSAGE_TEXT + "` TEXT, " +
                "`" + KEY_MESSAGE_TITLE + "` VARCHAR, " +
                "`" + KEY_MESSAGE_LEVEL + "` INT," +
                "`" + KEY_MESSAGE_TIMESTAMP + "` INTEGER," +
                "`" + KEY_MESSAGE_LINK + "` VARCHAR)";

        db.execSQL(CREATE_TABLE_LISTEN);
        db.execSQL(CREATE_TABLE_MESSAGE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i2) {
        // Does nothing for now
    }

    public PushjetMessage[] getAllMessages() {
        SQLiteDatabase db = this.getReadableDatabase();

        try {
            List<PushjetMessage> result = new ArrayList<PushjetMessage>();
            Cursor cMsg = db.query(TABLE_MESSAGE, TABLE_MESSAGE_KEYS, null, null, null, null, null);
            if (cMsg.getCount() > 0 && cMsg.moveToFirst()) {
                do {
                    result.add(getMessageFromRow(cMsg));
                } while (cMsg.moveToNext());
            }
            PushjetMessage[] ret = new PushjetMessage[result.size()];
            return result.toArray(ret);
        } finally {
            db.close();
        }
    }

    public void addMessage(PushjetMessage msg) {
        ContentValues vMsg = new ContentValues();
        vMsg.put(KEY_MESSAGE_SERVICE, msg.getService().getToken());
        vMsg.put(KEY_MESSAGE_TEXT, msg.getMessage());
        vMsg.put(KEY_MESSAGE_LEVEL, msg.getLevel());
        vMsg.put(KEY_MESSAGE_TITLE, msg.getTitle());
        vMsg.put(KEY_MESSAGE_LINK, msg.getLink());
        vMsg.put(KEY_MESSAGE_TIMESTAMP, Math.round(msg.getTimestamp().getTime() / 1000L));

        SQLiteDatabase db = this.getWritableDatabase();

        try {
            db.insert(TABLE_MESSAGE, null, vMsg);

            if (!isListening(msg.getService())) {
                PushjetService srv = new PushjetService(msg.getService().getToken(), "UNKNOWN");
                addService(srv);
            }
        } finally {
            db.close();
        }
    }

    public void cleanService(PushjetService service) {
        if (isListening(service)) {
            SQLiteDatabase db = this.getWritableDatabase();
            String fmt = "`%s` = '%s'";

            try {
                db.delete(TABLE_MESSAGE, String.format(fmt, KEY_MESSAGE_SERVICE, service.getToken()), null);
            } finally {
                db.close();
            }
        }
    }

    public void addService(PushjetService service) {
        addServices(new PushjetService[]{service});
    }

    public void removeService(PushjetService service) {
        if (isListening(service)) {
            SQLiteDatabase db = this.getWritableDatabase();

            String fmt = "`%s` = '%s'";
            db.delete(TABLE_MESSAGE, String.format(fmt, KEY_MESSAGE_SERVICE, service.getToken()), null);
            db.delete(TABLE_LISTEN, String.format(fmt, KEY_LISTEN_TOKEN, service.getToken()), null);
            db.close();
        }
    }

    public void deleteMessage(PushjetMessage message) {
        SQLiteDatabase db = this.getWritableDatabase();
        String fmt = "`%s` = '%s'";

        try {
            db.delete(TABLE_MESSAGE, String.format(fmt, KEY_MESSAGE_ID, message.getId()), null);
        } finally {
            db.close();
        }
    }

    public PushjetMessage getMessage(int id) throws PushjetException {
        SQLiteDatabase db = this.getReadableDatabase();

        try {
            Cursor cMsg = db.query(TABLE_MESSAGE, TABLE_MESSAGE_KEYS, KEY_MESSAGE_ID + " = ?", new String[]{id + ""}, null, null, null);
            if (cMsg.getCount() > 0 && cMsg.moveToFirst())
                return getMessageFromRow(cMsg);
            throw new PushjetException("Message not found", 400);
        } finally {
            db.close();
        }
    }

    public void addServices(PushjetService[] services) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            for (PushjetService service : services) {
                boolean listening = isListening(service);
                if (!db.isOpen()) db = this.getWritableDatabase();

                if (listening) {
                    String fmt = "`%s` = '%s'";
                    db.delete(TABLE_LISTEN, String.format(fmt, KEY_LISTEN_TOKEN, service.getToken()), null);
                }

                ContentValues vSrv = new ContentValues();
                vSrv.put(KEY_LISTEN_TOKEN, service.getToken());
                vSrv.put(KEY_LISTEN_NAME, service.getName());
                vSrv.put(KEY_LISTEN_SECRET, service.getSecret());
                vSrv.put(KEY_LISTEN_ICON, service.getIcon());
                vSrv.put(KEY_LISTEN_TIMESTAMP, Math.round(service.getCreated().getTime() / 1000L));

                db.insert(TABLE_LISTEN, null, vSrv);
            }
        } finally {
            db.close();
        }
    }

    public int getServiceCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        String countQuery = "SELECT  * FROM " + TABLE_LISTEN;

        try {
            return db.rawQuery(countQuery, null).getCount();
        } finally {
            db.close();
        }
    }

    public PushjetService getService(String token) {
        SQLiteDatabase db = this.getReadableDatabase();
        PushjetService srv;
        try {
            Cursor cLsn = db.query(TABLE_LISTEN, TABLE_LISTEN_KEYS, KEY_LISTEN_TOKEN + " = ?", new String[]{token}, null, null, null);
            cLsn.moveToFirst();
            srv = new PushjetService(
                    cLsn.getString(0),
                    cLsn.getString(2),
                    cLsn.getString(3),
                    cLsn.getString(1),
                    new Date((long) cLsn.getInt(4) * 1000)
            );
        } catch (CursorIndexOutOfBoundsException ignore) {
            srv = new PushjetService(token, "UNKNOWN");
        } finally {
            db.close();
        }
        return srv;
    }

    public void refreshServices(PushjetService[] services) {
        this.addServices(services);

        PushjetService[] listening = this.getAllServices();
        for (PushjetService l1 : listening) {
            boolean rm = true;
            for (PushjetService l2 : services) {
                if (l1.getToken().equals(l2.getToken())) {
                    rm = false; break;
                }
            }

            if (rm) this.removeService(l1);
        }
    }

    public boolean isListening(PushjetService service) {
        return this.isListening(service.getToken());
    }

    public boolean isListening(String service) {
        SQLiteDatabase db = this.getReadableDatabase();

        try {
            Cursor cursor = db.query(TABLE_LISTEN, TABLE_LISTEN_KEYS, KEY_LISTEN_TOKEN + " = ?", new String[]{service}, null, null, null);
            return cursor.getCount() > 0;
        } finally {
            db.close();
        }
    }

    public PushjetService[] getAllServices() {
        SQLiteDatabase db = this.getReadableDatabase();
        List<PushjetService> result = new ArrayList<PushjetService>();

        try {
            Cursor cSrv = db.query(TABLE_LISTEN, TABLE_LISTEN_KEYS, null, null, null, null, null);
            if (cSrv.getCount() > 0 && cSrv.moveToFirst()) {
                do {
                    result.add(new PushjetService(
                            cSrv.getString(0),
                            cSrv.getString(2),
                            cSrv.getString(3),
                            cSrv.getString(1),
                            new Date((long) cSrv.getInt(4) * 1000)
                    ));
                } while (cSrv.moveToNext());
            }
            return result.toArray(new PushjetService[result.size()]);
        } finally {
            db.close();
        }
    }

    public void truncateMessages() {
        SQLiteDatabase db = this.getReadableDatabase();

        try {
            db.delete(TABLE_MESSAGE, null, null);
        } finally {
            db.close();
        }
    }

    public void truncateServices() {
        SQLiteDatabase db = this.getReadableDatabase();

        try {
            db.delete(TABLE_LISTEN, null, null);
        } finally {
            db.close();
        }
    }

    private PushjetMessage getMessageFromRow(Cursor cMsg) {
        PushjetMessage msg = new PushjetMessage(
                getService(cMsg.getString(1)),
                cMsg.getString(2),
                cMsg.getString(3),
                cMsg.getInt(5)
        );
        msg.setId(cMsg.getInt(0));
        msg.setLevel(cMsg.getInt(4));
        msg.setLink(cMsg.getString(6));

        return msg;
    }
}
