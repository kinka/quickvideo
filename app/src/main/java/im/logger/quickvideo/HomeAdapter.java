package im.logger.quickvideo;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.pm.ShortcutInfoCompat;
import android.support.v4.content.pm.ShortcutManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static android.app.Activity.RESULT_OK;
import static im.logger.quickvideo.Home.PREFS_NAME;

public class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.ViewHolder>{

    private AppCompatActivity mAct;
    private PackageManager mPackageManager;
    private ImageView mCurrentImage;
    private int mCurrentIndex;
    private ArrayList<HomeAdapter.Avatar> mAvatars;

    public HomeAdapter(AppCompatActivity activity) {
        this.mAct = activity;

        mPackageManager = mAct.getPackageManager();

        loadAvatars();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // 实例化展示的view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.avatar, parent, false);
        // 实例化viewholder
        ViewHolder viewHolder = new ViewHolder(v);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        // 绑定数据
        Avatar avatar = mAvatars.get(position);
        holder.mTv.setText(avatar.nickname);
        if (avatar.url.length() > 0) holder.image.setImageURI(Uri.parse(avatar.url));

        final ViewHolder innerHolder = holder;
        holder.btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String nickname = innerHolder.mTv.getText().toString();
                mAvatars.get(position).nickname = nickname;
                saveAvatars();
                openApp(nickname);
            }
        });

        holder.image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickImage(innerHolder.image, position);
            }
        });

        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAvatars.remove(position);
                saveAvatars();
                notifyDataSetChanged();
            }
        });

        holder.btnIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Avatar avatar = mAvatars.get(position);
                saveAvatars();
                addShortcut(avatar.nickname, Uri.parse(avatar.url));
            }
        });
    }

    @Override
    public int getItemCount() {
        return mAvatars == null ? 0 : mAvatars.size();
    }

    public void addAvatar() {
        mAvatars.add(new Avatar("", ""));
        notifyDataSetChanged();
    }

    public void openApp(String nickname) {
        Toast.makeText(mAct.getApplicationContext(), nickname, Toast.LENGTH_SHORT).show();

        Intent intent = mPackageManager.getLaunchIntentForPackage(AccessibilityService.MM_PNAME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        setValue(true, nickname);

        mAct.startActivity(intent);
    }

    public void pickImage(ImageView image, int currIndex) {
        mCurrentImage = image;
        mCurrentIndex = currIndex;

        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        mAct.startActivityForResult(intent, 0x1);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0x1 && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                mCurrentImage.setImageURI(uri);
                mAvatars.get(mCurrentIndex).url = uri.toString();
//                Toast.makeText(mAct, data.getData().toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void addShortcut(String nickname, Uri uri) {
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(mAct)) {
            Intent shortcutInfoIntent = new Intent(mAct, Home.class);
            shortcutInfoIntent.setAction(Intent.ACTION_VIEW); //action必须设置，不然报错
            shortcutInfoIntent.putExtra("nickname", nickname);

            Bitmap bitmap = null;
            try {
                bitmap = getThumbnail(uri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            ShortcutInfoCompat info = new ShortcutInfoCompat.Builder(mAct, nickname)
                    .setIcon(bitmap)
                    .setShortLabel(nickname)
                    .setIntent(shortcutInfoIntent)
                    .build();

            // 当添加快捷方式的确认弹框弹出来时，将被回调
            // TODO 目前receiver 并没有回调成功
            PendingIntent shortcutCallbackIntent = PendingIntent.getBroadcast(mAct, 0, new Intent(mAct, HomeAdapter.ShortcutReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
            ShortcutManagerCompat.requestPinShortcut(mAct, info, shortcutCallbackIntent.getIntentSender());
        }
    }

    // https://stackoverflow.com/a/6228188/1096852
    public Bitmap getThumbnail(Uri uri) throws FileNotFoundException, IOException{
        InputStream input = mAct.getContentResolver().openInputStream(uri);

        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither=true;//optional
        onlyBoundsOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();

        if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1)) {
            return null;
        }

        int originalSize = (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) ? onlyBoundsOptions.outHeight : onlyBoundsOptions.outWidth;

        int THUMBNAIL_SIZE = 100;
        double ratio = (originalSize > THUMBNAIL_SIZE) ? (originalSize / THUMBNAIL_SIZE) : 1.0;

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);
        bitmapOptions.inDither = true; //optional
        bitmapOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//
        input = mAct.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        input.close();
        return bitmap;
    }

    private int getPowerOfTwoForSampleRatio(double ratio){
        int k = Integer.highestOneBit((int)Math.floor(ratio));
        if(k==0) return 1;
        else return k;
    }

    private void setValue(boolean v, String nickname) {
        SharedPreferences settings = mAct.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("onekey", v);
        editor.putString("nickname", nickname);

        editor.apply();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView mTv;
        ImageView image;
        Button btnOpen;
        Button btnIcon;
        Button btnDelete;

        public ViewHolder(View itemView) {
            super(itemView);
            mTv = (EditText) itemView.findViewById(R.id.item_nickname);
            btnOpen = itemView.findViewById(R.id.item_open);
            btnIcon = itemView.findViewById(R.id.item_icon);
            btnDelete = itemView.findViewById(R.id.item_delete);
            image = itemView.findViewById(R.id.item_image);
         }
    }

    private void saveAvatars() {
        SharedPreferences settings = mAct.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("avatars", toJSON(this.mAvatars));

        editor.apply();

//        Toast.makeText(this.mAct.getApplicationContext(), toJSON(mAvatars), Toast.LENGTH_SHORT).show();
    }

    private void loadAvatars() {
        SharedPreferences settings = mAct.getSharedPreferences(PREFS_NAME, 0);
        String jsonStr = settings.getString("avatars", "[]");
        mAvatars = fromJSON(jsonStr);
//        Toast.makeText(this.mAct.getApplicationContext(), toJSON(mAvatars), Toast.LENGTH_SHORT).show();
    }

    public static class ShortcutReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context.getApplicationContext(), "创建快捷方式成功！", Toast.LENGTH_SHORT).show();
        }
    }

    public static String toJSON(ArrayList<Avatar> avatars) {
        JSONArray jsonArr = new JSONArray();
        try {
            for (Avatar avatar:avatars) {
                JSONObject obj = new JSONObject();
                obj.put("nickname", avatar.nickname);
                obj.put("url", avatar.url);
                jsonArr.put(obj);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonArr.toString();
    }

    public static ArrayList<Avatar> fromJSON(String json) {
        ArrayList<Avatar> avatars = new ArrayList<>();
        try {
            JSONTokener tokener = new JSONTokener(json);
            JSONArray arr = (JSONArray) tokener.nextValue();
            for (int i=0; i<arr.length(); i+=1) {
                JSONObject obj = arr.getJSONObject(i);
                Avatar avatar = new Avatar(obj.getString("nickname"), obj.getString("url"));
                avatars.add(avatar);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return avatars;
    }

    public static class Avatar {
        String nickname = "";
        String url = "";

        public Avatar(String nickname, String url) {
            this.nickname = nickname;
            this.url = url;
        }
    }
}