package com.trialapp;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ErrorFragment extends Fragment {

    private static final String ERR_TEXT = "ERR_TEXT";

    private String errText;

    public ErrorFragment() {
        // Required empty public constructor
    }

    public static ErrorFragment newInstance(String error) {
        ErrorFragment fragment = new ErrorFragment();
        Bundle args = new Bundle();
        args.putString(ERR_TEXT, error);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            errText = getArguments().getString(ERR_TEXT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_error, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.<TextView>findViewById(R.id.tv_error).setText(errText);
    }
}