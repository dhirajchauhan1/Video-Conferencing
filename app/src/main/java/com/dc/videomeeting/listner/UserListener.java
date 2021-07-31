package com.dc.videomeeting.listner;

import com.dc.videomeeting.model.User;

public interface UserListener {
    void initiateVideoMeeting(User user);
    void initiateAudioMeeting(User user);

    void onMultipleUserAction(Boolean isMultipleUserSelected);
}
