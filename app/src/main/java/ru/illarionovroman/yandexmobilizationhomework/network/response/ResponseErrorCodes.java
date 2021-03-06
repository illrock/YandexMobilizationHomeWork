package ru.illarionovroman.yandexmobilizationhomework.network.response;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


@IntDef({ResponseErrorCodes.API_KEY_INVALID, ResponseErrorCodes.API_KEY_BLOCKED,
        ResponseErrorCodes.DAY_LIMIT_EXCEED, ResponseErrorCodes.TEXT_SIZE_EXCEED,
        ResponseErrorCodes.TEXT_UNTRANSLATABLE, ResponseErrorCodes.TRANSLATION_DIRECTION_UNSUPPORTED})
@Retention(RetentionPolicy.SOURCE)
public @interface ResponseErrorCodes {
    int API_KEY_INVALID = 401;
    int API_KEY_BLOCKED = 402;
    int DAY_LIMIT_EXCEED = 404;
    int TEXT_SIZE_EXCEED = 413;
    int TEXT_UNTRANSLATABLE = 422;
    int TRANSLATION_DIRECTION_UNSUPPORTED = 501;
}
