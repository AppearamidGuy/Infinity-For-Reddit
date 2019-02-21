package ml.docilealligator.infinityforreddit;

import androidx.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import com.google.android.material.chip.Chip;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.RequestOptions;

import javax.inject.Inject;
import javax.inject.Named;

import SubscribedUserDatabase.SubscribedUserDao;
import SubscribedUserDatabase.SubscribedUserData;
import SubscribedUserDatabase.SubscribedUserRoomDatabase;
import User.UserDao;
import User.UserData;
import User.UserRoomDatabase;
import User.UserViewModel;
import butterknife.BindView;
import butterknife.ButterKnife;
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;
import pl.droidsonroids.gif.GifImageView;
import retrofit2.Retrofit;

public class ViewUserDetailActivity extends AppCompatActivity {

    static final String EXTRA_USER_NAME_KEY = "EUNK";

    private static final String FRAGMENT_OUT_STATE_KEY = "FOSK";

    @BindView(R.id.coordinator_layout_view_user_detail_activity) CoordinatorLayout coordinatorLayout;
    @BindView(R.id.banner_image_view_view_user_detail_activity) ImageView bannerImageView;
    @BindView(R.id.icon_gif_image_view_view_user_detail_activity) GifImageView iconGifImageView;
    @BindView(R.id.user_name_text_view_view_user_detail_activity) TextView userNameTextView;
    @BindView(R.id.subscribe_user_chip_view_user_detail_activity) Chip subscribeUserChip;
    @BindView(R.id.karma_text_view_view_user_detail_activity) TextView karmaTextView;

    private Fragment mFragment;
    private SubscribedUserDao subscribedUserDao;
    private RequestManager glide;
    private UserViewModel userViewModel;

    private boolean subscriptionReady = false;

    @Inject
    @Named("no_oauth")
    Retrofit mRetrofit;

    @Inject
    @Named("oauth")
    Retrofit mOauthRetrofit;

    @Inject
    @Named("auth_info")
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_user_detail);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ButterKnife.bind(this);

        ((Infinity) getApplication()).getmNetworkComponent().inject(this);

        setSupportActionBar(toolbar);

        //Get status bar height
        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
        params.topMargin = statusBarHeight;

        String userName = getIntent().getExtras().getString(EXTRA_USER_NAME_KEY);
        String title = "u/" + userName;
        userNameTextView.setText(title);

        CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar_layout_view_user_detail_activity);
        AppBarLayout appBarLayout = findViewById(R.id.app_bar_layout_view_user_detail_activity);

        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            int previousVerticalOffset = 0;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if(scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                } else {
                    if(verticalOffset < previousVerticalOffset) {
                        //Scroll down
                        if(scrollRange - Math.abs(verticalOffset) <= toolbar.getHeight()) {
                            collapsingToolbarLayout.setTitle(title);
                        }
                    } else {
                        //Scroll up
                        if(scrollRange - Math.abs(verticalOffset) > toolbar.getHeight()) {
                            collapsingToolbarLayout.setTitle(" ");//carefull there should a space between double quote otherwise it wont work
                        }
                    }
                    previousVerticalOffset = verticalOffset;
                }
            }
        });

        subscribedUserDao = SubscribedUserRoomDatabase.getDatabase(this).subscribedUserDao();
        glide = Glide.with(this);

        userViewModel = ViewModelProviders.of(this, new UserViewModel.Factory(getApplication(), userName))
                .get(UserViewModel.class);
        userViewModel.getUserLiveData().observe(this, userData -> {
            if(userData != null) {
                if(userData.getBanner().equals("")) {
                    bannerImageView.setOnClickListener(view -> {
                        //Do nothing since the user has no banner image
                    });
                } else {
                    glide.load(userData.getBanner()).into(bannerImageView);
                    bannerImageView.setOnClickListener(view -> {
                        Intent intent = new Intent(this, ViewImageActivity.class);
                        intent.putExtra(ViewImageActivity.TITLE_KEY, title);
                        intent.putExtra(ViewImageActivity.IMAGE_URL_KEY, userData.getBanner());
                        intent.putExtra(ViewImageActivity.FILE_NAME_KEY, userName + "-banner");
                        startActivity(intent);
                    });
                }

                if(userData.getIconUrl().equals("")) {
                    glide.load(getDrawable(R.drawable.subreddit_default_icon))
                            .apply(RequestOptions.bitmapTransform(new RoundedCornersTransformation(216, 0)))
                            .into(iconGifImageView);
                    iconGifImageView.setOnClickListener(view -> {
                        //Do nothing since the user has no icon image
                    });
                } else {
                    glide.load(userData.getIconUrl())
                            .apply(RequestOptions.bitmapTransform(new RoundedCornersTransformation(216, 0)))
                            .error(glide.load(R.drawable.subreddit_default_icon))
                            .into(iconGifImageView);

                    iconGifImageView.setOnClickListener(view -> {
                        Intent intent = new Intent(this, ViewImageActivity.class);
                        intent.putExtra(ViewImageActivity.TITLE_KEY, title);
                        intent.putExtra(ViewImageActivity.IMAGE_URL_KEY, userData.getIconUrl());
                        intent.putExtra(ViewImageActivity.FILE_NAME_KEY, userName + "-icon");
                        startActivity(intent);
                    });
                }

                if(userData.isCanBeFollowed()) {
                    subscribeUserChip.setVisibility(View.VISIBLE);
                    subscribeUserChip.setOnClickListener(view -> {
                        if(subscriptionReady) {
                            subscriptionReady = false;
                            if(subscribeUserChip.getText().equals(getResources().getString(R.string.follow))) {
                                UserFollowing.followUser(mOauthRetrofit, mRetrofit, sharedPreferences,
                                        userName, subscribedUserDao, new UserFollowing.UserFollowingListener() {
                                            @Override
                                            public void onUserFollowingSuccess() {
                                                subscribeUserChip.setText(R.string.unfollow);
                                                subscribeUserChip.setChipBackgroundColor(getResources().getColorStateList(R.color.colorAccent));
                                                makeSnackbar(R.string.followed);
                                                subscriptionReady = true;
                                            }

                                            @Override
                                            public void onUserFollowingFail() {
                                                makeSnackbar(R.string.follow_failed);
                                                subscriptionReady = true;
                                            }
                                        });
                            } else {
                                UserFollowing.unfollowUser(mOauthRetrofit, mRetrofit, sharedPreferences,
                                        userName, subscribedUserDao, new UserFollowing.UserFollowingListener() {
                                            @Override
                                            public void onUserFollowingSuccess() {
                                                subscribeUserChip.setText(R.string.follow);
                                                subscribeUserChip.setChipBackgroundColor(getResources().getColorStateList(R.color.colorPrimaryDark));
                                                makeSnackbar(R.string.unfollowed);
                                                subscriptionReady = true;
                                            }

                                            @Override
                                            public void onUserFollowingFail() {
                                                makeSnackbar(R.string.unfollow_failed);
                                                subscriptionReady = true;
                                            }
                                        });
                            }
                        }
                    });

                    new CheckIsFollowingUserAsyncTask(subscribedUserDao, userName, new CheckIsFollowingUserAsyncTask.CheckIsFollowingUserListener() {
                        @Override
                        public void isSubscribed() {
                            subscribeUserChip.setText(R.string.unfollow);
                            subscribeUserChip.setChipBackgroundColor(getResources().getColorStateList(R.color.colorAccent));
                            subscriptionReady = true;
                        }

                        @Override
                        public void isNotSubscribed() {
                            subscribeUserChip.setText(R.string.follow);
                            subscribeUserChip.setChipBackgroundColor(getResources().getColorStateList(R.color.colorPrimaryDark));
                            subscriptionReady = true;
                        }
                    }).execute();
                } else {
                    subscribeUserChip.setVisibility(View.GONE);
                }

                String userFullName = "u/" + userData.getName();
                userNameTextView.setText(userFullName);
                String karma = getString(R.string.karma_info, userData.getKarma());
                karmaTextView.setText(karma);
            }
        });

        FetchUserData.fetchUserData(mRetrofit, userName, new FetchUserData.FetchUserDataListener() {
            @Override
            public void onFetchUserDataSuccess(UserData userData) {
                new InsertUserDataAsyncTask(UserRoomDatabase.getDatabase(ViewUserDetailActivity.this).userDao(), userData).execute();
            }

            @Override
            public void onFetchUserDataFail() {
                makeSnackbar(R.string.cannot_fetch_user_info);
            }
        });

        if(savedInstanceState == null) {
            mFragment = new PostFragment();
            Bundle bundle = new Bundle();
            bundle.putString(PostFragment.NAME_KEY, userName);
            bundle.putInt(PostFragment.POST_TYPE_KEY, PostDataSource.TYPE_USER);
            mFragment.setArguments(bundle);
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout_view_user_detail_activity, mFragment).commit();
        } else {
            mFragment = getSupportFragmentManager().getFragment(savedInstanceState, FRAGMENT_OUT_STATE_KEY);
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout_view_user_detail_activity, mFragment).commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return false;
    }

    private void makeSnackbar(int resId) {
        Snackbar.make(coordinatorLayout, resId, Snackbar.LENGTH_SHORT).show();
    }

    private static class InsertUserDataAsyncTask extends AsyncTask<Void, Void, Void> {

        private UserDao userDao;
        private UserData subredditData;

        InsertUserDataAsyncTask(UserDao userDao, UserData userData) {
            this.userDao = userDao;
            this.subredditData = userData;
        }

        @Override
        protected Void doInBackground(final Void... params) {
            userDao.insert(subredditData);
            return null;
        }
    }

    private static class CheckIsFollowingUserAsyncTask extends AsyncTask<Void, Void, Void> {

        private SubscribedUserDao subscribedUserDao;
        private String userName;
        private SubscribedUserData subscribedUserData;
        private CheckIsFollowingUserListener checkIsFollowingUserListener;

        interface CheckIsFollowingUserListener {
            void isSubscribed();
            void isNotSubscribed();
        }

        CheckIsFollowingUserAsyncTask(SubscribedUserDao subscribedUserDao, String userName,
                                      CheckIsFollowingUserListener checkIsFollowingUserListener) {
            this.subscribedUserDao = subscribedUserDao;
            this.userName = userName;
            this.checkIsFollowingUserListener = checkIsFollowingUserListener;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            subscribedUserData = subscribedUserDao.getSubscribedUser(userName);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if(subscribedUserData != null) {
                checkIsFollowingUserListener.isSubscribed();
            } else {
                checkIsFollowingUserListener.isNotSubscribed();
            }
        }
    }
}
