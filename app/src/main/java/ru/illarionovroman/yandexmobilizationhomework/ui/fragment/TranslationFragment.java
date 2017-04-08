package ru.illarionovroman.yandexmobilizationhomework.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jakewharton.rxbinding2.widget.RxTextView;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.HttpException;
import ru.illarionovroman.yandexmobilizationhomework.R;
import ru.illarionovroman.yandexmobilizationhomework.db.DBManager;
import ru.illarionovroman.yandexmobilizationhomework.model.HistoryItem;
import ru.illarionovroman.yandexmobilizationhomework.network.ApiManager;
import ru.illarionovroman.yandexmobilizationhomework.network.response.ErrorResponse;
import ru.illarionovroman.yandexmobilizationhomework.network.response.ResponseErrorCodes;
import ru.illarionovroman.yandexmobilizationhomework.ui.activity.FullscreenActivity;
import ru.illarionovroman.yandexmobilizationhomework.ui.activity.LanguageSelectionActivity;
import ru.illarionovroman.yandexmobilizationhomework.util.Utils;
import timber.log.Timber;


public class TranslationFragment extends BaseFragment {

    public static final int REQUEST_CODE_LANGUAGE_FROM = 1;
    public static final int REQUEST_CODE_LANGUAGE_TO = 2;

    public static final String EXTRA_CURRENT_LANGUAGE = "ru.illarionovroman.yandexmobilizationhomework.ui.fragments.TranslationFragment.EXTRA_CURRENT_LANGUAGE";
    public static final String EXTRA_REQUEST_CODE = "ru.illarionovroman.yandexmobilizationhomework.ui.fragments.TranslationFragment.EXTRA_REQUEST_CODE";

    private static final float ALPHA_BUTTONS_DISABLED = 0.3f;
    public static final int USER_INPUT_UPDATE_TIMEOUT_SECONDS = 2;

    @BindView(R.id.etWordInput)
    EditText mEtWordInput;
    @BindView(R.id.tvTranslation)
    TextView mTvTranslation;
    @BindView(R.id.llTranslationButtons)
    LinearLayout mLlTranslationButtons;
    @BindView(R.id.llTranslationError)
    LinearLayout mLlTranslationError;
    @BindView(R.id.tvTranslationErrorTitle)
    TextView mTvTranslationErrorTitle;
    @BindView(R.id.tvTranslationErrorText)
    TextView mTvTranslationErrorText;
    @BindView(R.id.pbLoading)
    ProgressBar mPbLoading;

    @BindView(R.id.ivTranslationSpeaker)
    ImageView mIvTranslationSpeaker;
    @BindView(R.id.ivTranslationFavorite)
    ImageView mIvTranslationFavorite;
    @BindView(R.id.ivTranslationShare)
    ImageView mIvTranslationShare;
    @BindView(R.id.ivTranslationFullscreen)
    ImageView mIvTranslationFullscreen;

    @BindView(R.id.tvLanguageFrom)
    TextView mTvLanguageFrom;
    @BindView(R.id.tvLanguageTo)
    TextView mTvLanguageTo;
    @BindView(R.id.ivSwapLanguages)
    ImageView mIvSwapLanguages;

    private CompositeDisposable mDisposables;
    private HistoryItem mCurrentItem;

    public TranslationFragment() {
    }

    public static TranslationFragment newInstance() {
        TranslationFragment fragment = new TranslationFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_translation, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeFragment();
    }

    private void initializeFragment() {
        initializeActionBar();

        mDisposables = new CompositeDisposable();
        mDisposables.add(createDisposableUserInputWatcher());
    }

    private void initializeActionBar() {
        setActionBarClickListeners();
    }

    private void setActionBarClickListeners() {
        mTvLanguageFrom.setOnClickListener(tvLangFromTextView -> {
            Intent intent = new Intent(getContext(), LanguageSelectionActivity.class);
            intent.putExtra(EXTRA_CURRENT_LANGUAGE, getCurrentCodeLanguageFrom());
            intent.putExtra(EXTRA_REQUEST_CODE, REQUEST_CODE_LANGUAGE_FROM);
            startActivityForResult(intent, REQUEST_CODE_LANGUAGE_FROM);
        });

        mTvLanguageTo.setOnClickListener(tvLangToTextView -> {
            Intent intent = new Intent(getContext(), LanguageSelectionActivity.class);
            intent.putExtra(EXTRA_CURRENT_LANGUAGE, getCurrentCodeLanguageTo());
            intent.putExtra(EXTRA_REQUEST_CODE, REQUEST_CODE_LANGUAGE_TO);
            startActivityForResult(intent, REQUEST_CODE_LANGUAGE_TO);
        });

        mIvSwapLanguages.setOnClickListener(swapLangsImageView -> {
            // Swap actionBar items
            String buf = mTvLanguageFrom.getText().toString();
            mTvLanguageFrom.setText(mTvLanguageTo.getText().toString());
            mTvLanguageTo.setText(buf);

            // Insert translation to input
            String translation = mTvTranslation.getText().toString();
            if (!TextUtils.isEmpty(translation)) {
                mEtWordInput.setText(translation);
            }
        });
    }

    /**
     * Rx watcher for user input in the EditText. After every valid update initiates translation
     * loading procedure
     * @return
     */
    @NonNull
    private Disposable createDisposableUserInputWatcher() {
        return RxTextView.textChanges(mEtWordInput)
                // RxBinding doc for textChanges() says that charSequence is mutable, get rid of it.
                .map(String::valueOf)
                .map(String::trim)
                .distinctUntilChanged()
                .debounce(USER_INPUT_UPDATE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .filter(inputText -> !TextUtils.isEmpty(inputText))
                .filter(inputText -> {
                    // Don't pass if the same item is currently displayed
                    if (mCurrentItem != null && inputText.equals(mCurrentItem.getWord())) {
                        return false;
                    } else {
                        return true;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateHistoryItem);
    }

    /**
     * We use reactivex.Single here,
     * since there could be either just one resulting item or failure
     */
    private void updateHistoryItem(String wordToTranslate) {
        showLoading();

        Single<HistoryItem> historyItemObservable =
                createHistoryItemUpdateSingle(wordToTranslate);

        mDisposables.add(historyItemObservable
                .subscribe(this::handleTranslationSuccess, this::handleTranslationError));
    }

    /**
     * The common scheme is as follows: at first, we are trying to get translation from DB,
     * if there is no such translation that we are looking for, then try to get it from the server
     * @param wordToTranslate
     * @return
     */
    private Single<HistoryItem> createHistoryItemUpdateSingle(final String wordToTranslate) {
        return Single.just(wordToTranslate)
                .flatMap(new Function<String, Single<HistoryItem>>() {
                    @Override
                    public Single<HistoryItem> apply(String word) throws Exception {
                        // Try to find this word in database
                        HistoryItem historyItem = DBManager.getHistoryItemByWord(getContext(),
                                word);
                        if (historyItem == null) {
                            historyItem = new HistoryItem();
                        }
                        return Single.just(historyItem);
                    }
                })
                .flatMap(new Function<HistoryItem, Single<HistoryItem>>() {
                    @Override
                    public Single<HistoryItem> apply(HistoryItem historyItem) throws Exception {
                        if (historyItem.getId() != HistoryItem.UNSPECIFIED_ID) {
                            // If found in DB - just return
                            return Single.just(historyItem);
                        } else {
                            // If not found - do network request
                            return createHistoryItemFromNetworkSingle(wordToTranslate);
                        }
                    }
                })
                // Do the work with DB and network in another thread
                .subscribeOn(Schedulers.io())
                // Show result in ui thread
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Perform network request, transform response to desired HistoryItem. This is reachable
     * only through DB write and read to obtain autogenerated item's ID and Date
     */
    private Single<HistoryItem> createHistoryItemFromNetworkSingle(String wordToTranslate) {
        String langFromTo = buildTranslationLangParam();

        Single<HistoryItem> historyItemSingle = ApiManager.getApiInterfaceInstance()
                .getTranslation(wordToTranslate, langFromTo, null)
                .map(translationResponse -> {
                    // Pull data from response
                    StringBuilder translationBuilder = new StringBuilder();
                    List<String> translations = translationResponse.getTranslations();
                    for (int i = 0; i < translations.size(); i++) {
                        translationBuilder.append(translations.get(i));
                        if (i != translations.size() - 1) {
                            translationBuilder.append("\n");
                        }
                    }
                    // Create incomplete item
                    HistoryItem item = new HistoryItem(
                            wordToTranslate,
                            translationBuilder.toString(),
                            getCurrentCodeLanguageFrom(),
                            getCurrentCodeLanguageTo()
                    );
                    // Write it to DB
                    long id = DBManager.addHistoryItem(getContext(), item);
                    // Now we can get the completed item
                    HistoryItem resultItem = DBManager.getHistoryItemById(getContext(), id);
                    return resultItem;
                });
        return historyItemSingle;
    }

    /**
     * Load item with given id from DB in background, then pass it to UI
     * @param itemId Id of item to load from DB
     */
    public void loadAndShowHistoryItem(long itemId) {
        Observable.just(itemId)
                .map(id -> {
                    HistoryItem item = DBManager.getHistoryItemById(getContext(), id);
                    return item;
                })
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleTranslationSuccess);
    }

    /**
     * Update current item, toolbar items and show successful screen state
     * @param item {@link HistoryItem} to show
     */
    private void handleTranslationSuccess(HistoryItem item) {
        if (item != null) {
            mCurrentItem = item;

            String languageFrom = Utils.getLangNameByCode(getContext(), item.getLanguageCodeFrom());
            String languageTo = Utils.getLangNameByCode(getContext(), item.getLanguageCodeTo());
            mTvLanguageFrom.setText(languageFrom);
            mTvLanguageTo.setText(languageTo);

            showHistoryItemTranslation(item);
        } else {
            Timber.wtf("Item can't be null in successful case");
        }
    }

    /**
     * Method for processing all kinds of errors that may appear during data loading.
     * It recognizes type of error and shows corresponding localized text to UI
     * @param error Throwable to analyze
     */
    private void handleTranslationError(Throwable error) {
        showTranslationViews();

        if (error instanceof HttpException) {
            try {
                // Get real error code
                Converter<ResponseBody, ErrorResponse> errorConverter = ApiManager
                        .getRetrofitInstance()
                        .responseBodyConverter(ErrorResponse.class, new Annotation[0]);
                ResponseBody errorBody = ((HttpException) error).response().errorBody();
                ErrorResponse errorResponse = errorConverter.convert(errorBody);
                // Use code to show localized error
                handleError(errorResponse.getCode(), errorResponse.getMessage());
            } catch (IOException ex) {
                showError(getString(R.string.error_title_unknown),
                        getString(R.string.error_text_unknown));
                Timber.e(error, "Couldn't convert error response! Message: " + error.getMessage());
            }
        } else if (error instanceof UnknownHostException) {
            showError(getString(R.string.error_title_connection),
                    getString(R.string.error_text_connection));
            Timber.d(error, "Unknown host error. Message: " + error.getMessage());
        } else {
            showError(getString(R.string.error_title_simple),
                    getString(R.string.error_text_unknown));
            Timber.e(error, "Not HTTP translation error! Message: " + error.getMessage());
        }

        error.printStackTrace();
    }

    /**
     * Method for building parameter for translation request using currently selected languages.
     * @return E.g.: "ru-en", "en-ru", etc.
     */
    private String buildTranslationLangParam() {
        String codeLangFrom = getCurrentCodeLanguageFrom();
        String codeLangTo = getCurrentCodeLanguageTo();
        return getString(R.string.translate_query_param_language_from_to,
                codeLangFrom, codeLangTo);
    }

    private void showHistoryItemTranslation(HistoryItem item) {
        mEtWordInput.setText(item.getWord());
        mTvTranslation.setText(item.getTranslation());
        mIvTranslationFavorite.setActivated(item.getIsFavorite());
        showTranslationViews();
    }

    @OnClick(R.id.btnRetry)
    void retry() {
        updateHistoryItem(mEtWordInput.getText().toString());
    }

    @OnClick(R.id.ivWordClean)
    void cleanWordInput() {
        mEtWordInput.setText("");
    }

    @OnClick(R.id.ivTranslationFavorite)
    void toggleTranslationFavorite() {
        if (mCurrentItem != null) {
            boolean activated = mIvTranslationFavorite.isActivated();
            if (activated) {
                mCurrentItem.setIsFavorite(false);
                DBManager.updateHistoryItem(getContext(), mCurrentItem);
                mIvTranslationFavorite.setActivated(false);
            } else {
                mCurrentItem.setIsFavorite(true);
                DBManager.updateHistoryItem(getContext(), mCurrentItem);
                mIvTranslationFavorite.setActivated(true);
            }
        }
    }

    @OnClick(R.id.ivTranslationShare)
    void shareTranslation() {
        String translation = mTvTranslation.getText().toString();
        if (TextUtils.isEmpty(translation)) {
            return;
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, translation);

        Intent chooser = Intent.createChooser(shareIntent,
                getString(R.string.translation_share_intent_title));

        if (shareIntent.resolveActivity(getContext().getPackageManager()) != null) {
            getContext().startActivity(chooser);
        }
    }

    @OnClick(R.id.ivTranslationFullscreen)
    void showFullScreen() {
        startActivity(new Intent(getContext(), FullscreenActivity.class));
    }

    /**
     * Method for showing a localized error message by its code
     */
    private void handleError(@ResponseErrorCodes int errorCode, @Nullable String errorMessage) {
        showTranslationViews();

        switch (errorCode) {
            case ResponseErrorCodes.API_KEY_BLOCKED:
                showError(getString(R.string.error_title_authorization),
                        getString(R.string.error_text_api_key_blocked));
                break;
            case ResponseErrorCodes.API_KEY_INVALID:
                showError(getString(R.string.error_title_authorization),
                        getString(R.string.error_text_api_key_invalid));
                break;
            case ResponseErrorCodes.DAY_LIMIT_EXCEED:
                showError(getString(R.string.error_title_translation),
                        getString(R.string.error_text_day_limit_exceed));
                break;
            case ResponseErrorCodes.TEXT_SIZE_EXCEED:
                showError(getString(R.string.error_title_translation),
                        getString(R.string.error_text_text_size_exceed));
                break;
            case ResponseErrorCodes.TEXT_UNTRANSLATABLE:
                showError(getString(R.string.error_title_translation),
                        getString(R.string.error_text_untranslatable));
                break;
            case ResponseErrorCodes.TRANSLATION_DIRECTION_UNSUPPORTED:
                showError(getString(R.string.error_title_translation),
                        getString(R.string.error_text_direction_unsupported));
                break;
            default:
                showError(getString(R.string.error_title_unknown),
                        getString(R.string.error_text_unknown));
                break;
        }
        Timber.d("handleError: errorCode=" + errorCode + ", msg=" + errorMessage);
    }

    /**
     * Method for processing language selection
     * @param data Intent, which contains code of selected language
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String resultLangCode = data.getStringExtra(LanguageSelectionActivity.EXTRA_RESULT);
            String selectedLangName = Utils.getLangNameByCode(getContext(), resultLangCode);
            if (requestCode == REQUEST_CODE_LANGUAGE_FROM) {
                mTvLanguageFrom.setText(selectedLangName);
            } else if (requestCode == REQUEST_CODE_LANGUAGE_TO) {
                mTvLanguageTo.setText(selectedLangName);
            }
            updateHistoryItem(mEtWordInput.getText().toString());
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private String getCurrentCodeLanguageFrom() {
        if (mTvLanguageFrom != null) {
            String nameLangFrom = mTvLanguageFrom.getText().toString();
            return Utils.getLangCodeByName(getContext(), nameLangFrom);
        } else {
            return "";
        }
    }

    private String getCurrentCodeLanguageTo() {
        if (mTvLanguageTo != null) {
            String nameLangTo = mTvLanguageTo.getText().toString();
            return Utils.getLangCodeByName(getContext(), nameLangTo);
        } else {
            return "";
        }
    }

    private void showLoading() {
        mPbLoading.setVisibility(View.VISIBLE);
        mTvTranslation.setVisibility(View.GONE);
        setTranslationButtonsEnabledState(false);
        mLlTranslationError.setVisibility(View.GONE);
    }

    private void showTranslationViews() {
        mPbLoading.setVisibility(View.GONE);
        mTvTranslation.setVisibility(View.VISIBLE);
        mLlTranslationButtons.setVisibility(View.VISIBLE);
        setTranslationButtonsEnabledState(true);
        mLlTranslationError.setVisibility(View.GONE);
    }

    private void setTranslationButtonsEnabledState(boolean enabled) {
        float alpha = enabled ? 1f : ALPHA_BUTTONS_DISABLED;

        mIvTranslationSpeaker.setEnabled(enabled);
        mIvTranslationSpeaker.setAlpha(alpha);
        mIvTranslationFavorite.setEnabled(enabled);
        mIvTranslationFavorite.setAlpha(alpha);
        mIvTranslationShare.setEnabled(enabled);
        mIvTranslationShare.setAlpha(alpha);
        mIvTranslationFullscreen.setEnabled(enabled);
        mIvTranslationFullscreen.setAlpha(alpha);
    }

    private void showError(String errorTitle, String errorText) {
        mPbLoading.setVisibility(View.GONE);
        mTvTranslation.setVisibility(View.GONE);
        mLlTranslationButtons.setVisibility(View.GONE);

        mTvTranslationErrorTitle.setText(errorTitle);
        mTvTranslationErrorText.setText(errorText);
        mLlTranslationError.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDisposables != null) {
            mDisposables.clear();
        }
    }
}
