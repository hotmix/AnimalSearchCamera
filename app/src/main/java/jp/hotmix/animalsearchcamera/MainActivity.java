package jp.hotmix.animalsearchcamera;


import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class MainActivity extends AppCompatActivity {
    private AutoFitTextureView mTextureView;
    private ImageView mImageView;
    private Camera2StateMachine mCamera2;

    // API21以下の時利用する
    private Camera mCam;
    private CameraPreview cameraPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){

            mTextureView = (AutoFitTextureView) findViewById(R.id.textureView);
            mImageView = (ImageView) findViewById(R.id.imageView);

            //APIレベル21以降の機種の場合の処理
            mCamera2 = new Camera2StateMachine();

        } else {

            // カメラインスタンスの取得
            try {
                mCam = Camera.open();
            } catch (Exception e) {
                // エラー
                this.finish();
            }

           // RelativeLayout preview = (RelativeLayout)findViewById(R.id.cameraView);
            cameraPreview = new CameraPreview(this, mCam);
           // preview.addView(cameraPreview);

        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            //APIレベル21以降の機種の場合の処理
            mCamera2.open(this, mTextureView);
        } else {

        }

    }
    @Override
    protected void onPause() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            //APIレベル21以降の機種の場合の処理
            mCamera2.close();
        } else {

            if (mCam != null) {
                mCam.release();
                mCam = null;
            }
        }

        super.onPause();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mImageView.getVisibility() == View.VISIBLE) {
            mTextureView.setVisibility(View.VISIBLE);
            mImageView.setVisibility(View.INVISIBLE);
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onClickShutter(View view) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){

            Log.d("onClickShutter","シャッターが押された");

            //APIレベル21以降の機種の場合の処理
            mCamera2.takePicture(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    takePictureByCamera2(reader);
                }
            });

        } else {

            mCam.takePicture(null, null, mPicJpgListener);

        }

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void takePictureByCamera2(ImageReader reader) {
        // 撮れた画像をImageViewに貼り付けて表示。
        final Image image = reader.acquireLatestImage();
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];

        buffer.get(bytes);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        image.close();

        savePicture(bitmap);

        mImageView.setImageBitmap(bitmap);
        mImageView.setVisibility(View.VISIBLE);
        mTextureView.setVisibility(View.INVISIBLE);
    }

    /**
     * JPEG データ生成完了時のコールバック
     */
    private Camera.PictureCallback mPicJpgListener = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            if (data == null) {
                return;
            }

            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            savePicture(bitmap);


            // takePicture するとプレビューが停止するので、再度プレビュースタート
            mCam.startPreview();

        }
    };

    /**
     * アンドロイドのデータベースへ画像のパスを登録
     * @param path 登録するパス
     */
    private void registAndroidDB(String path) {
        // アンドロイドのデータベースへ登録
        // (登録しないとギャラリーなどにすぐに反映されないため)
        ContentValues values = new ContentValues();
        ContentResolver contentResolver = MainActivity.this.getContentResolver();
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put("_data", path);
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    private void savePicture(Bitmap bitmap) {

        // SD カードフォルダを取得
        File file = new File(Environment.getExternalStorageDirectory(),"sampleDir");

        // フォルダ作成
        if (!file.exists()) {
            if (!file.mkdir()) {
                Log.e("Debug", "Make Dir Error");
            }
        }

        saveBitmapToStorage(bitmap, file);

    }

    private void saveBitmapToStorage(Bitmap bitmap, File file) {
        // 画像保存パス
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String imgPath = file.getPath() + "/" + sf.format(cal.getTime()) + ".jpg";

        // ファイル保存
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(imgPath, true);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();

            // アンドロイドのデータベースへ登録
            // (登録しないとギャラリーなどにすぐに反映されないため)
            registAndroidDB(imgPath);

        } catch (Exception e) {
            Log.e("Debug", e.getMessage());
        }

        if (fos != null) {
            fos = null;
        }
    }
}
