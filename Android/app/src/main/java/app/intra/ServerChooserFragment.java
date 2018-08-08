/*
Copyright 2018 Jigsaw Operations LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package app.intra;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User interface for the server name selection.
 */

public class ServerChooserFragment extends PreferenceDialogFragmentCompat
    implements RadioGroup.OnCheckedChangeListener, TextWatcher, EditText.OnEditorActionListener {

    private static final String LOG_TAG = "ServerChooserFragment";
    private RadioGroup buttons = null;
    private EditText text = null;

    public static ServerChooserFragment newInstance(String key) {
        final ServerChooserFragment fragment = new ServerChooserFragment();
        final Bundle bundle = new Bundle(1);
        bundle.putString(ARG_KEY, key);
        fragment.setArguments(bundle);
        return fragment;
    }

    private String getName() {
        int checkedId = buttons.getCheckedRadioButtonId();
        if (checkedId == R.id.pref_server_switch) {
            return getResources().getString(R.string.server0);
        } else if (checkedId == R.id.pref_server_cloudflare) {
            return getResources().getString(R.string.server1);
        } else {
            return text.getText().toString();
        }
    }

    private void updateUI() {
        int checkedId = buttons.getCheckedRadioButtonId();
        boolean custom = checkedId == R.id.pref_server_custom;
        text.setEnabled(custom);
        if (custom) {
            setValid(checkName(getName()));
        } else {
            setValid(true);
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        updateUI();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        updateUI();
    }

    // Check that the server name is a plausible DoT server name
    private boolean checkName(String name) {
        return true;
        /*
        if ( isDomain(name) || isIp(name) ) {
            return true;
        }
        return false;
        */
    }

    private boolean isIp(String text) {
        Pattern p = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
        Matcher m = p.matcher(text);
        return m.find();
    }

    private boolean isDomain(String text) {
        Pattern p = Pattern.compile("^(?=.{1,253}\\.?$)(?:(?!-|[^.]+_)[A-Za-z0-9-_]{1,63}(?<!-)(?:\\.|$)){2,}$");
        Matcher m = p.matcher(text);
        return m.find();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        // Usability optimization:
        // If the user is typing in the free text field and presses "enter" or "go" on the keyboard
        // while the server name is valid, treat that the same as closing the keyboard and pressing "OK".
        if (checkName(v.getText().toString())) {
            Dialog dialog = getDialog();
            if (dialog instanceof AlertDialog) {
                Button ok = ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                ok.setEnabled(true);
                ok.performClick();
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        ServerChooser preference = (ServerChooser) getPreference();
        String serverName = preference.getServerName();
        buttons = view.findViewById(R.id.pref_server_radio_group);
        text = view.findViewById(R.id.custom_server_name);
        if (serverName == null || serverName.equals(getResources().getString(R.string.server0))) {
            buttons.check(R.id.pref_server_switch);
        } else if (serverName.equals(getResources().getString(R.string.server1))) {
            buttons.check(R.id.pref_server_cloudflare);
        } else {
            buttons.check(R.id.pref_server_custom);
            text.setText(serverName);
        }
        buttons.setOnCheckedChangeListener(this);
        text.addTextChangedListener(this);
        text.setOnEditorActionListener(this);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            ServerChooser preference = (ServerChooser) getPreference();
            preference.setServerName(getName());
        }
        text.removeTextChangedListener(this);
        text.setOnEditorActionListener(null);
        buttons.setOnCheckedChangeListener(null);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        updateUI();
    }

    private void setValid(boolean valid) {
        Dialog dialog = getDialog();
        if (dialog instanceof AlertDialog) {
            ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(valid);
        }
        dialog.findViewById(R.id.server_warning).setVisibility(valid ? View.INVISIBLE : View.VISIBLE);
    }
}
