package com.sam_chordas.android.stockhawk.widget;

class StockWidgetItem {

    private String symbol;
    private String change;
    private String percentChange;
    private boolean positive;

    public StockWidgetItem(String symbol, String change, String percentChange, boolean positive) {
        this.symbol = symbol;
        this.change = change;
        this.percentChange = percentChange;
        this.positive = positive;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getChange() {
        return change;
    }

    public void setChange(String change) {
        this.change = change;
    }

    public String getPercentChange() {
        return percentChange;
    }

    public void setPercentChange(String percentChange) {
        this.percentChange = percentChange;
    }

    public boolean isPositive() {
        return positive;
    }

    public void setPositive(boolean positive) {
        this.positive = positive;
    }
}
