package app.intra;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import app.intra.util.Untemplate;

/**
 * Static class representing on-disk storage of mutable state.  Collecting this all in one class
 * helps to reduce duplication of code using SharedPreferences, allows settings to be read and
 * written by separate components, and also helps to improve preference naming consistency.
 */
public class PersistentState {
  private static final String LOG_TAG = "PersistentState";

  public static final String APPS_KEY = "pref_apps";
  public static final String SERVER_NAME_KEY = "pref_server_name";

  private static final String APPROVED_KEY = "approved";
  private static final String ENABLED_KEY = "enabled";
  private static final String EXTRA_SERVERS_V4_KEY = "extraServersV4";
  private static final String EXTRA_SERVERS_V6_KEY = "extraServersV6";
  private static final String SERVER_KEY = "server";

  private static final String INTERNAL_STATE_NAME = "MainActivity";

  // The approval state is currently stored in a separate preferences file.
  // TODO: Unify preferences into a single file.
  private static final String APPROVAL_PREFS_NAME = "IntroState";

  private static SharedPreferences getInternalState(Context context) {
    return context.getSharedPreferences(INTERNAL_STATE_NAME, Context.MODE_PRIVATE);
  }

  private static SharedPreferences getUserPreferences(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context);
  }

  public static boolean getVpnEnabled(Context context) {
    return getInternalState(context).getBoolean(ENABLED_KEY, false);
  }

  public static void setVpnEnabled(Context context, boolean enabled) {
    SharedPreferences.Editor editor = getInternalState(context).edit();
    editor.putBoolean(ENABLED_KEY, enabled);
    editor.apply();
  }

  // Apart from syncLegacyState() above, the URL is only set by the PreferenceScreen, not by Intra.
  private static void setServerUrl(Context context, String url) {
    SharedPreferences.Editor editor = getUserPreferences(context).edit();
    editor.putString(SERVER_NAME_KEY, url);
    editor.apply();
  }

  public static String getServerName(Context context) {
    String srvTemplate = getUserPreferences(context).getString(SERVER_NAME_KEY, null);
    if (srvTemplate == null) {
      return context.getResources().getString(R.string.server0);
    }
    return Untemplate.strip(srvTemplate);
  }

  private static SharedPreferences getApprovalSettings(Context context) {
    return context.getSharedPreferences(APPROVAL_PREFS_NAME, Context.MODE_PRIVATE);
  }

  public static boolean getWelcomeApproved(Context context) {
    return getApprovalSettings(context).getBoolean(APPROVED_KEY, false);
  }

  public static void setWelcomeApproved(Context context, boolean approved) {
    SharedPreferences.Editor editor = getApprovalSettings(context).edit();
    editor.putBoolean(APPROVED_KEY, approved);
    editor.apply();
  }

  public static Set<String> getExcludedPackages(Context context) {
    return getUserPreferences(context).getStringSet(APPS_KEY, new HashSet<String>());
  }
}
