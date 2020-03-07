package com.freefriday.wherewasi;

import android.provider.BaseColumns;

public final class DB {
    public static final class CreateDB implements BaseColumns{
        public static final String TIME = "[time]";
        public static final String LONG = "[long]";
        public static final String LAT = "[lat]";
        public static final String ACCR = "[accr]";
        public static final String TABLE_NAME = "[table]";
        public static final String CLEARDB = "DELETE FROM "+TABLE_NAME;
        public static final String CREATE = "create table if not exists " + TABLE_NAME
                + "(" + _ID + " integer primary key autoincrement, "
                + TIME + " text not null , "
                + LONG + " text not null , "
                + LAT + " text not null, "
                + ACCR + " text not null);";
    }
}

