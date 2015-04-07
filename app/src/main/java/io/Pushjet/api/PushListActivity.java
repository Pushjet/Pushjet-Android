package io.Pushjet.api;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import io.Pushjet.api.Async.FirstLaunchAsync;
import io.Pushjet.api.Async.GCMRegistrar;
import io.Pushjet.api.Async.ReceivePushAsync;
import io.Pushjet.api.Async.ReceivePushCallback;
import io.Pushjet.api.PushjetApi.PushjetApi;
import io.Pushjet.api.PushjetApi.PushjetMessage;

import java.util.ArrayList;
import java.util.Arrays;

public class PushListActivity extends ListActivity {
    private PushjetApi api;
    private DatabaseHandler db;
    private PushListAdapter adapter;
    private BroadcastReceiver receiver;
    private SwipeRefreshLayout refreshLayout;

    private SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            ReceivePushAsync receivePushAsync = new ReceivePushAsync(api, adapter);
            receivePushAsync.setCallBack(new ReceivePushCallback() {
                @Override
                public void receivePush(ArrayList<PushjetMessage> msg) {
                    refreshLayout.setRefreshing(false);
                }
            });
            refreshLayout.setRefreshing(true);
            receivePushAsync.execute();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push_list);

        this.api = new PushjetApi(getApplicationContext(), SettingsActivity.getRegisterUrl(this));
        this.db = new DatabaseHandler(getApplicationContext());
        this.refreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        this.refreshLayout.setEnabled(true);
        this.refreshLayout.setOnRefreshListener(refreshListener);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean firstLaunch = preferences.getBoolean("first_launch", true);
        if(firstLaunch) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("first_launch", false);
            editor.apply();

            new FirstLaunchAsync().execute(getApplicationContext());
        }


        adapter = new PushListAdapter(this);
        setListAdapter(adapter);
        this.getListView().setLongClickable(true);
        this.getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                PushjetMessage message = (PushjetMessage) adapter.getItem(position);

                MiscUtil.WriteToClipboard(message.getMessage(), "Pushjet message", getApplicationContext());
                Toast.makeText(getApplicationContext(), "Copied message to clipboard", Toast.LENGTH_SHORT).show();
                return true;
            }
        });


        GCMRegistrar mGCMRegistrar = new GCMRegistrar(getApplicationContext());
        if (firstLaunch || mGCMRegistrar.shouldRegister()) {
            if (mGCMRegistrar.checkPlayServices(this)) {
                mGCMRegistrar.registerInBackground(firstLaunch);
            } else {
                finish();
            }
        }

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updatePushList();
            }
        };
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        if (adapter.getSelected() == position)
            adapter.clearSelected();
        else
            adapter.setSelected(position);
    }

    @Override
    protected void onStart() {
        super.onStart();

        registerReceiver(receiver, new IntentFilter("PushjetMessageRefresh"));
        registerReceiver(receiver, new IntentFilter("PushjetIconDownloaded"));
    }

    @Override
    protected void onStop() {
        super.onStop();

        unregisterReceiver(receiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePushList();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void updatePushList() {
        adapter.upDateEntries(new ArrayList<PushjetMessage>(Arrays.asList(db.getAllMessages())));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.push_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Intent intent;
        switch (id) {
            case R.id.action_subscriptions:
                intent = new Intent(getApplicationContext(), SubscriptionsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_settings:
                intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
