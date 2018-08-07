package app.intra;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
  public static final String URL_KEY = "pref_server_url";

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

  public static void syncLegacyState(Context context) {
    // Copy the domain choice into the new URL setting, if necessary.
    if (getServerUrl(context) != null) {
      // New server URL is already populated
      return;
    }

    // There is no URL setting, so read the legacy server name.
    SharedPreferences settings = getInternalState(context);
    String defaultDomain = context.getResources().getStringArray(R.array.domains)[0];
    String domain = settings.getString(SERVER_KEY, defaultDomain);

    if (domain == null) {
      // Legacy setting is in the default state, so we can leave the new URL setting in the default
      // state as well.
      return;
    }

    // Get the corresponding URL.
    String[] domains = context.getResources().getStringArray(R.array.domains);
    String[] urls = context.getResources().getStringArray(R.array.urls);
    String url = null;
    for (int i = 0; i < domains.length; ++i) {
      if (domains[i].equals(domain)) {
        url = urls[i];
        break;
      }
    }

    if (url == null) {
      Log.w(LOG_TAG, "Legacy domain is unrecognized");
      return;
    }
    setServerUrl(context, url);
  }

  // Apart from syncLegacyState() above, the URL is only set by the PreferenceScreen, not by Intra.
  private static void setServerUrl(Context context, String url) {
    SharedPreferences.Editor editor = getUserPreferences(context).edit();
    editor.putString(URL_KEY, url);
    editor.apply();
  }

  public static String getServerUrl(Context context) {
    String urlTemplate = getUserPreferences(context).getString(URL_KEY, null);
    if (urlTemplate == null) {
      return null;
    }
    return Untemplate.strip(urlTemplate);
  }

  public static String getServerName(Context context) {
    String url = getServerUrl(context);
    if (url == null || url.isEmpty()) {
      return context.getResources().getString(R.string.domain0);
    }

    try {
      URL parsed = new URL(url);
      return parsed.getHost();
    } catch (MalformedURLException e) {
      Log.w(LOG_TAG, "Stored URL is corrupted");
      return null;
    }
  }

  public static Set<String> getExtraGoogleV4Servers(Context context) {
    return getInternalState(context).getStringSet(EXTRA_SERVERS_V4_KEY, new HashSet<String>());
  }

  public static void setExtraGoogleV4Servers(Context context, String[] servers) {
    SharedPreferences.Editor editor = getInternalState(context).edit();
    editor.putStringSet(EXTRA_SERVERS_V4_KEY,
        new HashSet<String>(Arrays.asList(servers)));
    editor.apply();
  }

  public static Set<String> getExtraGoogleV6Servers(Context context) {
    return getInternalState(context).getStringSet(EXTRA_SERVERS_V6_KEY, new HashSet<String>());
  }

  public static void setExtraGoogleV6Servers(Context context, String[] servers) {
    SharedPreferences.Editor editor = getInternalState(context).edit();
    editor.putStringSet(EXTRA_SERVERS_V6_KEY,
        new HashSet<String>(Arrays.asList(servers)));
    editor.apply();
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
