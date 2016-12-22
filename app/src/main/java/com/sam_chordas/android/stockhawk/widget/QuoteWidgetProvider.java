package com.sam_chordas.android.stockhawk.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;
import com.sam_chordas.android.stockhawk.ui.StockHistoryActivity;

public class QuoteWidgetProvider extends AppWidgetProvider {

    public static String ACTION_STOCKS = "com.sam_chordas.android.stockhawk.widget.action.ACTION_STOCKS";
    public static String ACTION_HISTORY = "com.sam_chordas.android.stockhawk.widget.action.ACTION_HISTORY";

    private static final String TAG = QuoteWidgetProvider.class.getSimpleName();

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                        int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_collection);

        // this will open the app
        Intent active = new Intent(context, QuoteWidgetProvider.class);
        active.setAction(ACTION_STOCKS);
        PendingIntent actionPendingIntent = PendingIntent.getBroadcast(context, 0, active, 0);
        views.setOnClickPendingIntent(R.id.widget, actionPendingIntent);

        // this will go to the stock history screen
        Intent stockIntent = new Intent(context, QuoteWidgetProvider.class);
        stockIntent.setAction(ACTION_HISTORY);
        PendingIntent stockPendingIntent = PendingIntent.getBroadcast(context, 0, stockIntent, 0);
        views.setPendingIntentTemplate(R.id.widget_list, stockPendingIntent);

        setRemoteAdapter(context, views);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive(): action = " + intent.getAction());
        if (intent.getAction().equals(ACTION_STOCKS)) {
            Intent i = new Intent(context, MyStocksActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        } else if (intent.getAction().equals(ACTION_HISTORY)) {
            String symbol = intent.getStringExtra("stock_name");
            Intent resultIntent = StockHistoryActivity.intent(context, symbol)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(resultIntent);
        } else {
            super.onReceive(context, intent);
        }
    }

    private static void setRemoteAdapter(Context context, RemoteViews views) {
        views.setRemoteAdapter(R.id.widget_list, new Intent(context, QuoteWidgetRemoteViewsService.class));
    }
}