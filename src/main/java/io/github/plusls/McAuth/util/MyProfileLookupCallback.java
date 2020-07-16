package io.github.plusls.McAuth.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.ProfileLookupCallback;

public class MyProfileLookupCallback implements ProfileLookupCallback {
    public GameProfile gameProfile = null;
    public Exception exception = null;

    @Override
    public void onProfileLookupSucceeded(GameProfile profile) {
        this.gameProfile = profile;
    }

    @Override
    public void onProfileLookupFailed(GameProfile profile, Exception exception) {
        this.gameProfile = null;
        this.exception = exception;
    }
}
