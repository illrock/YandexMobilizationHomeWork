package ru.illarionovroman.yandexmobilizationhomework.network;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Query;
import ru.illarionovroman.yandexmobilizationhomework.network.response.DetectLanguageResponse;
import ru.illarionovroman.yandexmobilizationhomework.network.response.SupportedLanguagesResponse;
import ru.illarionovroman.yandexmobilizationhomework.network.response.TranslationResponse;


/**
 * API interface to interact with the server.
 */
public interface RestApi {

    /**
     * @param languageCode Generally, it's 2 or 3 letter language code (ISO 639-1 or ISO 639-2)
     * @return JSON response which contains list of supported languages
     */
    @GET("getLangs")
    Single<SupportedLanguagesResponse> getSupportedLanguages(@Query("ui") String languageCode);

    @GET("detect")
    Single<DetectLanguageResponse> detectLanguage(@Query("text") String languageCode,
                                                      @Query("hint") String hintList);

    /**
     * @param text The text which we want to be translated
     * @param langFromTo String of pattern "langCodeFrom-langCodeTo", e.g. "en-ru"
     * @param format Possible values:<br>
     * &nbsp&nbsp&nbsp&nbsp plain — Text without markup (default value);<br>
     * &nbsp&nbsp&nbsp&nbsp html — HTML text.
     * @return {@link Single<TranslationResponse>} with full translation data
     */
    @GET("translate")
    Single<TranslationResponse> getTranslation(@Query("text") String text,
                                               @Query("lang") String langFromTo,
                                               @Query("format") String format);
}