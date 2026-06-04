package com.spark.apibalance;

import android.content.Context;
import android.content.SharedPreferences;

public class LocalStore {
    private static final String PREFS = "api_balance_store";
    private final SharedPreferences prefs;

    public LocalStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void put(String name, String value) {
        prefs.edit().putString(name, value == null ? "" : value).apply();
    }

    public String get(String name) {
        return prefs.getString(name, "");
    }

    public void clear(String name) {
        prefs.edit().remove(name).apply();
    }
}
