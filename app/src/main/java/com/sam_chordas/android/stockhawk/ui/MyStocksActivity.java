package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MyStocksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private Intent mServiceIntent;
    private static final int CURSOR_LOADER_ID = 0;
    private QuoteCursorAdapter mCursorAdapter;
    private Context mContext;
    private Loader<Cursor> mLoader;

    @Bind(R.id.recycler_view)
    RecyclerView recyclerView;
    @Bind(R.id.fab)
    FloatingActionButton fab;
    @Bind(R.id.empty_view)
    TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_my_stocks);
        ButterKnife.bind(this);
        // The intent service is for executing immediate pulls from the Yahoo API
        // GCMTaskService can only schedule tasks, they cannot execute immediately
        mServiceIntent = new Intent(this, StockIntentService.class);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mCursorAdapter = new QuoteCursorAdapter(this, null);
        mLoader = getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);
        recyclerView.setAdapter(mCursorAdapter);

        fab.attachToRecyclerView(recyclerView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Utils.isNetworkAvailable(MyStocksActivity.this)) {
                    new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
                            .content(R.string.content_test)
                            .inputType(InputType.TYPE_CLASS_TEXT)
                            .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                                    // On FAB click, receive user input. Make sure the stock doesn't already exist
                                    // in the DB and proceed accordingly
                                    Cursor c = null;
                                    try {
                                        c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                                new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
                                                new String[]{input.toString()}, null);
                                        assert c != null;
                                        if (c.getCount() != 0) {
                                            Toast toast =
                                                    Toast.makeText(MyStocksActivity.this, "This stock is already saved!",
                                                            Toast.LENGTH_LONG);
                                            toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                                            toast.show();
                                        } else {
                                            // Add the stock to DB
                                            mServiceIntent.putExtra("tag", "add");
                                            mServiceIntent.putExtra("symbol", input.toString());
                                            startService(mServiceIntent);
                                        }
                                    } finally {
                                        if (c != null) {
                                            c.close();
                                        }
                                    }
                                }
                            })
                            .show();
                } else {
                    networkToast();
                }

            }
        });

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        mTitle = getTitle();
        emptyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Utils.isNetworkAvailable(MyStocksActivity.this)) {
                    startService(mServiceIntent);

                    long period = 3600L;
                    long flex = 10L;
                    String periodicTag = "periodic";

                    // create a periodic task to pull stocks once every hour after the app has been opened. This
                    // is so Widget data stays up to date.
                    PeriodicTask periodicTask = new PeriodicTask.Builder()
                            .setService(StockTaskService.class)
                            .setPeriod(period)
                            .setFlex(flex)
                            .setTag(periodicTag)
                            .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                            .setRequiresCharging(false)
                            .build();
                    // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
                    // are updated.
                    GcmNetworkManager.getInstance(MyStocksActivity.this).schedule(periodicTask);
                } else {
                    networkToast();
                }
                mLoader.startLoading();
            }
        });

        if (savedInstanceState == null) {
            mServiceIntent.putExtra("tag", "init");
            emptyView.callOnClick();
        }

        mCursorAdapter.setEmptyView(emptyView);
    }

    public void networkToast() {
        Toast.makeText(mContext, getString(R.string.network_required_toast_message), Toast.LENGTH_SHORT).show();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(mTitle);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_stocks, menu);
        restoreActionBar();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_change_units:
                // this is for changing stock changes from percent value to dollar value
                Utils.showPercent = !Utils.showPercent;
                this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        invalidateEmptyMessage(null);
        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(
                this,
                QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null
        );

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursorAdapter.swapCursor(data);
        invalidateEmptyMessage(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }


    public final void invalidateEmptyMessage(Cursor data) {
        if (data == null) {
            // loading data
            emptyView.setText(R.string.loading);
        } else if (data.getPosition() == ListView.INVALID_POSITION) {
            // received empty response
            if (Utils.isNetworkAvailable(this)) {
                emptyView.setText(R.string.no_stocks_message);
            } else {
                emptyView.setText(R.string.no_internet_message);
            }
        }
    }

}
