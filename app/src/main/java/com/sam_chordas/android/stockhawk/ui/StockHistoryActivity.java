package com.sam_chordas.android.stockhawk.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LabelFormatter;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteHistory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import butterknife.Bind;
import butterknife.ButterKnife;

public class StockHistoryActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<QuoteHistory> {

    private static final int LOADER_ID = 1;
    private static final String ARG_STOCK_NAME = "stock_name";
    private static final DateFormat DATE_FORMAT = SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT);

    public static Intent intent(Context context, String stockName) {
        return new Intent(context, StockHistoryActivity.class)
                .putExtra(ARG_STOCK_NAME, stockName);
    }

    @Bind(R.id.line_chart)
    GraphView lineChart;
    @Bind(R.id.empty_view)
    TextView emptyView;

    private Loader<QuoteHistory> loader;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_history);
        ButterKnife.bind(this);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getIntent().getStringExtra(ARG_STOCK_NAME).toUpperCase());
        }

        loader = getSupportLoaderManager().initLoader(LOADER_ID, getLoaderArgs(), this);

        if (savedInstanceState == null) {
            reloadData();
        }

        emptyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reloadData();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                NavUtils.navigateUpFromSameTask(this);
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<QuoteHistory> loader) {
        lineChart.getSeries().clear();
    }

    @Override
    public void onLoadFinished(Loader<QuoteHistory> loader, final QuoteHistory data) {
        lineChart.getSeries().clear();
        if (data.getStatus() == QuoteHistory.STATUS_OK) {
            DataPoint[] points = new DataPoint[data.getValues().length];
            for (int i=0; i<points.length; i++) {
                points[i] = new DataPoint(i, data.getValues()[i]);
            }
            LineGraphSeries<DataPoint> dataSet = new LineGraphSeries<>(points);
            dataSet.setColor(Color.RED);
            lineChart.addSeries(dataSet);
            lineChart.getGridLabelRenderer().setLabelFormatter(new LabelFormatter() {
                @Override
                public String formatLabel(double value, boolean isValueX) {
                    if (isValueX) {
                        Calendar c = Calendar.getInstance();
                        c.setTime(data.getStartDate());
                        c.add(Calendar.DAY_OF_YEAR, (int) value);
                        return DATE_FORMAT.format(c.getTime());
                    } else {
                        return String.valueOf(Math.floor(value * 100 + 0.5) / 100);
                    }
                }

                @Override
                public void setViewport(Viewport viewport) {
                    // do nothing
                }
            });
            emptyView.setVisibility(View.GONE);
            lineChart.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.VISIBLE);
            lineChart.setVisibility(View.GONE);
            emptyView.setText(R.string.error_history_load);
        }
    }

    @Override
    public Loader<QuoteHistory> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID:
                return new QuoteHistoryLoader(this.getApplicationContext(), args.getString(ARG_STOCK_NAME));
            default:
                return null;
        }
    }

    private Bundle getLoaderArgs() {
        String stockName = getIntent().getStringExtra(ARG_STOCK_NAME);
        Bundle args = new Bundle();
        args.putString(ARG_STOCK_NAME, stockName);
        return args;
    }

    private void reloadData() {
        emptyView.setVisibility(View.VISIBLE);
        lineChart.setVisibility(View.GONE);
        emptyView.setText(R.string.loading);
        loader.reset();
        loader.forceLoad();
    }
}

