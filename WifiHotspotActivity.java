package com.ford.oa.myvehicles.wifihotspot.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.ford.networkutils.StatusCodes;
import com.ford.ngsdnutils.models.NgsdnException;
import com.ford.oa.R;
import com.ford.oa.activity.BaseActivity;
import com.ford.oa.commons.ui.widgets.LoadingSpinnerWidget;
import com.ford.oa.commons.utils.ColorUtil;
import com.ford.oa.commons.utils.DeviceUtil;
import com.ford.oa.commons.utils.DialerUtil;
import com.ford.oa.myvehicles.providers.WifiHotspotUtilsProvider;
import com.ford.oa.myvehicles.ui.widgets.MaintenanceScheduleWidget;
import com.ford.oa.myvehicles.wifihotspot.providers.WifiDialogProvider;
import com.ford.oa.ui.animators.FordObjectAnimator;
import com.ford.oa.ui.dialog.FordDialogFactory;
import com.ford.oa.utils.ConversionUtil;
import com.ford.oa.utils.ErrorDialogUtil;
import com.ford.oa.myvehicles.wifihotspot.utils.WifiHotspotUtil;
import com.ford.utils.BrowserUtil;
import com.ford.utils.TextUtils;
import com.ford.utils.providers.DateFormatProvider;
import com.ford.utils.providers.LocaleProvider;
import com.ford.vehiclecommon.models.Vehicle;
import com.ford.wifihotspot.models.NgsdnWifiDataPlan;
import com.ford.wifihotspot.models.NgsdnWifiDataPlanUsage;

import java.text.DateFormat;
import java.util.Date;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.Optional;

public class WifiHotspotActivity extends BaseActivity implements WifiHotspotContract.View {

    @BindView(R.id.activity_wifi_hotspot_loading_spinner)
    LoadingSpinnerWidget mLoadingSpinner;

    @BindView(R.id.activity_wifi_hotspot_data_plan_view)
    TextView mDataPlanTextView;

    @BindView(R.id.activity_wifi_hotspot_data_plan_renewal_or_expire_details_view)
    TextView mDataPlanRenewalOrExpireDetailTextView;

    @BindView(R.id.wifi_usage_bar_usage_text)
    TextView mDataUsedTextView;

    @BindView(R.id.wifi_usage_bar_start_text)
    TextView mUsageBarStartText;

    @BindView(R.id.wifi_usage_bar_widget_gauge)
    MaintenanceScheduleWidget mWifiUsageBarWidget;

    @BindView(R.id.wifi_usage_bar_usage_end_text)
    TextView mUsageBarEndText;

    @BindView(R.id.activity_wifi_hotspot_status_refresh_view)
    TextView mStatusRefreshTimeTextView;

    @BindView(R.id.activity_wifi_hotspot_status_refresh_spinner)
    ImageView mStatusRefreshSpinnerImageView;

    @BindView(R.id.activity_wifi_hotspot_manage_account)
    View mManageAccountButton;

    @BindView(R.id.activity_wifi_hotspot_data_info_banner)
    TextView mHotspotDataInfoBanner;

    @BindView(R.id.activity_wifi_hotspot_limited_plan_usage_container)
    ViewGroup mLimitedPlanUsageContainer;

    @BindView(R.id.activity_wifi_hotspot_unlimited_plan_usage_container)
    ViewGroup mUnlimitedPlanUsageContainer;

    @BindView(R.id.wifi_unlimited_plan_widget_data_usage_text)
    TextView mUnlimitedPlanWidgetUsageText;

    @Inject
    WifiHotspotContract.Presenter mPresenter;

    @Inject
    ErrorDialogUtil mErrorDialogUtil;

    @Inject
    FordObjectAnimator mFordObjectAnimator;

    @Inject
    DateFormatProvider mDateFormatProvider;

    @Inject
    DialerUtil mDialerUtil;

    @Inject
    BrowserUtil mBrowserUtil;

    @Inject
    ConversionUtil mConversionUtil;

    @Inject
    WifiHotspotUtil mWifiHotspotUtil;

    @Inject
    WifiDialogProvider mWifiDialogProvider;

    @Inject
    LocaleProvider mLocaleProvider;

    @Inject
    WifiHotspotUtilsProvider mWifiHotspotUtilsProvider;

    @Inject
    ColorUtil mColorUtil;

    private static final int REFRESH_DURATION = 500;
    private static final String WIFI_DATA_PLAN_INVALID_DATA = "--";
    private boolean mManualStatusRefreshing = false;
    private Vehicle mVehicle;
    private boolean mIsActivityInitialized = false;

    public static void newInstance(final Context context, Vehicle vehicle, NgsdnWifiDataPlan ngsdnWifiDataPlan, NgsdnWifiDataPlanUsage ngsdnWifiDataPlanUsage, int status) {
        Intent intent = new Intent(context, WifiHotspotActivity.class);
        intent.putExtra(Extras.VEHICLE, vehicle);
        intent.putExtra(Extras.WIFI_DATA_PLAN, ngsdnWifiDataPlan);
        intent.putExtra(Extras.WIFI_DATA_PLAN_USAGE, ngsdnWifiDataPlanUsage);
        intent.putExtra(Extras.WIFI_DATA_USAGE_RESPONSE_STATUS, status);
        context.startActivity(intent);
    }

    public static void newInstance(final Context context, Vehicle vehicle) {
        Intent intent = new Intent(context, WifiHotspotActivity.class);
        intent.putExtra(Extras.VEHICLE, vehicle);
        context.startActivity(intent);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_wifi_hotspot;
    }

    @Override
    protected boolean needsDaggerInjection() {
        return true;
    }

    //region LifeCycle Methods

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setToolbarTitle(getString(R.string.vehicle_info_hotspot));
        setToolbarTitleColor(Color.WHITE);
        setToolbarColor(getResources().getColor(R.color.ford_medium_blue));
        setToolbarDrawableResource(R.drawable.back_white_btn);
        mIsActivityInitialized = true;

        setupStatusRefresh();

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(Extras.VEHICLE)) {
                mVehicle = intent.getParcelableExtra(Extras.VEHICLE);
                mPresenter.setVehicleVin(mVehicle.getVin());
            }
            if (intent.hasExtra(Extras.WIFI_DATA_PLAN)
                    && intent.hasExtra(Extras.WIFI_DATA_PLAN_USAGE)
                    && intent.hasExtra(Extras.WIFI_DATA_USAGE_RESPONSE_STATUS)) {
                NgsdnWifiDataPlan dataPlan = intent.getParcelableExtra(Extras.WIFI_DATA_PLAN);
                NgsdnWifiDataPlanUsage dataPlanUsage = intent.getParcelableExtra(Extras.WIFI_DATA_PLAN_USAGE);
                int status = intent.getIntExtra(Extras.WIFI_DATA_USAGE_RESPONSE_STATUS, 0);
                mPresenter.setWifiData(dataPlan, dataPlanUsage, status);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPresenter.attachView(this);
        if (mIsActivityInitialized) {
            mPresenter.updateViewWithWifiDataUsage();
            mIsActivityInitialized = false;
        } else {
            checkActiveNetworkConnection();
        }
    }

    @Override
    protected void onPause() {
        mPresenter.detachView();
        super.onPause();
    }

    //endregion

    //region WifiHotspotContract Methods

    @Override
    public void showLoadingSpinner() {
        if (!mManualStatusRefreshing) {
            mLoadingSpinner.show();
        }
    }

    @Override
    public void hideLoadingSpinner() {
        mManualStatusRefreshing = false;
        mLoadingSpinner.hide();
    }

    @Override
    public void showErrorDialog(Throwable e) {
        mErrorDialogUtil.showErrorDialog(e, this, new FordDialogFactory.FordDialogListener() {
            public void onButtonClickedAtIndex(int index) {
                if (index == ErrorDialogUtil.RETRY_BUTTON_INDEX) {
                    checkActiveNetworkConnection();
                }
            }
        });
    }

    @Override
    public void updateDataPlanUsage(NgsdnWifiDataPlan dataPlan, NgsdnWifiDataPlanUsage dataPlanUsage, Date refreshTime) {
        Resources resources = getResources();
        float dataPlanLimit = dataPlanUsage.getDataPlanLimit();
        float dataUsed = dataPlanUsage.getDataPlanUsed();
        String dataPlanUom = dataPlanUsage.getTotalDataUnitOfMeasurement();
        String usageUom = dataPlanUsage.getDataUsageUnitOfMeasurement();

        if (TextUtils.isBlank(dataPlanUom) || TextUtils.isBlank(usageUom)) {
            mDataUsedTextView.setText(resources.getString(R.string.wifi_hotspot_data_plan_usage_of_total, WIFI_DATA_PLAN_INVALID_DATA, WIFI_DATA_PLAN_INVALID_DATA, WIFI_DATA_PLAN_INVALID_DATA, WIFI_DATA_PLAN_INVALID_DATA));
            mUsageBarStartText.setText(resources.getString(R.string.wifi_hotspot_data_usage_start_end, WIFI_DATA_PLAN_INVALID_DATA, WIFI_DATA_PLAN_INVALID_DATA));
            mUsageBarEndText.setText(resources.getString(R.string.wifi_hotspot_data_usage_start_end, WIFI_DATA_PLAN_INVALID_DATA, WIFI_DATA_PLAN_INVALID_DATA));
            mWifiUsageBarWidget.setNeedleProgress(0.0f);
        } else {
            String formattedPlanDataLimit = mWifiHotspotUtil.getFormattedDataString(dataPlanLimit);
            String formattedPlanDataUsed = mWifiHotspotUtil.getFormattedDataString(dataUsed);
            String convertedUsedValueInDataPlanUom = mConversionUtil.convertDataValue(formattedPlanDataUsed, usageUom, dataPlanUom, 2);
            float convertedFloatUsedValueInPlanUom = Float.parseFloat(convertedUsedValueInDataPlanUom);

            mDataUsedTextView.setText(resources.getString(R.string.wifi_hotspot_data_plan_usage_of_total, formattedPlanDataUsed, usageUom, formattedPlanDataLimit, dataPlanUom));
            mUsageBarStartText.setText(resources.getString(R.string.wifi_hotspot_data_usage_start_end, "0", dataPlanUom));
            mUsageBarEndText.setText(resources.getString(R.string.wifi_hotspot_data_usage_start_end, formattedPlanDataLimit, dataPlanUom));
            mWifiUsageBarWidget.setNeedleProgress((float) Math.min(convertedFloatUsedValueInPlanUom / dataPlanLimit, 1.0));
            mWifiUsageBarWidget.setGaugeNeedleFillGradient(R.color.gauge_gradient_start_color, R.color.gauge_gradient_end_color);
        }

        DateFormat dateFormat = mDateFormatProvider.getDateTimeFormat(getResources().getString(R.string.wifi_hotspot_data_plan_refresh_status_date_time_format));
        mStatusRefreshTimeTextView.setVisibility(View.VISIBLE);
        mStatusRefreshTimeTextView.setText(resources.getString(R.string.wifi_hotspot_data_plan_status_timestamp, dateFormat.format(refreshTime)));
    }

    @Override
    public void updateUiWithDataUsageErrorInfo() {
        mDataPlanTextView.setText(getString(R.string.wifi_hotspot_data_info_error_title));

        mLimitedPlanUsageContainer.setVisibility(View.GONE);
        mUnlimitedPlanUsageContainer.setVisibility(View.GONE);
        mDataPlanRenewalOrExpireDetailTextView.setVisibility(View.GONE);
        mStatusRefreshTimeTextView.setVisibility(View.GONE);

        mHotspotDataInfoBanner.setVisibility(View.VISIBLE);
        mHotspotDataInfoBanner.setBackgroundColor(ContextCompat.getColor(this, R.color.ford_style_red));
        mHotspotDataInfoBanner.setText(R.string.wifi_hotspot_data_info_error_banner_text);

        if (mWifiHotspotUtilsProvider.isErrorBannerRefreshIconEnabled()) {
            Drawable drawableRefresh = ContextCompat.getDrawable(this, R.drawable.operate_ic_refresh_dark);
            mColorUtil.applyTintOnTopOfDrawable(drawableRefresh, Color.WHITE);
            mHotspotDataInfoBanner.setCompoundDrawablesWithIntrinsicBounds(null, null, drawableRefresh, null);
        } else {
            mHotspotDataInfoBanner.setOnClickListener(null);
        }
    }

    @Override
    public void showUnlimitedDataPlanInfo(NgsdnWifiDataPlanUsage dataPlanUsage) {
        mUnlimitedPlanUsageContainer.setVisibility(View.VISIBLE);
        mLimitedPlanUsageContainer.setVisibility(View.GONE);
        if (!TextUtils.isBlank(dataPlanUsage.getDataUsageUnitOfMeasurement())) {
            float dataUsed = dataPlanUsage.getDataPlanUsed();
            String usageUom = dataPlanUsage.getDataUsageUnitOfMeasurement();
            String formattedPlanDataUsed = mWifiHotspotUtil.getFormattedDataString(dataUsed);
            mUnlimitedPlanWidgetUsageText.setText(getResources().getString(R.string.wifi_hotspot_data_unlimited_usage, formattedPlanDataUsed, usageUom));
        }
        mDataPlanTextView.setText(getString(R.string.wifi_hotspot_data_unlimited_data));
    }

    @Override
    public void showLimitedDataPlanInfo(float dataPlanSize, String dataPlanUom) {
        mUnlimitedPlanUsageContainer.setVisibility(View.GONE);
        mLimitedPlanUsageContainer.setVisibility(View.VISIBLE);
        String formattedDataPlanSize = mWifiHotspotUtil.getFormattedDataString(dataPlanSize);
        mDataPlanTextView.setText(getResources().getString(R.string.wifi_hotspot_data_plan, formattedDataPlanSize, dataPlanUom));
    }

    @Override
    public void showManageAccountButton() {
        mManageAccountButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void launchExternalWebsite(final String url) {
        mWifiDialogProvider.createExternalWebsiteWarningDialog(this, url).show();
    }

    @Override
    public void setupHotspotTrialInfoBanner() {
        mHotspotDataInfoBanner.setVisibility(View.VISIBLE);
        mHotspotDataInfoBanner.setBackgroundColor(ContextCompat.getColor(this, R.color.ford_green));
        mHotspotDataInfoBanner.setText(R.string.wifi_hotspot_data_usage_data_trial_banner_text);
        mHotspotDataInfoBanner.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
    }

    @Override
    public void setupHotspotOverageInfoBanner() {
        mHotspotDataInfoBanner.setVisibility(View.VISIBLE);
        mHotspotDataInfoBanner.setBackgroundColor(ContextCompat.getColor(this, R.color.ford_style_red));
        mHotspotDataInfoBanner.setText(R.string.wifi_hotspot_data_overage_banner_text);
    }

    @Override
    public void setupHotspotLimitedDataInfoBanner() {
        mHotspotDataInfoBanner.setVisibility(View.VISIBLE);
        mHotspotDataInfoBanner.setBackgroundColor(ContextCompat.getColor(this, R.color.ford_style_yellow));
        mHotspotDataInfoBanner.setText(R.string.wifi_hotspot_data_limited_data_banner_text);
    }

    @Override
    public void setDataUsageBarColor(@ColorRes int startColor, @ColorRes int endColor) {
        mWifiUsageBarWidget.setGaugeNeedleFillGradient(startColor, endColor);
        mHotspotDataInfoBanner.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
    }

    @Override
    public void hideHotspotDataInfoBanner() {
        mHotspotDataInfoBanner.setVisibility(View.GONE);
    }

    @Override
    public void setRenewalCycleDateInfo(String renewalDate) {
        mDataPlanRenewalOrExpireDetailTextView.setVisibility(View.VISIBLE);
        if (!TextUtils.isBlank(renewalDate)) {
            renewalDate = mWifiHotspotUtil.getFormattedDate(renewalDate, getResources().getString(R.string.wifi_hotspot_data_plan_renewal_time_date_format));
            mDataPlanRenewalOrExpireDetailTextView.setText(getResources().getString(R.string.wifi_hotspot_data_plan_renews_on, renewalDate));
        } else {
            String emptyRenewalDateFormat = getResources().getString(R.string.wifi_hotspot_data_plan_renewal_empty_date_format);
            mDataPlanRenewalOrExpireDetailTextView.setText(getResources().getString(R.string.wifi_hotspot_data_plan_renews_on, emptyRenewalDateFormat));
        }
    }

    @Override
    public void setExpiryDateInfo(String expiryDate) {
        if (!TextUtils.isBlank(expiryDate)) {
            expiryDate = mWifiHotspotUtil.getFormattedDate(expiryDate, getResources().getString(R.string.wifi_hotspot_data_plan_expiry_time_date_format));
            mDataPlanRenewalOrExpireDetailTextView.setVisibility(View.VISIBLE);
            mDataPlanRenewalOrExpireDetailTextView.setText(getResources().getString(R.string.wifi_hotspot_data_plan_unused_expires_on, expiryDate));
        } else {
            String emptyExpiryDateFormat = getResources().getString(R.string.wifi_hotspot_data_plan_expiry_empty_date_format);
            mDataPlanRenewalOrExpireDetailTextView.setVisibility(View.VISIBLE);
            mDataPlanRenewalOrExpireDetailTextView.setText(getResources().getString(R.string.wifi_hotspot_data_plan_unused_expires_on, emptyExpiryDateFormat));
        }
    }

    @Override
    public void showUsageInfoDialog(NgsdnWifiDataPlan ngsdnWifiDataPlan, NgsdnWifiDataPlanUsage ngsdnWifiDataPlanUsage, String url) {
        Dialog wifiInfoDialog = mWifiDialogProvider.createWifiInfoDialog(this,
                ngsdnWifiDataPlan.getSubscriptionType(),
                ngsdnWifiDataPlan.getExpiryDateString(),
                ngsdnWifiDataPlan.getRenewalCycleDateString(),
                ngsdnWifiDataPlanUsage.getDataPlanUsedPercent(),
                mWifiHotspotUtilsProvider.isOverageIndicatorOn(ngsdnWifiDataPlanUsage),
                ngsdnWifiDataPlanUsage.getDataPlanLimit(),
                ngsdnWifiDataPlanUsage.getTotalDataUnitOfMeasurement(),
                url);

        if (wifiInfoDialog != null) {
            wifiInfoDialog.show();
        }
    }

    @Override
    public void hideDataPlanRenewalOrExpiryDateInfo() {
        mDataPlanRenewalOrExpireDetailTextView.setVisibility(View.INVISIBLE);
    }

    @NonNull
    @Override
    public WifiHotspotUtil.WifiLaunchParameters computeWifiScreenForError(Throwable error) {
        return mWifiHotspotUtil.getWifiLaunchParametersForDataUsageError(WifiHotspotActivity.this, error, mVehicle);
    }

    @NonNull
    @Override
    public WifiHotspotUtil.WifiLaunchParameters computeWifiScreenForDataPlan(NgsdnWifiDataPlan dataPlan) {
        return mWifiHotspotUtil.getWifiLaunchParametersForDataUsageSuccess(WifiHotspotActivity.this, dataPlan, mVehicle);
    }

    @Override
    public void launchWifiSetupScreen(String setupBodyTitle, String setupBodyText, String primaryButtonText, String secondaryButtonText) {

    }

    //endregion

    //region OnClick

    @OnClick(R.id.activity_wifi_hotspot_status_refresh_container)
    protected void onRefreshStatusClicked() {
        mManualStatusRefreshing = true;
        mFordObjectAnimator.start();
        checkActiveNetworkConnection();
    }

    @OnClick(R.id.activity_wifi_hotspot_manage_account)
    protected void onManageAccountButtonClicked() {
        mPresenter.manageAccountClicked();
    }

    @Optional
    @OnClick(R.id.activity_wifi_hotspot_contact_carrier)
    protected void onContactCarrierButtonClicked() {
        mDialerUtil.launchDialerIntent(this, R.string.wifi_hotspot_carrier_hotline);
    }

    @OnClick(R.id.activity_wifi_hotspot_data_info_banner)
    protected void onDataUsageErrorBannerRefreshClicked() {
        mPresenter.dataUsageInfoBannerClicked();
    }

    @OnClick(R.id.wifi_usage_bar_info_icon)
    protected void onWifiUsageBarInfoIconClicked() {
        mPresenter.usageBarInfoIconClicked();
    }

    @OnClick(R.id.wifi_unlimited_usage_info_icon)
    protected void onWifiUnlimitedInfoIconClicked() {
        mPresenter.usageBarInfoIconClicked();
    }

    //endregion

    public boolean isManualStatusRefreshing() {
        return mManualStatusRefreshing;
    }

    public void setManualStatusRefreshing(boolean manualStatusRefreshing) {
        this.mManualStatusRefreshing = manualStatusRefreshing;
    }

    private void setupStatusRefresh() {
        mFordObjectAnimator.setTarget(mStatusRefreshSpinnerImageView);
        mFordObjectAnimator.setPropertyName("rotation");
        mFordObjectAnimator.setFloatValues(0f, 359f);
        mFordObjectAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mFordObjectAnimator.setInterpolator(new LinearInterpolator());
        mFordObjectAnimator.setDuration(REFRESH_DURATION);
        mFordObjectAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationRepeat(Animator animation) {
                if (!mManualStatusRefreshing) {
                    animation.end();
                }
            }
        });
    }

    private void checkActiveNetworkConnection() {
        if (DeviceUtil.hasActiveNetworkConnection(WifiHotspotActivity.this)) {
            mPresenter.activeNetworkConnectionAvailable();
        } else {
            Throwable error = new NgsdnException(StatusCodes.ERROR_RETROFIT_NO_CONNECTION);
            mErrorDialogUtil.showErrorDialog(error, WifiHotspotActivity.this, null);
            hideLoadingSpinner();
        }
    }

    //region Classes

    public static class Extras {
        public final static String VEHICLE = "vehicle";
        public final static String WIFI_DATA_PLAN = "wifi_data_plan";
        public final static String WIFI_DATA_PLAN_USAGE = "wifi_data_plan_usage";
        public final static String WIFI_DATA_USAGE_RESPONSE_STATUS = "wifi_data_usage_response_status";
    }

    //endregion
}
