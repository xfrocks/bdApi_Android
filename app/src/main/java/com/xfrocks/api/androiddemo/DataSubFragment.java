package com.xfrocks.api.androiddemo;

import android.os.Bundle;

import com.xfrocks.api.androiddemo.persist.Row;

import java.util.Collections;
import java.util.List;

public class DataSubFragment extends DataFragment {

    private static final String ARG_ROWS = "rows";

    public static DataSubFragment newInstance(List<Row> rows) {
        DataSubFragment fragment = new DataSubFragment();

        Bundle args = new Bundle();
        args.putParcelableArray(ARG_ROWS, rows.toArray(new Row[rows.size()]));
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();

        setListAdapterSafe();

        Bundle args = getArguments();
        if (args.containsKey(ARG_ROWS)) {
            mData.clear();

            Row[] rows = (Row[]) args.getParcelableArray(ARG_ROWS);
            if (rows != null) {
                Collections.addAll(mData, rows);
                mDataAdapter.notifyDataSetInvalidated();
            }
        }
    }
}