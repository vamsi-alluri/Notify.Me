package com.example.notifyme;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.notifyme.Service.OnStatusChangeListener;





public class MainActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            MainFragment fragment = new MainFragment();
            getFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commit();
        }
    }

    public static class MainFragment extends PreferenceFragment implements OnPreferenceClickListener, OnSharedPreferenceChangeListener {
        private Preference pStatus, pDeviceState, pQuietStart, pQuietEnd, pTest, pNotifyLog, pSupport;
        private final OnStatusChangeListener statusListener = new OnStatusChangeListener() {
            @Override
            public void onStatusChanged() {
                updateStatus();
            }
        };

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Common.init(getActivity());
            addPreferencesFromResource(R.xml.preferences);
            pStatus = findPreference(getString(R.string.key_status));
            pStatus.setOnPreferenceClickListener(this);
            pDeviceState = findPreference(getString(R.string.key_device_state));
            pDeviceState.setOnPreferenceClickListener(this);
            findPreference(getString(R.string.key_appList)).setIntent(new Intent(getActivity(), AppListActivity.class));
            Preference pTTS = findPreference(getString(R.string.key_ttsSettings));
            Intent ttsIntent = getTtsIntent();
            if (ttsIntent != null) {
                pTTS.setIntent(ttsIntent);
            } else {
                pTTS.setEnabled(false);
                pTTS.setSummary(R.string.tts_settings_summary_fail);
            }
            EditTextPreference pTtsString = (EditTextPreference)findPreference(getString(R.string.key_ttsString));
            if (pTtsString.getText().contains("%")) {
                Toast.makeText(getActivity(), R.string.tts_message_reset_default, Toast.LENGTH_LONG).show();
                pTtsString.setText(getString(R.string.ttsString_default_value));
            }
        }

        private Intent getTtsIntent() {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            if (checkActivityExist("com.android.settings.TextToSpeechSettings")) {
                intent.setClassName("com.android.settings", "com.android.settings.TextToSpeechSettings");
            } else if (checkActivityExist("com.android.settings.Settings$TextToSpeechSettingsActivity")) {
                intent.setClassName("com.android.settings", "com.android.settings.Settings$TextToSpeechSettingsActivity");
            } else if (checkActivityExist("com.google.tv.settings.TextToSpeechSettingsTop")) {
                intent.setClassName("com.google.tv.settings", "com.google.tv.settings.TextToSpeechSettingsTop");
            } else return null;
            return intent;
        }

        private boolean checkActivityExist(String name) {
            try {
                PackageInfo pkgInfo = getActivity().getPackageManager().getPackageInfo(
                        name.substring(0, name.lastIndexOf(".")), PackageManager.GET_ACTIVITIES);
                if (pkgInfo.activities != null) {
                    for (int n = 0; n < pkgInfo.activities.length; n++) {
                        if (pkgInfo.activities[n].name.equals(name)) return true;
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (preference == pStatus && Service.isRunning() && Service.isSuspended()) {
                Service.toggleSuspend();
                return true;
            } else if (preference == pDeviceState) {
                MyDialog.show(getFragmentManager(), MyDialog.ID.DEVICE_STATE);
                return true;
            }
            return false;
        }

        private void updateStatus() {
            if (Service.isSuspended() && Service.isRunning()) {
                pStatus.setTitle(R.string.service_suspended);
                pStatus.setSummary(R.string.status_summary_suspended);
                pStatus.setIntent(null);
            } else {
                pStatus.setTitle(Service.isRunning() ? R.string.service_running : R.string.service_disabled);
                if (NotificationManagerCompat.getEnabledListenerPackages(getActivity()).contains(getActivity().getPackageName())) {
                    pStatus.setSummary(R.string.status_summary_notification_access_enabled);
                } else {
                    pStatus.setSummary(R.string.status_summary_notification_access_disabled);
                }
                pStatus.setIntent(Common.getNotificationListenerSettingsIntent());
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            Common.getPrefs(getActivity()).registerOnSharedPreferenceChangeListener(this);
            Service.registerOnStatusChangeListener(statusListener);
            updateStatus();
        }

        @Override
        public void onPause() {
            Service.unregisterOnStatusChangeListener(statusListener);
            Common.getPrefs(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
            if (key.equals(getString(R.string.key_ttsStream))) {
                Common.setVolumeStream(getActivity());
            }
        }
    }

    public static class MyDialog extends DialogFragment {
        private static final String KEY_ID = "id";

        private enum ID {
            DEVICE_STATE,

        }

        private final TimePickerDialog.OnTimeSetListener sTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                Common.getPrefs(getActivity()).edit().putInt(getString(R.string.key_quietStart), hourOfDay * 60 + minute).apply();
            }
        };
        private final TimePickerDialog.OnTimeSetListener eTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                Common.getPrefs(getActivity()).edit().putInt(getString(R.string.key_quietEnd), hourOfDay * 60 + minute).apply();
            }
        };

        public MyDialog() {}

        private static void show(FragmentManager fm, ID id) {
            Bundle bundle = new Bundle();
            bundle.putSerializable(KEY_ID, id);
            MyDialog dialogFragment = new MyDialog();
            dialogFragment.setArguments(bundle);
            dialogFragment.show(fm, id.name());
        }

        /**
         * @return The intent for Google Wallet, otherwise null if installation is not found.
         */
        private Intent getWalletIntent() {
            String walletPackage = "com.google.android.apps.gmoney";
            PackageManager pm = getActivity().getPackageManager();
            try {
                pm.getPackageInfo(walletPackage, PackageManager.GET_ACTIVITIES);
                return pm.getLaunchIntentForPackage(walletPackage);
            } catch (PackageManager.NameNotFoundException e) {
                return null;
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            ID id = (ID)getArguments().getSerializable(KEY_ID);
            assert id != null; // Prevent Lint warning. Should never be null, I want a crash report if it is.
            switch (id) {
                case DEVICE_STATE:
                    final CharSequence[] items = getResources().getStringArray(R.array.device_states);
                    return new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.device_state_dialog_title)
                            .setMultiChoiceItems(items,
                                    new boolean[]{
                                            Common.getPrefs(getActivity()).getBoolean(Common.KEY_SPEAK_SCREEN_OFF, true),
                                            Common.getPrefs(getActivity()).getBoolean(Common.KEY_SPEAK_SCREEN_ON, true),
                                            Common.getPrefs(getActivity()).getBoolean(Common.KEY_SPEAK_HEADSET_OFF, true),
                                            Common.getPrefs(getActivity()).getBoolean(Common.KEY_SPEAK_HEADSET_ON, true),
                                            Common.getPrefs(getActivity()).getBoolean(Common.KEY_SPEAK_SILENT_ON, false)
                                    },
                                    new DialogInterface.OnMultiChoiceClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                            switch (which) {
                                                case 0:  // Screen off
                                                    Common.getPrefs(getActivity()).edit().putBoolean(Common.KEY_SPEAK_SCREEN_OFF, isChecked).apply();
                                                    break;
                                                case 1:  // Screen on
                                                    Common.getPrefs(getActivity()).edit().putBoolean(Common.KEY_SPEAK_SCREEN_ON, isChecked).apply();
                                                    break;
                                                case 2:  // Headset off
                                                    Common.getPrefs(getActivity()).edit().putBoolean(Common.KEY_SPEAK_HEADSET_OFF, isChecked).apply();
                                                    break;
                                                case 3:  // Headset on
                                                    Common.getPrefs(getActivity()).edit().putBoolean(Common.KEY_SPEAK_HEADSET_ON, isChecked).apply();
                                                    break;
                                                case 4:  // Silent/vibrate
                                                    Common.getPrefs(getActivity()).edit().putBoolean(Common.KEY_SPEAK_SILENT_ON, isChecked).apply();
                                                    break;
                                            }
                                        }
                                    }
                            ).create();
            }
            return null;
        }
    }
}
