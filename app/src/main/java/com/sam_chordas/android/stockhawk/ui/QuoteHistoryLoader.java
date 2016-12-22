package com.sam_chordas.android.stockhawk.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.sam_chordas.android.stockhawk.data.QuoteHistory;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class QuoteHistoryLoader extends AsyncTaskLoader<QuoteHistory> {

    private static final String TAG = QuoteHistoryLoader.class.getSimpleName();

    private static final String BASE_URL = "https://query.yahooapis.com/v1/public/yql?q=";
    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private final String stockName;
    private final OkHttpClient client = new OkHttpClient();

    public QuoteHistoryLoader(Context context, String stockName) {
        super(context);
        this.stockName = stockName;
    }

    @Override
    public QuoteHistory loadInBackground() {
        StringBuilder urlBuilder = new StringBuilder();

        try {
            urlBuilder.append(BASE_URL);
            urlBuilder.append(URLEncoder.encode("SELECT * FROM yahoo.finance.historicaldata WHERE symbol IN (", "UTF-8"));
            urlBuilder.append(URLEncoder.encode("\"" + stockName + "\") ", "UTF-8"));
            urlBuilder.append(URLEncoder.encode("and startDate = '" + get30daysTimeStamp() + "' and " +
                    "endDate = '" + getCurrentTimeStamp() + "'", "UTF-8"));

            urlBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                    + "org%2Falltableswithkeys&callback=");

            String urlString = urlBuilder.toString();
            Log.d(TAG, "URL: " + urlString);

            String responseString = fetchData(urlString);
            Log.d(TAG, "response: " + responseString);

            return extractQuoteHistory(responseString);
        } catch (IOException e) {
            Log.e(TAG, "Network error: ", e);
            return new QuoteHistory(QuoteHistory.STATUS_NETWORK_ERROR, null, null);
        } catch (JSONException e) {
            Log.e(TAG, "Unsupported response error: ", e);
            return new QuoteHistory(QuoteHistory.STATUS_INVALID_RESPONSE, null, null);
        } catch (ParseException e) {
            Log.e(TAG, "Failed to parse response: ", e);
            return new QuoteHistory(QuoteHistory.STATUS_INVALID_RESPONSE, null, null);
        }
    }

    public String getCurrentTimeStamp() {
        return DATE_FORMAT.format(new Date());
    }

    private String get30daysTimeStamp() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -30);
        return DATE_FORMAT.format(c.getTime());
    }

    private String fetchData(String url) throws IOException{
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    private QuoteHistory extractQuoteHistory(String data) throws JSONException, ParseException {
        JSONObject json = new JSONObject(data);
        JSONArray history = json.getJSONObject("query")
                .getJSONObject("results")
                .getJSONArray("quote");
        Date[] dates = new Date[history.length()];
        float[] values = new float[history.length()];
        for (int i=1; i<=history.length(); i++) {
            JSONObject quote = history.getJSONObject(i-1);
            dates[history.length() - i] = DATE_FORMAT.parse(quote.getString("Date"));
            values[history.length() - i] = (float) quote.getDouble("Close");
        }
        return new QuoteHistory(QuoteHistory.STATUS_OK, dates[0], values);
    }

}
