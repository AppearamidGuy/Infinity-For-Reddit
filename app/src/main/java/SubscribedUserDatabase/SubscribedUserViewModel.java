package SubscribedUserDatabase;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class SubscribedUserViewModel extends AndroidViewModel {
    private SubscribedUserRepository mSubscribedUserRepository;
    private LiveData<List<SubscribedUserData>> mAllSubscribedUsers;

    public SubscribedUserViewModel(Application application) {
        super(application);
        mSubscribedUserRepository = new SubscribedUserRepository(application);
        mAllSubscribedUsers = mSubscribedUserRepository.getAllSubscribedSubreddits();
    }

    public LiveData<List<SubscribedUserData>> getAllSubscribedUsers() {
        return mAllSubscribedUsers;
    }

    public void insert(SubscribedUserData subscribedUserData) {
        mSubscribedUserRepository.insert(subscribedUserData);
    }
}
