package dotson.cread.customlauncher;

import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static android.R.id.list;

public class HomeActivity extends AppCompatActivity {

    GridLayout mainLayout;

    ImageView battery_icon;
    ImageView wifiIcon;
    ImageView locationIcon;
    ImageView networkIcon;
    EditText search_bar;
    TextView battery_pct;

    GridView suggestions;

    private PowerConnectionReceiver receiver;
    private IntentFilter filter;

    private SearchBarHandler searchBarHandler;
    private SearchBarEnterHandler searchBarEnterHandler;

    private PackageManager manager;
    private List<AppDetail> apps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(HomeActivity.this);
        final Drawable wallpaperDrawable = wallpaperManager.getDrawable();

        loadApps();

        mainLayout = (GridLayout) findViewById(R.id.main_grid_layout);
        mainLayout.setBackground(wallpaperDrawable);

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        battery_icon = (ImageView) findViewById(R.id.battery_icon);
        filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        receiver = new PowerConnectionReceiver();
        Intent batteryStatus = getApplicationContext().registerReceiver(receiver, filter);
        battery_pct = (TextView) findViewById(R.id.battery_pct);

        search_bar = (EditText) findViewById(R.id.mainSearchBar);
        searchBarEnterHandler = new SearchBarEnterHandler();
        search_bar.setOnEditorActionListener(searchBarEnterHandler);
        searchBarHandler = new SearchBarHandler();
        search_bar.addTextChangedListener(searchBarHandler);

        suggestions = new GridView(HomeActivity.this);
        suggestions.setNumColumns(6);

    }

    private List<AppDetail> findApps(String seq){
        List<AppDetail> foundApps = new ArrayList<AppDetail>();
        for(AppDetail app: apps){
            String label = app.label.toString().toLowerCase();
            if(label.contains(seq.toLowerCase())){
                foundApps.add(app);
            }
        }
        return foundApps;
    }

    private void loadListView(String seq){

        final List<AppDetail> foundApps = findApps(seq);

        ArrayAdapter<AppDetail> adapter = new ArrayAdapter<AppDetail>(this, R.layout.list_item, foundApps) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if(convertView == null){
                    convertView = getLayoutInflater().inflate(R.layout.list_item, null);
                }

                ImageView appIcon = (ImageView)convertView.findViewById(R.id.item_app_icon);
                appIcon.setImageDrawable(foundApps.get(position).icon);

                TextView appLabel = (TextView)convertView.findViewById(R.id.item_app_label);
                appLabel.setText(foundApps.get(position).label);

                return convertView;
            }
        };

        suggestions.setAdapter(adapter);
        addClickListener(foundApps);
    }

    private void addClickListener(final List<AppDetail> foundApps){
        suggestions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> av, View v, int pos, long id) {
                Intent i = manager.getLaunchIntentForPackage(foundApps.get(pos).name.toString());
                HomeActivity.this.startActivity(i);
            }
        });
    }

    private void loadApps(){
        manager = getPackageManager();
        apps = new ArrayList<AppDetail>();

        Intent i = new Intent(Intent.ACTION_MAIN, null);
        i.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> availableActivities = manager.queryIntentActivities(i, 0);
        for(ResolveInfo ri:availableActivities){
            AppDetail app = new AppDetail();
            app.label = ri.loadLabel(manager);
            app.name = ri.activityInfo.packageName;
            app.icon = ri.activityInfo.loadIcon(manager);
            apps.add(app);
        }
    }

    public class SearchBarEnterHandler implements TextView.OnEditorActionListener {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String text = v.getText().toString();
                openApp(text);
                return true;
            }
            return false;
        }
    }

    private void openApp(String name){
        for(AppDetail app : apps){
            if(app.label.toString().equalsIgnoreCase(name)){
                Intent i = manager.getLaunchIntentForPackage(app.name.toString());
                HomeActivity.this.startActivity(i);
                return;
            }
        }
        Toast.makeText(HomeActivity.this,"No App Found", Toast.LENGTH_SHORT).show();
    }

    public class SearchBarHandler implements TextWatcher {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count){
            if(s != null && s.length() > 0){
                loadListView(s.toString());
                suggestions.setVisibility(View.VISIBLE);
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                mainLayout.removeAllViews();
                mainLayout.addView(suggestions, params);
            } else {
                mainLayout.removeAllViews();
            }
        }
        @Override
        public void afterTextChanged(Editable e){

        }
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
    }

    public class PowerConnectionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;

            int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
            boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
            float batteryPct = level / (float)scale;

            setBattery_icon(batteryPct, isCharging);
        }
    }

    private void setBattery_icon(float batteryPct, boolean isCharging){
        battery_pct.setText(Math.round(batteryPct*2.0) + "");
        int batPct = (int) Math.floor((batteryPct*2)/(100/8));
        switch (batPct){
            case 8:
                battery_icon.setImageResource(R.mipmap.ic_battery_08);
                break;
            case 7:
                battery_icon.setImageResource(R.mipmap.ic_battery_07);
                break;
            case 6:
                battery_icon.setImageResource(R.mipmap.ic_battery_06);
                break;
            case 5:
                battery_icon.setImageResource(R.mipmap.ic_battery_05);
                break;
            case 4:
                battery_icon.setImageResource(R.mipmap.ic_battery_04);
                break;
            case 3:
                battery_icon.setImageResource(R.mipmap.ic_battery_03);
                break;
            case 2:
                battery_icon.setImageResource(R.mipmap.ic_battery_02);
                break;
            case 1:
                battery_icon.setImageResource(R.mipmap.ic_battery_01);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause() {
        if(receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
        super.onPause();
    }

}