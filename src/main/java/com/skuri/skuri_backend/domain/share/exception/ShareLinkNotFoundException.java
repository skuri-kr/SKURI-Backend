package com.skuri.skuri_backend.domain.share.exception;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;

public class ShareLinkNotFoundException extends BusinessException {

    public ShareLinkNotFoundException() {
        super(ErrorCode.SHARE_LINK_NOT_FOUND);
    }
}
