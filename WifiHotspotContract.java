package com.ford.oa.myvehicles.wifihotspot.activities;

import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;

import com.ford.oa.myvehicles.wifihotspot.utils.WifiHotspotUtil;
import com.ford.wifihotspot.models.NgsdnWifiDataPlan;
import com.ford.wifihotspot.models.NgsdnWifiDataPlanUsage;

import java.util.Date;

public class WifiHotspotContract {
    public interface Presenter {
        void setVehicleVin(String vehicleVin);
        void setWifiData(NgsdnWifiDataPlan dataPlan, NgsdnWifiDataPlanUsage dataPlanUsage, int status);
        void attachView(View view);
        void detachView();

        void activeNetworkConnectionAvailable();
        void manageAccountClicked();
        void updateViewWithWifiDataUsage();
        void usageBarInfoIconClicked();
        void dataUsageInfoBannerClicked();
    }

    public interface View {
        void showLoadingSpinner();
        void hideLoadingSpinner();

        void showErrorDialog(Throwable e);
        void updateDataPlanUsage(NgsdnWifiDataPlan dataPlan, NgsdnWifiDataPlanUsage dataPlanUsage, Date refreshTime) ;
        void updateUiWithDataUsageErrorInfo();
        void showUnlimitedDataPlanInfo(NgsdnWifiDataPlanUsage dataPlanUsage);
        void showLimitedDataPlanInfo(float dataPlanSize, String dataPlanUom);
        void showManageAccountButton();
        void launchExternalWebsite(final String url);
        void setupHotspotTrialInfoBanner();
        void setupHotspotOverageInfoBanner();
        void setupHotspotLimitedDataInfoBanner();
        void setDataUsageBarColor(@ColorRes int startColor, @ColorRes int endColor);
        void hideHotspotDataInfoBanner();
        void setRenewalCycleDateInfo(String renewalDate);
        void setExpiryDateInfo(String expiryDate);
        void showUsageInfoDialog(NgsdnWifiDataPlan ngsdnWifiDataPlan, NgsdnWifiDataPlanUsage ngsdnWifiDataPlanUsage, String url);
        void hideDataPlanRenewalOrExpiryDateInfo();
        @NonNull WifiHotspotUtil.WifiLaunchParameters computeWifiScreenForError(Throwable error);
        @NonNull WifiHotspotUtil.WifiLaunchParameters computeWifiScreenForDataPlan(NgsdnWifiDataPlan dataPlan);
        void launchWifiSetupScreen(String setupBodyTitle, String setupBodyText, String primaryButtonText, String secondaryButtonText);
    }
}
