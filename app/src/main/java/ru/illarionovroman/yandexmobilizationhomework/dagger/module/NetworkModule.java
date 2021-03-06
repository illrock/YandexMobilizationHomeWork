package ru.illarionovroman.yandexmobilizationhomework.dagger.module;


import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import ru.illarionovroman.yandexmobilizationhomework.BuildConfig;
import ru.illarionovroman.yandexmobilizationhomework.network.ApiKeyInsertInterceptor;
import ru.illarionovroman.yandexmobilizationhomework.network.RestApi;


@Module
public class NetworkModule {

    private static final String TRANSLATOR_END_POINT = "https://translate.yandex.net/api/v1.5/tr.json/";

    @Provides
    @Singleton
    Gson provideGson() {
        return new GsonBuilder().create();
    }

    @Provides
    @Singleton
    OkHttpClient provideOkHttpClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder okHttpBuilder = new OkHttpClient().newBuilder();
        if (BuildConfig.DEBUG) {
            okHttpBuilder.addNetworkInterceptor(new StethoInterceptor())
                    .addInterceptor(loggingInterceptor);
        }

        return okHttpBuilder
                .addInterceptor(new ApiKeyInsertInterceptor())
                .build();
    }

    @Provides
    @Singleton
    Retrofit provideRetrofit(OkHttpClient okHttpClient, Gson gson) {
        return new Retrofit.Builder()
                .baseUrl(TRANSLATOR_END_POINT)
                .client(okHttpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    @Provides
    @Singleton
    RestApi provideRestApi(Retrofit retrofit) {
        return retrofit.create(RestApi.class);
    }
}
