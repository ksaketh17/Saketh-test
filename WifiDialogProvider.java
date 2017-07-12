package com.ford.oa.myvehicles.wifihotspot.providers;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.Nullable;

import com.ford.oa.R;
import com.ford.oa.activity.WebviewActivity;
import com.ford.oa.ui.dialog.FordDialogFactory;
import com.ford.oa.myvehicles.wifihotspot.utils.WifiHotspotUtil;
import com.ford.utils.BrowserUtil;
import com.ford.utils.TextUtils;
import com.ford.wifihotspot.models.NgsdnWifiDataPlan;
import com.google.common.base.Optional;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

public class WifiDialogProvider {
    private final FordDialogFactory mFordDialogFactory;
    private final WifiHotspotUtil mWifiHotspotUtil;
    private final BrowserUtil mBrowserUtil;

    @Inject
    public WifiDialogProvider(FordDialogFactory fordDialogFactory, WifiHotspotUtil wifiHotspotUtil, BrowserUtil browserUtil) {
        mFordDialogFactory = fordDialogFactory;
        mWifiHotspotUtil = wifiHotspotUtil;
        mBrowserUtil = browserUtil;
    }

    public @Nullable
    Dialog createWifiInfoDialog(final Context activityContext, final String dataPlanSubscriptionType, final String dataPlanExpiryDateString,
                                final String dataPlanRenewalCycleDateString, int dataPlanUsedPercent, boolean isOverageIndicatorOn,
                                float dataPlanLimit, final String dataPlanUom, final String seeDetailsUrl) {
        final DataPlanType dataPlanType = getDataPlanType(dataPlanSubscriptionType, dataPlanRenewalCycleDateString, dataPlanUsedPercent, isOverageIndicatorOn);

        String dialogTitle = "";
        String dialogMessage = "";
        List<String> dialogButtons = null;

        final String formattedDataPlanAmount = mWifiHotspotUtil.getFormattedDataString(dataPlanLimit);

        switch (dataPlanType) {
            case PAIDSESSION_AUTORENEWAL_WITH_DATA:
                dialogTitle = activityContext.getString(R.string.wifi_hotspot_data_usage_info_dialog_limited_plan_title, formattedDataPlanAmount, dataPlanUom);
                dialogMessage = activityContext.getString(R.string.wifi_hotspot_limited_data_dialog_expiry_message, formattedDataPlanAmount,
                        dataPlanUom, mWifiHotspotUtil.getFormattedDate(dataPlanRenewalCycleDateString, activityContext.getString(R.string.wifi_hotspot_data_plan_expiry_time_date_format)));
                dialogButtons = Arrays.asList(activityContext.getString(R.string.dialog_see_details), activityContext.getString(R.string.dialog_close));
                break;
            case PAIDSESSION_AUTORENEWAL_WITHOUT_DATA:
                dialogTitle = activityContext.getString(R.string.wifi_hotspot_data_usage_info_dialog_limited_plan_title, formattedDataPlanAmount, dataPlanUom);
                dialogMessage = activityContext.getString(R.string.wifi_hotspot_limited_data_dialog_renewal_exceeded_message,
                        mWifiHotspotUtil.getFormattedDate(dataPlanRenewalCycleDateString, activityContext.getString(R.string.wifi_hotspot_data_plan_renewal_time_date_format)));
                dialogButtons = Arrays.asList(activityContext.getString(R.string.dialog_see_details), activityContext.getString(R.string.dialog_close));
                break;
            case PAIDSHARED_WITH_DATA:
                String displayDate = !TextUtils.isBlank(dataPlanExpiryDateString) ? dataPlanExpiryDateString : dataPlanRenewalCycleDateString;
                dialogTitle = activityContext.getString(R.string.wifi_hotspot_data_usage_info_dialog_limited_plan_title, formattedDataPlanAmount, dataPlanUom);
                dialogMessage = activityContext.getString(R.string.wifi_hotspot_limited_data_dialog_paid_shared_before_exceeded_message, formattedDataPlanAmount,
                        dataPlanUom, mWifiHotspotUtil.getFormattedDate(displayDate, activityContext.getString(R.string.wifi_hotspot_data_plan_expiry_time_date_format)));
                dialogButtons = Arrays.asList(activityContext.getString(R.string.dialog_see_details), activityContext.getString(R.string.dialog_close));
                break;
            case PAIDSHARED_WITHOUT_DATA:
                dialogTitle = activityContext.getString(R.string.wifi_hotspot_data_usage_info_dialog_limited_plan_title, formattedDataPlanAmount, dataPlanUom);
                dialogMessage = activityContext.getString(R.string.wifi_hotspot_limited_data_dialog_paid_shared_exceeded_message);
                dialogButtons = Arrays.asList(activityContext.getString(R.string.dialog_see_details), activityContext.getString(R.string.dialog_close));
                break;
            case UNLIMITED:
                dialogTitle = activityContext.getString(R.string.wifi_hotspot_data_usage_info_dialog_unlimited_plan_title);
                dialogMessage = activityContext.getString(R.string.wifi_hotspot_unlimited_data_dialog_message);
                dialogButtons = Arrays.asList(activityContext.getString(R.string.dialog_see_details), activityContext.getString(R.string.dialog_close));
                break;
            case PAIDSESSION_NORENEWAL:
            case TRIAL:
                dialogTitle = activityContext.getString(R.string.wifi_hotspot_data_usage_info_dialog_limited_plan_title, formattedDataPlanAmount, dataPlanUom);
                dialogMessage = activityContext.getString(R.string.wifi_hotspot_trial_or_shared_without_renewal_dialog_message, formattedDataPlanAmount,
                        dataPlanUom, mWifiHotspotUtil.getFormattedDate(dataPlanExpiryDateString, activityContext.getString(R.string.wifi_hotspot_data_plan_expiry_time_date_format)));
                dialogButtons = Arrays.asList(activityContext.getString(R.string.dialog_see_details), activityContext.getString(R.string.dialog_close));
                break;
            case UNSUPPORTED:
            default:
                break;
        }

        if (!TextUtils.isBlank(dialogTitle) && !TextUtils.isBlank(dialogMessage) && dialogButtons != null) {
            return mFordDialogFactory.create(activityContext, dialogTitle, dialogMessage, dialogButtons, FordDialogFactory.ButtonLayoutOrientation.VERTICAL, new FordDialogFactory.FordDialogListener() {
                @Override
                public void onButtonClickedAtIndex(int index) {
                    if(index == 0) {
                        createExternalWebsiteWarningDialog(activityContext, seeDetailsUrl).show();
                    }
                }
            });
        }

        return null;
    }

    public Dialog createExternalWebsiteWarningDialog(final Context context, final String url) {
        return createExternalWebsiteWarningDialog(context, url, Optional.<String>absent());
    }

    public Dialog createExternalWebsiteWarningDialog(final Context context, final String url, final Optional<String> postData) {
        return mFordDialogFactory.create(context,
                context.getString(R.string.wifi_hotspot_leaving_fordpass_dialog_title),
                context.getString(R.string.wifi_hotspot_leaving_fordpass_dialog_body),
                Arrays.asList(context.getString(R.string.dialog_no), context.getString(R.string.dialog_yes)),
                new FordDialogFactory.FordDialogListener() {
                    @Override
                    public void onButtonClickedAtIndex(int index) {
                        if (index == 1) {
                            if (postData.isPresent()) {
                                WebviewActivity.newInstance(context, url, true, R.string.wifi_data_plan_setup_webview_title, postData);
                            } else {
                                mBrowserUtil.launchBrowserIntent(context, url);
                            }
                        }
                    }
                });
    }

    DataPlanType getDataPlanType(final String dataPlanSubscriptionType, final String dataPlanRenewalCycleDateString, int dataPlanUsedPercent, boolean isOverageIndicatorOn) {
        if (NgsdnWifiDataPlan.PAID_SESSION.equals(dataPlanSubscriptionType)) {
            if (!TextUtils.isBlank(dataPlanRenewalCycleDateString)) {
                if (dataPlanUsedPercent < 100) {
                    return DataPlanType.PAIDSESSION_AUTORENEWAL_WITH_DATA;
                } else {
                    return DataPlanType.PAIDSESSION_AUTORENEWAL_WITHOUT_DATA;
                }
            } else {
                return DataPlanType.PAIDSESSION_NORENEWAL;
            }
        } else if (NgsdnWifiDataPlan.PAID_SHARED.equals(dataPlanSubscriptionType)) {
            if (!isOverageIndicatorOn) {
                return DataPlanType.PAIDSHARED_WITH_DATA;
            } else {
                return DataPlanType.PAIDSHARED_WITHOUT_DATA;
            }
        } else if (NgsdnWifiDataPlan.PAID_SESSION_UNLIMITED.equals(dataPlanSubscriptionType) || NgsdnWifiDataPlan.PAID_SHARED_UNLIMITED.equals(dataPlanSubscriptionType)) {
            return DataPlanType.UNLIMITED;
        } else if (NgsdnWifiDataPlan.TRIAL.equals(dataPlanSubscriptionType)) {
            return DataPlanType.TRIAL;
        } else {
            return DataPlanType.UNSUPPORTED;
        }
    }

    enum DataPlanType {
        PAIDSESSION_AUTORENEWAL_WITH_DATA,
        PAIDSESSION_AUTORENEWAL_WITHOUT_DATA,
        PAIDSESSION_NORENEWAL,
        PAIDSHARED_WITH_DATA,
        PAIDSHARED_WITHOUT_DATA,
        UNLIMITED,
        TRIAL,
        UNSUPPORTED
    }
}
