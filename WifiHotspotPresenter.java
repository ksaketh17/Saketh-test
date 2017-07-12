package com.ford.oa.myvehicles.wifihotspot.activities;

import com.ford.networkutils.StatusCodes;
import com.ford.oa.R;
import com.ford.oa.config.BaseApiConfig;
import com.ford.oa.myvehicles.providers.WifiHotspotConstantsProvider;
import com.ford.oa.myvehicles.providers.WifiHotspotUtilsProvider;
import com.ford.oa.presenters.BaseAttachablePresenter;
import com.ford.oa.myvehicles.wifihotspot.utils.WifiHotspotUtil;
import com.ford.oa.vehicles.providers.VehicleInfoProvider;
import com.ford.utils.CalendarProvider;
import com.ford.utils.TextUtils;
import com.ford.wifihotspot.models.NgsdnWifiDataPlan;
import com.ford.wifihotspot.models.NgsdnWifiDataPlanUsage;
import com.ford.wifihotspot.models.NgsdnWifiDataUsageResponse;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;

import javax.inject.Inject;

import rx.Subscriber;

public class WifiHotspotPresenter extends BaseAttachablePresenter<WifiHotspotContract.View> implements WifiHotspotContract.Presenter {

    private final VehicleInfoProvider mVehicleInfoProvider;
    private final CalendarProvider mCalendarProvider;
    private final BaseApiConfig mBaseApiConfig;
    private final WifiHotspotUtil mWifiHotspotUtil;
    private final WifiHotspotConstantsProvider mConstantsProvider;
    private final WifiHotspotUtilsProvider mWifiHotspotUtilsProvider;
    private boolean mIsDataUsageErrorBannerDisplayed;

    private String mVin;
    private Optional<NgsdnWifiDataPlan> mDataPlan = Optional.absent();
    private Optional<NgsdnWifiDataPlanUsage> mDataPlanUsage = Optional.absent();
    private Optional<Integer> mDataUsageResponseStatus = Optional.absent();

    @Inject
    public WifiHotspotPresenter(VehicleInfoProvider vehicleInfoProvider,
                                CalendarProvider calendarProvider,
                                BaseApiConfig baseApiConfig,
                                WifiHotspotUtil wifiHotspotUtil,
                                WifiHotspotConstantsProvider wifiHotspotConstantsProvider,
                                WifiHotspotUtilsProvider wifiHotspotUtilsProvider) {
        mVehicleInfoProvider = vehicleInfoProvider;
        mCalendarProvider = calendarProvider;
        mBaseApiConfig = baseApiConfig;
        mWifiHotspotUtil = wifiHotspotUtil;
        mConstantsProvider = wifiHotspotConstantsProvider;
        mWifiHotspotUtilsProvider = wifiHotspotUtilsProvider;
    }

    @Override
    public Class<WifiHotspotContract.View> getViewClass() {
        return WifiHotspotContract.View.class;
    }

    //region WifiHotspotContract Methods

    @Override
    public void setVehicleVin(String vehicleVin) {
        mVin = vehicleVin;
    }

    @Override
    public void setWifiData(NgsdnWifiDataPlan dataPlan, NgsdnWifiDataPlanUsage dataPlanUsage, int status) {
        mDataPlan = Optional.fromNullable(dataPlan);
        mDataPlanUsage = Optional.fromNullable(dataPlanUsage);
        mDataUsageResponseStatus = Optional.fromNullable(status);
    }

    @Override
    public void activeNetworkConnectionAvailable() {
        fetchDataPlan();
    }

    @Override
    public void manageAccountClicked() {
        Optional<String> manageAccountUrl = mBaseApiConfig.getWifiHotspotExternalUrlConfig().getManageAccountUrl(mVin);
        if (manageAccountUrl.isPresent()) {
            mViewProxy.launchExternalWebsite(manageAccountUrl.get());
        }
    }

    @Override
    public void updateViewWithWifiDataUsage() {
        if (mDataPlan.isPresent() && mDataPlanUsage.isPresent()
                && mDataUsageResponseStatus.isPresent() && mDataUsageResponseStatus.get() == StatusCodes.SUCCESS) {
            updateViewsWithAvailableData();
        } else {
            updateViewsWithUnavailableData();
        }
    }

    @Override
    public void usageBarInfoIconClicked() {
        Optional<String> serviceProviderUrl = mBaseApiConfig.getWifiHotspotExternalUrlConfig().getManageAccountUrl(mVin);
        if (mDataPlan.isPresent() && mDataPlanUsage.isPresent() && serviceProviderUrl.isPresent()) {
            mViewProxy.showUsageInfoDialog(mDataPlan.get(), mDataPlanUsage.get(), serviceProviderUrl.get());
        }
    }

    @Override
    public void dataUsageInfoBannerClicked() {
        if (mIsDataUsageErrorBannerDisplayed) {
            fetchDataPlan();
        }
    }

    //endregion

    @VisibleForTesting
    protected void fetchDataPlan() {
        if (!TextUtils.isBlank(mVin)) {
            mViewProxy.showLoadingSpinner();
            safeSubscribe(mVehicleInfoProvider.getWifiDataPlanAndUsage(mVin), new Subscriber<NgsdnWifiDataUsageResponse>() {
                @Override
                public void onCompleted() {
                    mIsDataUsageErrorBannerDisplayed = false;
                }

                @Override
                public void onError(Throwable error) {
                    mViewProxy.hideLoadingSpinner();

                    WifiHotspotUtil.WifiLaunchParameters wifiLaunchParameters = mViewProxy.computeWifiScreenForError(error);
                    switch (wifiLaunchParameters.getScreenToLaunch()) {
                        case WIFI_SETUP:
                            mViewProxy.launchWifiSetupScreen(wifiLaunchParameters.getSetupBodyTitle(), wifiLaunchParameters.getSetupBodyText(), wifiLaunchParameters.getSetupPrimaryButtonText(), wifiLaunchParameters.getSetupSecondaryButtonText());
                            break;
                        default:
                        case WIFI_USAGE:
                        case ERROR:
                            mIsDataUsageErrorBannerDisplayed = true;
                            updateViewsWithUnavailableData();
                            break;
                    }
                }

                @Override
                public void onNext(NgsdnWifiDataUsageResponse ngsdnWifiDataUsageResponse) {
                    mViewProxy.hideLoadingSpinner();
                    mDataPlan = Optional.fromNullable(ngsdnWifiDataUsageResponse.getWifiDataPlan());
                    mDataPlanUsage = Optional.fromNullable(ngsdnWifiDataUsageResponse.getWifiDataPlanUsage());
                    mDataUsageResponseStatus = Optional.fromNullable(ngsdnWifiDataUsageResponse.getStatus());

                    WifiHotspotUtil.WifiLaunchParameters wifiLaunchParameters = mViewProxy.computeWifiScreenForDataPlan(ngsdnWifiDataUsageResponse.getWifiDataPlan());
                    switch (wifiLaunchParameters.getScreenToLaunch()) {
                        case WIFI_SETUP:
                            mViewProxy.launchWifiSetupScreen(wifiLaunchParameters.getSetupBodyTitle(), wifiLaunchParameters.getSetupBodyText(), wifiLaunchParameters.getSetupPrimaryButtonText(), wifiLaunchParameters.getSetupSecondaryButtonText());
                            break;
                        default:
                        case WIFI_USAGE:
                        case ERROR:
                            updateViewWithWifiDataUsage();
                            break;
                    }
                }
            });
        }
    }

    private void updateViewsWithAvailableData() {
        NgsdnWifiDataPlan dataPlan = mDataPlan.get();
        NgsdnWifiDataPlanUsage dataPlanUsage = mDataPlanUsage.get();

        mViewProxy.updateDataPlanUsage(dataPlan, dataPlanUsage, mCalendarProvider.getInstance().getTime());
        mViewProxy.showManageAccountButton();
        if (isDataPlanActive(dataPlan)) {
            updateViewWithActiveDataPlan(dataPlan, dataPlanUsage);
        } else {
            mViewProxy.hideHotspotDataInfoBanner();
        }

        if (shouldShowRenewalOrExpiryDate(dataPlan)) {
            updateDataPlanRenewalOrExpiryDate(dataPlan);
        } else {
            mViewProxy.hideDataPlanRenewalOrExpiryDateInfo();
        }
    }

    private void updateViewWithActiveDataPlan(NgsdnWifiDataPlan dataPlan, NgsdnWifiDataPlanUsage dataPlanUsage) {
        if (isPaidSubscription(dataPlan)) {
            if (NgsdnWifiDataPlan.PAID_SESSION.equals(dataPlan.getSubscriptionType())) {
                updateViewWithPaidSubscription(dataPlanUsage);
            } else {
                if (mWifiHotspotUtilsProvider.isPaidSharedEnabled()) {
                    updateViewWithPaidSubscription(dataPlanUsage);
                } else {
                    updateViewsWithUnavailableData();
                }
            }
        } else if (isTrialSubscription(dataPlan)) {
            updateViewWithTrialSubscription(dataPlanUsage);
        } else {
            mViewProxy.hideHotspotDataInfoBanner();
            if (mWifiHotspotUtilsProvider.isUnlimitedDataPlanEnabled()) {
                mViewProxy.showUnlimitedDataPlanInfo(dataPlanUsage);
            } else {
                updateViewsWithUnavailableData();
            }
        }
    }

    private void updateViewWithPaidSubscription(NgsdnWifiDataPlanUsage dataPlanUsage) {
        if (isDataPlanOverage(dataPlanUsage)) {
            mViewProxy.setupHotspotOverageInfoBanner();
            updateDataUsageBannerColor(dataPlanUsage);
        } else if (isLimitedDataPlanRemaining(dataPlanUsage)) {
            mViewProxy.setupHotspotLimitedDataInfoBanner();
            updateDataUsageBannerColor(dataPlanUsage);
        } else {
            mViewProxy.hideHotspotDataInfoBanner();
        }
        mViewProxy.showLimitedDataPlanInfo(dataPlanUsage.getDataPlanLimit(), dataPlanUsage.getTotalDataUnitOfMeasurement());
    }

    private void updateViewWithTrialSubscription(NgsdnWifiDataPlanUsage dataPlanUsage) {
        if (mConstantsProvider.isTrialOverageEnabled() && isDataPlanOverage(dataPlanUsage)) {
            mViewProxy.setupHotspotOverageInfoBanner();
        } else {
            mViewProxy.setupHotspotTrialInfoBanner();
        }
        mViewProxy.showLimitedDataPlanInfo(dataPlanUsage.getDataPlanLimit(), dataPlanUsage.getTotalDataUnitOfMeasurement());
        updateDataUsageBannerColor(dataPlanUsage);
    }

    private boolean isDataPlanOverage(NgsdnWifiDataPlanUsage dataPlanUsage) {
        return mWifiHotspotUtilsProvider.isOverageIndicatorOn(dataPlanUsage) ||
                isDataPlanLimitReached(dataPlanUsage);
    }

    private boolean isDataPlanActive(NgsdnWifiDataPlan dataPlan) {
        return dataPlan.getDataPlanStatus().equals(NgsdnWifiDataPlan.ACTIVE);
    }

    private boolean isPaidSubscription(NgsdnWifiDataPlan dataPlan) {
        return dataPlan.getSubscriptionType().equals(NgsdnWifiDataPlan.PAID_SESSION)
                || dataPlan.getSubscriptionType().equals(NgsdnWifiDataPlan.PAID_SHARED);
    }

    private boolean isTrialSubscription(NgsdnWifiDataPlan dataPlan) {
        return dataPlan.getSubscriptionType().equals(NgsdnWifiDataPlan.TRIAL);
    }

    private boolean isDataPlanLimitReached(NgsdnWifiDataPlanUsage dataPlanUsage) {
        return dataPlanUsage.getDataPlanUsedPercent() >= 100;
    }

    private void updateDataUsageBannerColor(NgsdnWifiDataPlanUsage dataPlanUsage) {
        if (isDataPlanOverage(dataPlanUsage)) {
            mViewProxy.setDataUsageBarColor(R.color.ford_style_red, R.color.ford_style_red);
        } else if (isLimitedDataPlanRemaining(dataPlanUsage)) {
            mViewProxy.setDataUsageBarColor(R.color.ford_style_yellow, R.color.ford_style_yellow);
        }
    }

    private boolean isLimitedDataPlanRemaining(NgsdnWifiDataPlanUsage dataPlanUsage) {
        return dataPlanUsage.getDataPlanUsedPercent() >= mConstantsProvider.getWifiUsageWarningPercentage()
                && dataPlanUsage.getDataPlanUsedPercent() < 100;
    }

    private void updateViewsWithUnavailableData() {
        mIsDataUsageErrorBannerDisplayed = true;
        mViewProxy.updateUiWithDataUsageErrorInfo();
        mViewProxy.showManageAccountButton();
    }

    private void updateDataPlanRenewalOrExpiryDate(NgsdnWifiDataPlan dataPlan) {
        if (hasExpirationPlan(dataPlan)) {
            mViewProxy.setExpiryDateInfo(dataPlan.getExpiryDateString());
        } else if (mWifiHotspotUtilsProvider.isDataRenewalPlanEnabled()) {
            mViewProxy.setRenewalCycleDateInfo(dataPlan.getRenewalCycleDateString());
        } else {
            mViewProxy.hideDataPlanRenewalOrExpiryDateInfo();
        }
    }

    private boolean hasExpirationPlan(NgsdnWifiDataPlan dataPlan) {
        boolean isTrialPlan = isTrialSubscription(dataPlan);
        boolean isPaidSessionWithExpiry = dataPlan.getSubscriptionType().equals(NgsdnWifiDataPlan.PAID_SESSION) && !TextUtils.isBlank(dataPlan.getExpiryDateString());
        return isTrialPlan || isPaidSessionWithExpiry;
    }

    private boolean shouldShowRenewalOrExpiryDate(NgsdnWifiDataPlan dataPlan) {
        if (dataPlan.getSubscriptionType().equals(NgsdnWifiDataPlan.PAID_SHARED_UNLIMITED)
                && dataPlan.getRenewalCycleDateString() == null
                && dataPlan.getExpiryDateString() == null) {
            return false;
        }
        return true;
    }
}
