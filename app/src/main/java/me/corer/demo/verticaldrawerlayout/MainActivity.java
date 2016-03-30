package me.corer.demo.verticaldrawerlayout;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import me.corer.verticaldrawerlayout.VerticalDrawerLayout;

public class MainActivity extends AppCompatActivity {

    VerticalDrawerLayout mDrawerLayout;
    ImageView mArrow;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDrawerLayout = (VerticalDrawerLayout) findViewById(R.id.vertical);
        mArrow = (ImageView) findViewById(R.id.img);


        mDrawerLayout.setDrawerListener(new VerticalDrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                mArrow.setRotation(slideOffset*180);
            }
        });
    }


    public void onClick(View v) {
        if (mDrawerLayout.isDrawerOpen()) {
            mDrawerLayout.closeDrawer();
        } else {
            mDrawerLayout.openDrawerView();
        }
    }
}
