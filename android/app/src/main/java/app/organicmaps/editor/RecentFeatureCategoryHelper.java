package app.organicmaps.editor;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import app.organicmaps.MwmApplication;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class RecentFeatureCategoryHelper
{
  private static final String PREF_KEY = "RecentFeatureCategories";
  private static final String SEPARATOR = ",";
  private static final int MAX_RECENT = 5;

  @NonNull
  static List<String> getRecentTypes(@NonNull Context context)
  {
    String stored = MwmApplication.prefs(context).getString(PREF_KEY, "");
    if (stored == null || stored.isEmpty())
      return new ArrayList<>();
    return new ArrayList<>(Arrays.asList(stored.split(SEPARATOR)));
  }

  static void addRecentType(@NonNull Context context, @NonNull String type)
  {
    List<String> recents = getRecentTypes(context);
    recents.remove(type);
    recents.add(0, type);
    if (recents.size() > MAX_RECENT)
        recents.remove(recents.size() - 1);
    String stored = String.join(SEPARATOR, recents);
    MwmApplication.prefs(context).edit().putString(PREF_KEY, stored).apply();
  }
}
