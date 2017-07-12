package com.ford.oa.myvehicles.wifihotspot.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ford.networkutils.NetworkingErrorUtil;
import com.ford.networkutils.StatusCodes;
import com.ford.oa.R;
import com.ford.oa.commons.utils.DateUtil;
import com.ford.utils.TextUtils;
import com.ford.vehiclecommon.models.Vehicle;
import com.ford.wifihotspot.models.NgsdnWifiDataPlan;
import com.google.common.base.Optional;

import java.io.IOException;

import javax.inject.Inject;

public class WifiHotspotUtil {

    private static final String WIFI_EXPIRY_RENEWAL_ALTERNATIVE_FORMAT = "yyyy-MM-dd";

    private final DateUtil mDateUtil;

    private final NetworkingErrorUtil mNetworkingErrorUtil;

    @Inject
    WifiHotspotUtil(DateUtil dateUtil, NetworkingErrorUtil networkingErrorUtil) {
        mDateUtil = dateUtil;
        mNetworkingErrorUtil = networkingErrorUtil;
    }

    public @NonNull String getFormattedDate(@Nullable String dateString, @Nullable String toFormat) {
        String resultDate = null;
        if (!TextUtils.isBlank(dateString) && !TextUtils.isBlank(toFormat)) {
            resultDate = mDateUtil.parseISO8601ToStringIgnoreTimezone(dateString, toFormat);
            if (resultDate == null) {
                resultDate = mDateUtil.parseDateToString(dateString, WIFI_EXPIRY_RENEWAL_ALTERNATIVE_FORMAT, toFormat);
            }
        }
        return resultDate != null ? resultDate : "";
    }

    public @NonNull String getFormattedDataString(float dataValue) {
        String stringDataValue = String.valueOf(dataValue);
        int firstIndexOfDecimal = stringDataValue.indexOf('.');
        if (firstIndexOfDecimal != -1 && stringDataValue.lastIndexOf('.') == firstIndexOfDecimal) {
            String[] split = stringDataValue.split("\\.");
            if (split.length == 2) {
                if (split[1].length() == 1) {
                    if ("0".equals(split[1])) {
                        return split[0];
                    } else {
                        return split[0] + "." + split[1] + "0";
                    }
                } else {
                    return split[0] + "." + split[1].substring(0, 2);
                }
            } else if (split.length == 1) {
                return split[0];
            }
        }
        return "";
    }

    public @NonNull WifiLaunchParameters getWifiLaunchParametersForDataUsageError(Context context, Throwable error, Vehicle vehicle) {
        final int errorStatusCode = mNetworkingErrorUtil.getErrorStatusCode(error);

        if (errorStatusCode == StatusCodes.ERROR_WIFI_DATA_PLAN_NOT_SET_UP) {
            String wifiDataPlanSecondOptionButtonText = context.getString(R.string.wifi_hotspot_setup_authorize);
            if (vehicle.isTcuEnabled() && vehicle.isPendingAuthorization()) {
                wifiDataPlanSecondOptionButtonText = context.getString(R.string.wifi_hotspot_setup_already_set_up);
            }
            return new WifiLaunchParameters(WifiScreen.WIFI_SETUP,
                    context.getString(R.string.wifi_hotspot_setup_data_plan),
                    wifiDataPlanSecondOptionButtonText,
                    context.getString(R.string.wifi_hotspot_setup_page_title),
                    context.getString(R.string.wifi_hotspot_setup_body_not_activated));

        } else if (error instanceof IOException) {
            return new WifiLaunchParameters(WifiScreen.ERROR);
        } else {
            return new WifiLaunchParameters(WifiScreen.WIFI_USAGE);
        }
    }

    public @NonNull WifiLaunchParameters getWifiLaunchParametersForDataUsageSuccess(Context context, NgsdnWifiDataPlan dataPlan, Vehicle vehicle) {
        String wifiDataPlanStatus = dataPlan != null ? dataPlan.getDataPlanStatus() : null;

        if (dataPlan != null && wifiDataPlanStatus != null) {
            String primaryButtonText = context.getString(R.string.wifi_hotspot_setup_data_plan);
            String secondaryButtonText = context.getString(R.string.wifi_hotspot_setup_set_up_later);
            String wifiDataPlanBodyTitleText = context.getString(R.string.wifi_hotspot_setup_page_title);
            String wifiSetupBodyText = context.getString(R.string.wifi_hotspot_setup_body);

            Optional<Vehicle.Authorization> authorization = vehicle.getAuthorization();
            switch (wifiDataPlanStatus) {
                case NgsdnWifiDataPlan.PENDING:
                    if (dataPlan.getSubscriptionType().equals(NgsdnWifiDataPlan.TRIAL)) {
                        wifiDataPlanBodyTitleText = context.getString(R.string.wifi_hotspot_setup_complimentary_trial);
                        if (vehicle.isAuthorized()) {
                            primaryButtonText = context.getString(R.string.wifi_hotspot_setup_trial_pending_primary_button_text);
                            wifiSetupBodyText = context.getString(R.string.wifi_hotspot_setup_body_learn_more_trial);
                        } else if (authorization.isPresent() && authorization.get().equals(Vehicle.Authorization.UNAUTHORIZED)) {
                            secondaryButtonText = context.getString(R.string.wifi_hotspot_setup_authorize);
                            wifiSetupBodyText = context.getString(R.string.wifi_hotspot_setup_body_not_activated);
                        } else {
                            secondaryButtonText = context.getString(R.string.wifi_hotspot_setup_already_set_up);
                            wifiSetupBodyText = context.getString(R.string.wifi_hotspot_setup_body_not_activated);
                        }
                        return new WifiLaunchParameters(WifiScreen.WIFI_SETUP,primaryButtonText, secondaryButtonText, wifiDataPlanBodyTitleText, wifiSetupBodyText);
                    }
                case NgsdnWifiDataPlan.INACTIVE:
                case NgsdnWifiDataPlan.EXPIRED:
                    if (authorization.isPresent() && authorization.get().equals(Vehicle.Authorization.UNAUTHORIZED)) {
                        secondaryButtonText = context.getString(R.string.wifi_hotspot_setup_authorize);
                        wifiSetupBodyText = context.getString(R.string.wifi_hotspot_setup_body_not_activated);
                    } else if (vehicle.isAuthorized()) {
                        secondaryButtonText = context.getString(R.string.wifi_hotspot_setup_set_up_later);
                    } else {
                        secondaryButtonText = context.getString(R.string.wifi_hotspot_setup_already_set_up);
                    }
                    return new WifiLaunchParameters(WifiScreen.WIFI_SETUP,primaryButtonText, secondaryButtonText, wifiDataPlanBodyTitleText, wifiSetupBodyText);

                case NgsdnWifiDataPlan.ACTIVE:
                    return new WifiLaunchParameters(WifiScreen.WIFI_USAGE);
                default:
                    break;
            }
        }

        return new WifiLaunchParameters(WifiScreen.ERROR);
    }

    public enum WifiScreen {
        WIFI_SETUP, WIFI_USAGE, ERROR
    }

    public class WifiLaunchParameters {

        private WifiScreen mScreenToLaunch;
        private String mSetupPrimaryButtonText;
        private String mSetupSecondaryButtonText;
        private String mSetupBodyTitle;
        private String mSetupBodyText;

        public WifiLaunchParameters(WifiScreen screenToLaunch) {
            mScreenToLaunch = screenToLaunch;
        }

        public WifiLaunchParameters(WifiScreen screenToLaunch, String primaryButtonText, String secondaryButtonText, String bodyTitle, String bodyText) {
            mScreenToLaunch = screenToLaunch;
            mSetupPrimaryButtonText = primaryButtonText;
            mSetupSecondaryButtonText = secondaryButtonText;
            mSetupBodyTitle = bodyTitle;
            mSetupBodyText = bodyText;
        }

        public WifiScreen getScreenToLaunch() {
            return mScreenToLaunch;
        }

        public String getSetupBodyTitle() {
            return mSetupBodyTitle;
        }

        public String getSetupPrimaryButtonText() {
            return mSetupPrimaryButtonText;
        }

        public String getSetupSecondaryButtonText() {
            return mSetupSecondaryButtonText;
        }

        public String getSetupBodyText() {
            return mSetupBodyText;
        }
    }
}
