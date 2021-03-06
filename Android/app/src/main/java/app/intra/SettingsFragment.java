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

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v14.preference.MultiSelectListPreference;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * UI Fragment for displaying user preferences.
 * See https://developer.android.com/guide/topics/ui/settings#Fragment
 *
 * This class contains the special logic for the app exclusion setting, which does not require a
 * custom UI.  The server selection preference requires a custom UI, so it has its own class.
 */

public class SettingsFragment extends PreferenceFragmentCompat {
  private List<String> labels;
  private List<String> packageNames;
  private MultiSelectListPreference apps = null;

  /**
   * Custom pair class for labels (human-readable names) and package names (Java-style package
   * identifiers).  This class's primary purpose is to expose a custom ordering, where entries
   * are sorted by label (if non-null) before packageName, and Entries with null labels are placed
   * after those with non-null labels.
   */
  private class Entry implements Comparable<Entry> {
    public String label;  // Human-readable app name (may be null)
    public String packageName;  // Java-style package name (non-null)

    Entry(@Nullable String label, @NonNull String packageName) {
      this.label = label;
      this.packageName = packageName;
    }

    @Override
    public int compareTo(@NonNull Entry other) {
      if (label == null) {
        if (other.label == null) {
          return packageName.compareTo(other.packageName);
        }
        return -1;
      } else if (other.label == null) {
        return 1;
      } else if (!label.equals(other.label)) {
        return label.compareTo(other.label);
      }
      return packageName.compareTo(other.packageName);
    }
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    // Load the preferences from an XML resource
    addPreferencesFromResource(R.xml.preferences);
    apps = (MultiSelectListPreference)findPreference(PersistentState.APPS_KEY);
    // App exclusion relies on VpnService.Builder.addDisallowedApplication, which was added in L.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      apps.setEntries(labels.toArray(new String[0]));
      apps.setEntryValues(packageNames.toArray(new String[0]));
    } else {
      apps.setEnabled(false);
      apps.setSummary(R.string.old_android);
    }
  }

  @Override
  public void onDisplayPreferenceDialog(Preference preference) {
    if (preference instanceof ServerChooser) {
      DialogFragment dialogFragment = ServerChooserFragment.newInstance(preference.getKey());
      dialogFragment.setTargetFragment(this, 0);
      dialogFragment.show(getFragmentManager(), null);
    } else {
      super.onDisplayPreferenceDialog(preference);
    }
  }

  /**
   * Update this SettingsFragment's list of installed apps for the "Excluded apps" setting.  This
   * method must be called before the fragment is displayed.
   * @param pm The global PackageManager.
   */
  public void updateInstalledApps(PackageManager pm) {
    List<ApplicationInfo> infos = pm.getInstalledApplications(0);
    ArrayList<Entry> entries = new ArrayList<>(infos.size());
    for (ApplicationInfo info : infos) {
      if ("app.intra".equals(info.packageName)) {
        // Showing ourselves in the list of apps would be confusing and useless.
        continue;
      }
      if (pm.getLaunchIntentForPackage(info.packageName) == null) {
        // This is not an app that can actually be launched, so it's confusing to show it.
        continue;
      }
      entries.add(new Entry(info.loadLabel(pm).toString(), info.packageName));
    }
    Collections.sort(entries);
    labels = new ArrayList<>(entries.size());
    packageNames = new ArrayList<>(entries.size());
    for (Entry e : entries) {
      labels.add(e.label);
      packageNames.add(e.packageName);
    }
  }
}
