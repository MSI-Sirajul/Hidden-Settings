package com.hidden.settings;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1500; // 2 সেকেন্ড

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // থিম সেট করার দরকার নেই, মেইন থিম স্বয়ংক্রিয়ভাবে প্রয়োগ হবে
        super.onCreate(savedInstanceState);

        setContentView(R.layout.splash_activity);

        ImageView gifImage = findViewById(R.id.gifImage);

        // Glide দিয়ে GIF লোড করা (অ্যানিমেটেড)
        Glide.with(this)
                .asGif()
                .load(R.drawable.load)
                .into(gifImage);

        // ৩ সেকেন্ড পর MainActivity চালানো
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, SPLASH_DELAY_MS);
    }
}
