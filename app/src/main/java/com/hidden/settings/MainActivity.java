package com.hidden.settings;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.graphics.Typeface;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import java.util.*;
import android.net.Uri;

public class MainActivity extends AppCompatActivity {

	LinearLayout container;
	EditText searchBar;
	TextView warningText;

	int bgColor, textColor, borderColor;

	SharedPreferences prefs;

	List<String> allActivityNames = new ArrayList<>();
	Map<String, ActivityItem> nameToItemMap = new HashMap<>();
	Map<String, Map<String, ActivityItem>> categorizedMap = new LinkedHashMap<>();

	private static class ActivityItem {
		String label;
		Drawable icon;

		ActivityItem(String label, Drawable icon) {
			this.label = label;
			this.icon = icon;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
		super.onCreate(savedInstanceState);

		prefs = getSharedPreferences("favorites", MODE_PRIVATE);

		// থিম অনুযায়ী কালার সেট
		int mode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
		if (mode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
			bgColor = ContextCompat.getColor(this, R.color.black);
			textColor = ContextCompat.getColor(this, R.color.white);
			borderColor = ContextCompat.getColor(this, R.color.search_color);
		} else {
			bgColor = ContextCompat.getColor(this, R.color.white);
			textColor = ContextCompat.getColor(this, R.color.text_light);
			borderColor = ContextCompat.getColor(this, R.color.border_light);
		}

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setBackgroundColor(bgColor);
		
		// Title bar container
		LinearLayout titleBar = new LinearLayout(this);
		titleBar.setOrientation(LinearLayout.HORIZONTAL);
		titleBar.setGravity(Gravity.END | Gravity.CENTER_VERTICAL); // সব কিছু ডানে বসবে
		titleBar.setPadding(20, 20, 20, 20);
		titleBar.setBackgroundColor(bgColor);

		// Title TextView
		TextView titleText = new TextView(this);
		titleText.setText("All Settings Here");
		titleText.setTextSize(20);
		titleText.setTextColor(textColor);
		titleText.setTypeface(null, Typeface.BOLD);
		LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT,
				1f); // এক্সপ্যান্ড করবে, যাতে প্রোফাইল আইকন ডানে ঠেলে দেয়
		titleText.setLayoutParams(titleParams);
		titleBar.addView(titleText);

		// Profile icon
		ImageView profileIcon = new ImageView(this);
		profileIcon.setImageResource(R.drawable.profile);
		LinearLayout.LayoutParams profileParams = new LinearLayout.LayoutParams(50, 50);
		profileIcon.setLayoutParams(profileParams);
		titleBar.addView(profileIcon);

		// Icon click → open link in Chrome
		profileIcon.setOnClickListener(v -> {
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://md-sirajul-islam.vercel.app/"));
			startActivity(browserIntent);
		});
		root.addView(titleBar);
		
		// Warning
		warningText = new TextView(this);
		warningText.setText("SOME SETTINGS MAY NOT WORK PROPERLY");
		warningText.setTextColor(ContextCompat.getColor(this, R.color.hint_color));
		warningText.setTextSize(16);
		warningText.setPadding(24, 0, 24, 16);
		root.addView(warningText);
		warningText.setGravity(Gravity.CENTER);

		// Search Bar
		searchBar = new EditText(this);
		searchBar.setHint("Search activities...");
		searchBar.setHintTextColor(ContextCompat.getColor(this, R.color.search_color));
		searchBar.setTextColor(textColor);
		searchBar.setPadding(32, 16, 32, 16);
		searchBar.setBackgroundResource(R.drawable.item_background);
		root.addView(searchBar);

		// Scrollable Area
		ScrollView scrollView = new ScrollView(this);
		container = new LinearLayout(this);
		container.setOrientation(LinearLayout.VERTICAL);
		container.setPadding(24, 24, 24, 24);
		scrollView.addView(container);
		root.addView(scrollView);

		setContentView(root);

		searchBar.addTextChangedListener(new android.text.TextWatcher() {
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			public void onTextChanged(CharSequence s, int start, int before, int count) {
				filterActivities(s.toString().trim());
			}

			public void afterTextChanged(android.text.Editable s) {
			}
		});

		listAllSettingsActivities();
	}

	private void listAllSettingsActivities() {
		try {
			PackageManager pm = getPackageManager();
			ActivityInfo[] activities = pm.getPackageInfo("com.android.settings",
					PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA).activities;

			Drawable defaultIcon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_manage);

			for (ActivityInfo activity : activities) {
				if (activity.exported) {
					String name = activity.name;
					String label;
					Drawable icon;
					try {
						label = activity.loadLabel(pm).toString();
					} catch (Exception e) {
						label = name;
					}
					try {
						icon = activity.loadIcon(pm);
					} catch (Exception e) {
						icon = defaultIcon;
					}
					if (icon == null)
						icon = defaultIcon;

					allActivityNames.add(name);
					nameToItemMap.put(name, new ActivityItem(label, icon));
				}
			}

			categorizeActivities();
			renderUI(allActivityNames);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void categorizeActivities() {
		categorizedMap.clear();
		categorizedMap.put("Favorites", new LinkedHashMap<>());
		categorizedMap.put("Apps", new LinkedHashMap<>());
		categorizedMap.put("Display", new LinkedHashMap<>());
		categorizedMap.put("System", new LinkedHashMap<>());
		categorizedMap.put("Others", new LinkedHashMap<>());

		Set<String> favSet = prefs.getStringSet("fav_list", new HashSet<>());

		for (String name : allActivityNames) {
			ActivityItem item = nameToItemMap.get(name);
			if (favSet.contains(name)) {
				categorizedMap.get("Favorites").put(name, item);
			} else if (name.toLowerCase().contains("display")) {
				categorizedMap.get("Display").put(name, item);
			} else if (name.toLowerCase().contains("apps") || name.toLowerCase().contains("manageapplications")) {
				categorizedMap.get("Apps").put(name, item);
			} else if (name.toLowerCase().contains("system") || name.toLowerCase().contains("deviceinfo")) {
				categorizedMap.get("System").put(name, item);
			} else {
				categorizedMap.get("Others").put(name, item);
			}
		}
	}

	private void renderUI(List<String> filteredList) {
		container.removeAllViews();

		for (String category : categorizedMap.keySet()) {
			Map<String, ActivityItem> map = categorizedMap.get(category);
			List<Map.Entry<String, ActivityItem>> list = new ArrayList<>();
			for (Map.Entry<String, ActivityItem> entry : map.entrySet()) {
				if (filteredList.contains(entry.getKey())) {
					list.add(entry);
				}
			}

			if (!list.isEmpty()) {
				TextView header = new TextView(this);
				header.setText(category);
				header.setTextColor(textColor);
				header.setTextSize(18);
				header.setPadding(8, 24, 8, 8);
				container.addView(header);

				for (Map.Entry<String, ActivityItem> entry : list) {
					addActivityItem(entry.getKey(), entry.getValue());
				}
			}
		}

		if (container.getChildCount() == 0) {
			TextView empty = new TextView(this);
			empty.setText("No activity found.");
			empty.setTextColor(ContextCompat.getColor(this, R.color.hint_color));
			empty.setGravity(Gravity.CENTER);
			empty.setPadding(20, 100, 20, 20);
			container.addView(empty);
		}
	}

	private void addActivityItem(String name, ActivityItem item) {
		LinearLayout row = new LinearLayout(this);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setPadding(16, 24, 16, 24);
		row.setGravity(Gravity.CENTER_VERTICAL);

		// Icon
		ImageView iconView = new ImageView(this);
		iconView.setImageDrawable(item.icon);
		LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(40, 40);
		iconParams.setMargins(0, 0, 32, 0);
		iconView.setLayoutParams(iconParams);

		// Text
		TextView textView = new TextView(this);
		textView.setText(item.label);
		textView.setTextColor(textColor);
		textView.setTextSize(16);
		textView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

		// Favorite Icon
		ImageView favIcon = new ImageView(this);
		Set<String> favSet = prefs.getStringSet("fav_list", new HashSet<>());
		boolean isFav = favSet.contains(name);
		favIcon.setImageResource(isFav ? R.drawable.fav : R.drawable.nfav);
		LinearLayout.LayoutParams favParams = new LinearLayout.LayoutParams(40, 40);
		favIcon.setLayoutParams(favParams);
		favIcon.setOnClickListener(v -> {
			Set<String> updatedFav = new HashSet<>(prefs.getStringSet("fav_list", new HashSet<>()));
			if (updatedFav.contains(name)) {
				updatedFav.remove(name);
				favIcon.setImageResource(R.drawable.nfav);
			} else {
				updatedFav.add(name);
				favIcon.setImageResource(R.drawable.fav);
			}
			prefs.edit().putStringSet("fav_list", updatedFav).apply();
			categorizeActivities();
			renderUI(allActivityNames);
		});

		row.setOnClickListener(v -> {
			try {
				Intent intent = new Intent();
				intent.setComponent(new ComponentName("com.android.settings", name));
				startActivity(intent);
			} catch (Exception e) {
				Toast.makeText(this, "Couldn't launch this setting.", Toast.LENGTH_SHORT).show();
			}
		});

		row.addView(iconView);
		row.addView(textView);
		row.addView(favIcon);

		container.addView(row);

		// Divider
		View divider = new View(this);
		LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
				1);
		dividerParams.setMargins(16, 8, 16, 8);
		divider.setLayoutParams(dividerParams);
		divider.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray));
		container.addView(divider);
	}

	private void filterActivities(String query) {
		if (query.isEmpty()) {
			renderUI(allActivityNames);
		} else {
			List<String> filtered = new ArrayList<>();
			for (String name : allActivityNames) {
				ActivityItem item = nameToItemMap.get(name);
				if (name.toLowerCase().contains(query.toLowerCase())
						|| (item != null && item.label.toLowerCase().contains(query.toLowerCase()))) {
					filtered.add(name);
				}
			}
			renderUI(filtered);
		}
	}
}
