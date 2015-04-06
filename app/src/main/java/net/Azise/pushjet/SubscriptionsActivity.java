package net.Azise.pushjet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.InputType;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.zxing.qrcode.QRCodeWriter;

import net.Azise.pushjet.Async.AddServiceAsync;
import net.Azise.pushjet.Async.DeleteServiceAsync;
import net.Azise.pushjet.Async.GenericAsyncCallback;
import net.Azise.pushjet.Async.RefreshServiceAsync;
import net.Azise.pushjet.Async.RefreshServiceCallback;
import net.Azise.pushjet.PushjetApi.PushjetApi;
import net.Azise.pushjet.PushjetApi.PushjetException;
import net.Azise.pushjet.PushjetApi.PushjetService;
import net.Azise.pushjet.PushjetApi.PushjetUri;

import java.util.ArrayList;
import java.util.Arrays;

public class SubscriptionsActivity extends ListActivity {
    private PushjetApi api;
    private DatabaseHandler db;
    private SubscriptionsAdapter adapter;
    private BroadcastReceiver receiver;
    private SwipeRefreshLayout refreshLayout;
    private SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            refreshServices();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscriptions);
        this.refreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        this.refreshLayout.setEnabled(true);
        this.refreshLayout.setOnRefreshListener(refreshListener);

        this.api = new PushjetApi(getApplicationContext(), SettingsActivity.getRegisterUrl(this));
        this.db = new DatabaseHandler(getApplicationContext());

        adapter = new SubscriptionsAdapter(this);
        setListAdapter(adapter);

        adapter.upDateEntries(new ArrayList<PushjetService>(Arrays.asList(db.getAllServices())));
        registerForContextMenu(findViewById(android.R.id.list));

        Uri pushjetUri = getIntent().getData();
        if (pushjetUri != null) {
            try {
                String host = pushjetUri.getHost();
                Log.d("PushjetService", "Host: " + host);
                parseTokenOrUri(host);
            } catch (PushjetException e) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            } catch (NullPointerException ignore) {
            }
        }

        if (adapter.getCount() == 0 && !this.refreshLayout.isRefreshing()) {
            refreshServices();
        }

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                adapter.upDateEntries(new ArrayList<PushjetService>(Arrays.asList(db.getAllServices())));
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(receiver, new IntentFilter("PushjetIconDownloaded"));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(receiver);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.subscriptions_context_menu, menu);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        openContextMenu(v);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        PushjetService service = (PushjetService) adapter.getItem(Math.round(info.id));
        switch (item.getItemId()) {
            case R.id.action_copy_token:
                MiscUtil.WriteToClipboard(service.getToken(), "Pushjet Token", this);
                Toast.makeText(this, "Copied token to clipboard", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_delete:
                DeleteServiceAsync deleteServiceAsync = new DeleteServiceAsync(api, db);
                deleteServiceAsync.setCallback(new GenericAsyncCallback() {
                    @Override
                    public void onComplete(Object... objects) {
                        adapter.upDateEntries(new ArrayList<PushjetService>(Arrays.asList(db.getAllServices())));
                    }
                });
                deleteServiceAsync.execute(service);
                return true;
            case R.id.action_clear_notifications:
                db.cleanService(service);
                sendBroadcast(new Intent("PushjetMessageRefresh"));
                return true;
            case R.id.action_share:
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, "pjet://" + service.getToken() + "/");
                startActivity(Intent.createChooser(share, "Share service"));
                return true;
            case R.id.action_show_qr:
                try {
                    BitMatrix matrix = new QRCodeWriter().encode(service.getToken(), BarcodeFormat.QR_CODE, 1000, 1000);

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    View view = getLayoutInflater().inflate(R.layout.dialog_display_qr, null);

                    ImageView image = (ImageView) view.findViewById(R.id.image_qr);
                    image.setImageBitmap(MiscUtil.matrixToBitmap(matrix));

                    builder.setView(view).show();
                } catch (WriterException e) {
                    Toast.makeText(getApplicationContext(), "Couldn't generate qr code", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.subscriptions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.actions_new:
                final String[] items = new String[]{
                        "Scan QR", "Enter token"
                };
                final Activity thisActivity = this;
                new AlertDialog.Builder(this)
                        .setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (i == 0)
                                    IntentIntegrator.initiateScan(thisActivity, IntentIntegrator.QR_CODE_TYPES);
                                if (i == 1) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(thisActivity);
                                    builder.setTitle("Public token");
                                    final EditText input = new EditText(thisActivity);

                                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                                    builder.setView(input);
                                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            try {
                                                parseTokenOrUri(input.getText().toString());
                                            } catch (PushjetException e) {
                                                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                            } catch (NullPointerException ignore) {
                                            }

                                        }
                                    });
                                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                        }
                                    });

                                    builder.show();
                                }

                            }
                        })
                        .show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void parseTokenOrUri(String token) throws PushjetException {
        token = token.trim();
        if (PushjetUri.isValidUri(token)) {
            try {
                token = PushjetUri.tokenFromUri(token);
            } catch (PushjetException ignore) {
            }
        }
        if (!PushjetUri.isValidToken(token))
            throw new PushjetException("Invalid service id", 2);

        AddServiceAsync addServiceAsync = new AddServiceAsync(api, db, adapter);
        addServiceAsync.execute(token);
    }

    // Used for parsing the QR code scanner result
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        try {
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
            if (scanResult == null)
                return;
            parseTokenOrUri(scanResult.getContents().trim());
        } catch (PushjetException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (NullPointerException ignore) {
        }
    }

    private void refreshServices() {
        RefreshServiceCallback callback = new RefreshServiceCallback() {
            @Override
            public void onComplete(PushjetService[] services) {
                adapter.upDateEntries(new ArrayList<PushjetService>(Arrays.asList(services)));
                refreshLayout.setRefreshing(false);
            }
        };
        RefreshServiceAsync refresh = new RefreshServiceAsync(api, db);
        refresh.setCallback(callback);
        refreshLayout.setRefreshing(true);
        refresh.execute();
    }
}
