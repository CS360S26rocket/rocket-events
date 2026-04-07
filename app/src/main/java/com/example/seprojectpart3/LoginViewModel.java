package com.example.seprojectpart3;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class LoginViewModel extends AndroidViewModel {
    private final AuthRepository authRepo;
    private final MutableLiveData<String> tokenLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public LoginViewModel(Application app) {
        super(app);
        authRepo = new AuthRepository();
    }

    public void login(String email, String password) {
        authRepo.loginUser(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(String token, String uid) {
                tokenLiveData.postValue(token);
            }
            @Override
            public void onFailure(String error) {
                errorLiveData.postValue(error);
            }
        });
    }

    public LiveData<String> getToken() { return tokenLiveData; }
    public LiveData<String> getError() { return errorLiveData; }
}