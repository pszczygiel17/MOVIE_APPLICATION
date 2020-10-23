package com.example.movieapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import static com.example.movieapplication.RegisterActivity.users;

import java.util.List;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class HomeActivity extends AppCompatActivity {

    ListView item_list;
    CustomAdapter adapter;
    public HomeActivity CustomListView = null;
    public ArrayList<ListModel> CustomListViewValuesArr = new ArrayList<ListModel>();

    FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    TextView loading;
    ProgressBar progressBar;
    RelativeLayout relativeLayout;
    CardView cardView;

    List<Profile> profileList = new ArrayList<>();

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        toolbar = findViewById(R.id.app_bar);
        toolbar.setTitle(R.string.home_title);
        setSupportActionBar(toolbar);

        firebaseAuth = FirebaseAuth.getInstance();

        show_navigation_btn(false);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);

        cardView = findViewById(R.id.card_view);
        progressBar = findViewById(R.id.home_progress_bar);
        relativeLayout = findViewById(R.id.home_layout);
        loading = findViewById(R.id.home_loading);
        item_list = findViewById(R.id.home_list);

        Menu nav_Menu = navigationView.getMenu();
        nav_Menu.findItem(R.id.nav_login).setVisible(false);
        nav_Menu.findItem(R.id.nav_registration).setVisible(false);

        load_data();

        CustomListView = this;

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Intent settings = new Intent(HomeActivity.this, OptionActivity.class);
                Intent log_out = new Intent(HomeActivity.this, MainActivity.class);
                Intent profile = new Intent(HomeActivity.this, ProfileActivity.class);

                switch (item.getItemId())
                {
                    case R.id.nav_home:
                        item.setChecked(true);
                        drawerLayout.closeDrawers();
                        return true;
                    case R.id.nav_profile:
                        item.setChecked(true);
                        drawerLayout.closeDrawers();
                        startActivity(profile);
                        return true;
                    case R.id.nav_settings:
                        item.setChecked(true);
                        drawerLayout.closeDrawers();
                        startActivity(settings);
                        return true;
                    case R.id.nav_logout:
                        item.setChecked(true);
                        drawerLayout.closeDrawers();
                        FirebaseAuth.getInstance().signOut();
                        log_out.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        log_out.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        log_out.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(log_out);
                        return true;
                }

                return false;
            }
        });

    }

    public void onItemClick(int mPosition) {
        Profile selected_profile = profileList.get(mPosition);
        Intent sendProfile = new Intent(HomeActivity.this, ProfileUserActivity.class);
        sendProfile.putExtra("selectedProfile", selected_profile);
        startActivity(sendProfile);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            drawerLayout.openDrawer(GravityCompat.START);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void load_data (){
        progressBar.setVisibility(View.VISIBLE);
        relativeLayout.setVisibility(View.INVISIBLE);
        loading.setVisibility(View.VISIBLE);
        String userID = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();
        ProfileDetails profileDetails = new ProfileDetails(userID);
        profile_details(profileDetails);
        set_values();
        load_matched_users();
    }

    public void show_navigation_btn(boolean state){
        final ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(state);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
    }

    public void set_values (){

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(LoadUserApi.MY_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        LoadUserApi loadUserApi = retrofit.create(LoadUserApi.class);

        Call<User> call = loadUserApi.loadUserDetails();

        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                Log.d("good", "good");
                Toast.makeText(getApplicationContext(), "załadowano header", Toast.LENGTH_LONG).show();

                User user = response.body();
                View nav_header = navigationView.getHeaderView(0);
                TextView header = nav_header.findViewById(R.id.nav_header);
                assert user != null;
                header.setText(user.get_login());

            }
            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Log.d("fail", "fail");
                Toast.makeText(getApplicationContext(), "nie załadowano headera", Toast.LENGTH_LONG).show();
            }
        });

    }

    public void load_matched_users (){

        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(LoadMatchedUsersApi.MY_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        LoadMatchedUsersApi loadMatchedUsersApi = retrofit.create(LoadMatchedUsersApi.class);

        Call<List<Profile>> call = loadMatchedUsersApi.loadMatchedUsers();

        call.enqueue(new Callback<List<Profile>>() {
            @Override
            public void onResponse(Call<List<Profile>> call, Response<List<Profile>> response) {
                Log.d("good", "good");
                Toast.makeText(getApplicationContext(), "załadowano liste", Toast.LENGTH_LONG).show();

                List<Profile> list = response.body();
                profileList = list;
                assert list != null;
                for (int i = 0; i < list.size(); i++) {

                    final ListModel sched = new ListModel();

                    sched.set_name(list.get(i).get_name());
                    sched.set_age(list.get(i).get_age());

                    CustomListViewValuesArr.add(sched);
                }

                Resources res = getResources();

                adapter = new CustomAdapter(CustomListView, CustomListViewValuesArr, res);
                item_list.setAdapter(adapter);

                show_navigation_btn(true);
                progressBar.setVisibility(View.INVISIBLE);
                relativeLayout.setVisibility(View.VISIBLE);
                loading.setVisibility(View.INVISIBLE);

            }
            @Override
            public void onFailure(Call<List<Profile>> call, Throwable t) {
                Log.d("fail", "fail");
                Toast.makeText(getApplicationContext(), "nie załadowano listy", Toast.LENGTH_LONG).show();

                show_navigation_btn(true);
                progressBar.setVisibility(View.INVISIBLE);
                relativeLayout.setVisibility(View.VISIBLE);
                loading.setVisibility(View.INVISIBLE);


            }
        });

    }

    public void profile_details(ProfileDetails profileDetails) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ProfileDetailsApi.MY_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ProfileDetailsApi profileDetailsApi = retrofit.create(ProfileDetailsApi.class);

        Call<ProfileDetails> call = profileDetailsApi.sendDetails(profileDetails);

        call.enqueue(new Callback<ProfileDetails>() {
            @Override
            public void onResponse(Call<ProfileDetails> call, Response<ProfileDetails> response) {
                Log.d("good", "good");
                Toast.makeText(getApplicationContext(), "przesłano uid", Toast.LENGTH_LONG).show();
            }
            @Override
            public void onFailure(Call<ProfileDetails> call, Throwable t) {
                Log.d("fail", "fail");
                Toast.makeText(getApplicationContext(), "nie udało się przesłać uid", Toast.LENGTH_LONG).show();
            }
        });
    }


}
