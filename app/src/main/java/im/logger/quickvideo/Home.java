package im.logger.quickvideo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.ArrayList;


public class Home extends AppCompatActivity {

    public static final String PREFS_NAME = "wechat_video";

    private BaseAccessibilityService as = null;
    private EditText nickName = null;
    private Button btnOpenApp = null;
    private RecyclerView mRecyclerView = null;
    private PackageManager mPackageManager;
    private HomeAdapter mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "发送邮件，联系作者", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                Uri uri = Uri.parse ("mailto: kinka@imweb.io");
                Intent intent = new Intent (Intent.ACTION_SENDTO, uri);
                startActivity(intent);
            }
        });

        mPackageManager = getPackageManager();

        as = AccessibilityService.getInstance();
        as.init(this);

        nickName = (EditText)findViewById(R.id.nick_name);
        btnOpenApp = (Button)findViewById(R.id.open_app);

        btnOpenApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openApp();
            }
        });

        mAdapter = new HomeAdapter(this);
        mRecyclerView = (RecyclerView) findViewById(R.id.list_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

//        grantUriPermission();
    }

    private void openApp() {
        String strNickName = nickName.getText().toString();
        Toast.makeText(getApplicationContext(), strNickName, Toast.LENGTH_SHORT).show();

        Intent intent = mPackageManager.getLaunchIntentForPackage(AccessibilityService.MM_PNAME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        setValue(true, strNickName);

        startActivity(intent);
    }

    private void setValue(boolean v, String nickname) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("onekey", v);
        editor.putString("nickname", nickname);

        editor.commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mAdapter.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_settings:
                as.goAccess();
                break;
            case R.id.action_add:
                mAdapter.addAvatar();
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
