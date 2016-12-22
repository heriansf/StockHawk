package com.sam_chordas.android.stockhawk.data;

import java.util.Date;

public class QuoteHistory {

    public static final int STATUS_OK = 0;
    public static final int STATUS_NETWORK_ERROR = 1;
    public static final int STATUS_INVALID_RESPONSE = 2;

    private final int status;
    private final Date startDate;
    private final float[] values;

    public QuoteHistory(int status, Date startDate, float[] values) {
        this.status = status;
        this.startDate = startDate;
        this.values = values;
    }

    public int getStatus() {
        return status;
    }

    public Date getStartDate() {
        return startDate;
    }

    public float[] getValues() {
        return values;
    }
}
