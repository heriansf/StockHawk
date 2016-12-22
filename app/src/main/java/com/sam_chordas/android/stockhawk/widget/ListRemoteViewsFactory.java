package com.sam_chordas.android.stockhawk.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.ui.StockHistoryActivity;

import java.util.ArrayList;
import java.util.List;

class ListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context mContext;
    private final Intent mIntent;

    private List<StockWidgetItem> mCollection = new ArrayList<>();

    public ListRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        mIntent = intent;
    }

    @Override
    public void onCreate() {
        loadData();
    }

    @Override
    public void onDataSetChanged() {
        loadData();
    }

    @Override
    public void onDestroy() {
        // do nothing
    }

    @Override
    public int getCount() {
        return mCollection.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews view = new RemoteViews(mContext.getPackageName(),
                R.layout.widget_collection_item);

        StockWidgetItem stock = mCollection.get(position);

        view.setTextViewText(R.id.stock_symbol, stock.getSymbol());
        view.setTextViewText(R.id.change, stock.getPercentChange());

        if (stock.isPositive()) {
            view.setInt(R.id.change, "setBackgroundColor", ContextCompat.getColor(mContext, R.color.material_green_700));
        } else {
            view.setInt(R.id.change, "setBackgroundColor", ContextCompat.getColor(mContext, R.color.material_red_700));
        }

        view.setOnClickFillInIntent(R.id.widget_list_item, StockHistoryActivity.intent(mContext, stock.getSymbol()));
        return view;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private void loadData() {
        mCollection.clear();

        final long token = Binder.clearCallingIdentity();
        Cursor cursor = null;
        try {
            // load sorted alphabetically
            cursor = mContext.getContentResolver().query(
                    QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{
                            QuoteColumns.SYMBOL,
                            QuoteColumns.CHANGE,
                            QuoteColumns.PERCENT_CHANGE,
                            QuoteColumns.ISUP,
                            QuoteColumns._ID
                    },
                    null,
                    null,
                    QuoteColumns.SYMBOL + " ASC, " + QuoteColumns._ID + " DESC"
            );

            if (cursor != null) {
                int symbolIndex = cursor.getColumnIndex(QuoteColumns.SYMBOL);
                int changeIndex = cursor.getColumnIndex(QuoteColumns.CHANGE);
                int changePercentIndex = cursor.getColumnIndex(QuoteColumns.PERCENT_CHANGE);
                int isupIndex = cursor.getColumnIndex(QuoteColumns.ISUP);

                if (cursor.getCount() != 0) {
                    String symbol = ""; // skipping repeated symbols
                    while (cursor.moveToNext()) {
                        if (!symbol.equals(cursor.getString(symbolIndex))) {
                            symbol = cursor.getString(symbolIndex);
                            String change = cursor.getString(changeIndex);
                            String percentChange = cursor.getString(changePercentIndex);
                            boolean positive = cursor.getInt(isupIndex) == 1;
                            if (Utils.isValidString(percentChange)) {
                                // only include valid data
                                StockWidgetItem stock = new StockWidgetItem(symbol, change, percentChange, positive);
                                mCollection.add(stock);
                            }
                        }
                    }
                }
            }
        } finally {
            if (cursor != null)
                cursor.close();
            Binder.restoreCallingIdentity(token);
        }
    }
}
