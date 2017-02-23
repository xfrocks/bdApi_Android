package com.xfrocks.api.androiddemo.common;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

import java.util.Calendar;

public class DatePickerDialogFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    private static final String ARG_REQUEST_CODE = "request_code";
    private static final String ARG_YEAR = "year";
    private static final String ARG_MONTH = "month";
    private static final String ARG_DAY = "day";

    public static DatePickerDialogFragment newInstance(int requestCode, Integer year, Integer monthFromOne, Integer day) {
        DatePickerDialogFragment fragment = new DatePickerDialogFragment();

        Bundle args = new Bundle();
        args.putInt(ARG_REQUEST_CODE, requestCode);
        if (year != null) {
            args.putInt(ARG_YEAR, year);
        }
        if (monthFromOne != null) {
            args.putInt(ARG_MONTH, monthFromOne);
        }
        if (day != null) {
            args.putInt(ARG_DAY, day);
        }
        fragment.setArguments(args);

        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int monthFromZero = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        Integer argYear = null;
        Integer argMonth = null;
        Integer argDay = null;
        Bundle args = getArguments();
        if (args.containsKey(ARG_YEAR)) {
            argYear = args.getInt(ARG_YEAR);
        }
        if (args.containsKey(ARG_MONTH)) {
            argMonth = args.getInt(ARG_MONTH);
        }
        if (args.containsKey(ARG_DAY)) {
            argDay = args.getInt(ARG_DAY);
        }

        if (argYear != null && argMonth != null && argDay != null) {
            year = argYear;
            monthFromZero = argMonth - 1;
            day = argDay;
        }

        return new DatePickerDialog(getContext(), this, year, monthFromZero, day);
    }

    @Override
    public void onDateSet(DatePicker datePicker, int year, int monthFromZero, int day) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (!(activity instanceof Listener)) {
            return;
        }
        Listener listener = (Listener) activity;

        listener.onDatePickerDialogFragmentNewDate(year, monthFromZero + 1, day);
    }

    public interface Listener {
        void onDatePickerDialogFragmentNewDate(int year, int monthFromOne, int day);
    }
}
