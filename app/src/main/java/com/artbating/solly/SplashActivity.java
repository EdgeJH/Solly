package com.artbating.solly;

import android.*;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.crash.FirebaseCrash;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.util.ArrayList;


/**
 * Created by chunghoen on 2017-03-19.
 */

public class SplashActivity extends Activity {
    ImageView imageView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.splash);
        imageView = (ImageView) findViewById(R.id.image);
        Glide.with(getApplicationContext()).load(R.drawable.mainloading)
                .into(imageView);

        MusicService.firstStrart =1;
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
            PermissionListener permissionlistener = new PermissionListener() {
                @Override
                public void onPermissionGranted() {
                    goMain();

                }

                @Override
                public void onPermissionDenied(ArrayList<String> deniedPermissions) {
                    Toast.makeText(SplashActivity.this, deniedPermissions.toString() + "권한이 거부되었습니다", Toast.LENGTH_SHORT).show();
                }


            };
            new TedPermission(this)
                    .setPermissionListener(permissionlistener)
                    .setRationaleMessage("본 서비스 이용시 오디오 파일 재생을 위해 저장공간 권한이 필요합니다.")
                    .setDeniedMessage("설정을 거부하시면 본서비스를 이용이 어렵습니다\n\n[설정] > [권한]에서 해당 권한을 활성화 해주세요")
                    .setGotoSettingButtonText("설정")
                    .setPermissions(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    .check();
        }else {
            goMain();

        }

    }
    private void goMain(){
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent =new Intent(getApplicationContext(),MainActivity.class);
                overridePendingTransition(R.anim.fadein,R.anim.fadeout);
                startActivity(intent);
                finish();
                handler.removeCallbacks(this);
            }
        },1000);
    }
}
