package com.skuri.skuri_backend.domain.app.exception;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;

public class AppNoticeCommentNotFoundException extends BusinessException {

    public AppNoticeCommentNotFoundException() {
        super(ErrorCode.APP_NOTICE_COMMENT_NOT_FOUND);
    }
}
