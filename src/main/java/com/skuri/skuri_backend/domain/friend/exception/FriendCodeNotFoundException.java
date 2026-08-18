package com.skuri.skuri_backend.domain.friend.exception;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;

public class FriendCodeNotFoundException extends BusinessException {

    public FriendCodeNotFoundException() {
        super(ErrorCode.FRIEND_CODE_NOT_FOUND);
    }
}
