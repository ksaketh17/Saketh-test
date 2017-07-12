package com.ford.oa.myvehicles.wifihotspot.activities;

import android.content.Intent;
import android.content.res.Resources;

import com.ford.espresso.EspressoFordApplication;
import com.ford.espresso.MockConfigurationProvider;
import com.ford.espresso.builders.NgsdnAccountInformationResponseBuilder;
import com.ford.espresso.builders.NgsdnWifiHotspotResponseBuilder;
import com.ford.espresso.helpers.MockLocaleProvider;
import com.ford.espresso.utils.FordActivityTestRule;
import com.ford.espresso.utils.FordMockWebServerRule;
import com.ford.espresso.utils.FordTestConfigurationRule;
import com.ford.ngsdn.models.NgsdnAccountInformationResponse;
import com.ford.ngsdn.models.NgsdnVehicleImpl;
import com.ford.ngsdn.providers.NgsdnGsonProvider;
import com.ford.oa.R;
import com.ford.oa.config.ConfigurationProvider;
import com.ford.oa.managers.ObjectGraphManager;
import com.ford.utils.CalendarProvider;
import com.ford.utils.providers.DateFormatProvider;
import com.ford.utils.providers.LocaleProvider;
import com.ford.wifihotspot.models.NgsdnWifiDataUsageResponse;
import com.google.gson.Gson;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Locale;

import javax.inject.Inject;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.ford.espresso.utils.ResourceUtils.getString;

public class WifiHotspotActivityTest_Canada {

    @Rule
    public FordTestConfigurationRule mFordTestConfigurationRule = new FordTestConfigurationRule(true);

    @Rule
    public FordActivityTestRule<WifiHotspotActivity> mFordActivityTestRule = new FordActivityTestRule<>(WifiHotspotActivity.class);

    @Rule
    public FordMockWebServerRule mFordMockWebServerRule = new FordMockWebServerRule();

    @Inject
    LocaleProvider mLocaleProvider;

    @Inject
    ConfigurationProvider mConfigurationProvider;

    @Inject
    DateFormatProvider mDateFormatProvider;

    @Inject
    CalendarProvider mCalendarProvider;

    @Inject
    Gson mGson;

    @Inject
    NgsdnGsonProvider mNgsdnGsonProvider;

    MockLocaleProvider mMockLocaleProvider;
    MockConfigurationProvider mMockConfigurationProvider;

    @Before
    public void setUp() {

        ObjectGraphManager.inject(this);

        mMockLocaleProvider = (MockLocaleProvider) mLocaleProvider;
        mMockConfigurationProvider = (MockConfigurationProvider) mConfigurationProvider;
        mMockLocaleProvider.setLocale(Locale.CANADA);
        mMockConfigurationProvider.refreshConfiguration();
        NgsdnAccountInformationResponse accountInformationResponse = mGson.fromJson(new NgsdnAccountInformationResponseBuilder().getDefaultResponse(), NgsdnAccountInformationResponse.class);
        accountInformationResponse.getProfile().setCountry(Locale.CANADA.getISO3Country());

        mFordMockWebServerRule.getDispatcher().setAccountProfileResponse(accountInformationResponse);
    }

    @Test
    public void onUsageInfoIconClicked_paidSessionWithExpiryAndRemainingData_showsInfoDialogWithCloseAndSeeDetailsButtons() {
        launchActivityWithExtraIntent(NgsdnWifiHotspotResponseBuilder.ResponseType.LIMITED_PLAN_NORMAL_USAGE);

        onView(withId(R.id.wifi_usage_bar_info_icon)).perform(scrollTo(), click());

        verifyWifiInfoDialogShown_closeButtonDismissesDialog(getString(R.string.wifi_hotspot_data_usage_info_dialog_limited_plan_title, "10", "GB"),
                getString(R.string.wifi_hotspot_limited_data_dialog_expiry_message, "10", "GB", "02/12/18"));
    }

    @Test
    public void onLoad_limitedDataPlan_showsPlanLimitAndUsage() {
        launchActivityWithExtraIntent(NgsdnWifiHotspotResponseBuilder.ResponseType.LIMITED_PLAN_NORMAL_USAGE);

        Resources resources = mFordActivityTestRule.getActivity().getResources();
        onView(withId(R.id.activity_wifi_hotspot_data_plan_view)).check(matches(isDisplayed())).check(matches(withText(resources.getString(R.string.wifi_hotspot_data_plan, "10", "GB"))));
        onView(withId(R.id.activity_wifi_hotspot_data_plan_renewal_or_expire_details_view)).check(matches(isDisplayed())).check(matches(withText(resources.getString(R.string.wifi_hotspot_data_plan_unused_expires_on, "12/12/17"))));
        onView(withId(R.id.wifi_usage_bar_usage_text)).check(matches(isDisplayed())).check(matches(withText(resources.getString(R.string.wifi_hotspot_data_plan_usage_of_total, "2", "GB", "10", "GB"))));
        onView(withId(R.id.wifi_usage_bar_info_icon)).check(matches(isDisplayed()));
        onView(withId(R.id.wifi_usage_bar_start_text)).check(matches(CoreMatchers.allOf(isDisplayed(), withText("0GB"))));
    }

    @Test
    public void onLoad_setsStatusRefreshTimestampWithTheProperFormatting() {
        launchActivityWithExtraIntent(NgsdnWifiHotspotResponseBuilder.ResponseType.UNLIMITED_PLAN_NORMAL_USAGE);

        Resources resources = mFordActivityTestRule.getActivity().getResources();
        String statusAsOfDate = mDateFormatProvider.getDateTimeFormat("dd/MM/yy hh:mm a").format(mCalendarProvider.getInstance().getTime());
        onView(withId(R.id.activity_wifi_hotspot_status_refresh_view)).check(matches(CoreMatchers.allOf(isDisplayed(), withText(resources.getString(R.string.wifi_hotspot_data_plan_status_timestamp, statusAsOfDate)))));
    }

    @Test
    public void onLoad_unlimitedDataPlan_showsUsage() {
        launchActivityWithExtraIntent(NgsdnWifiHotspotResponseBuilder.ResponseType.UNLIMITED_PLAN_NORMAL_USAGE);

        Resources resources = mFordActivityTestRule.getActivity().getResources();
        onView(withId(R.id.activity_wifi_hotspot_data_plan_view)).check(matches(isDisplayed())).check(matches(withText(resources.getString(R.string.wifi_hotspot_data_unlimited_data))));
        onView(withId(R.id.activity_wifi_hotspot_data_plan_renewal_or_expire_details_view)).check(matches(isDisplayed())).check(matches(withText(resources.getString(R.string.wifi_hotspot_data_plan_renews_on, "02/12/18"))));
        onView(withId(R.id.wifi_unlimited_plan_widget_data_label_text)).check(matches(isDisplayed())).check(matches(withText(resources.getString(R.string.wifi_hotspot_data_unlimited_usage_of_total))));
        onView(withId(R.id.wifi_unlimited_plan_widget_data_usage_text)).check(matches(CoreMatchers.allOf(isDisplayed(), withText("5 GB"))));
    }

    @Test
    public void onLoad_paidSharedPlan_shortRenewalCycleFormat_showsUsage() {
        launchActivityWithExtraIntent(NgsdnWifiHotspotResponseBuilder.ResponseType.PAID_SHARED_PLAN_SHORT_RENEWAL_CYCLE_FORMAT);

        Resources resources = mFordActivityTestRule.getActivity().getResources();
        onView(withId(R.id.activity_wifi_hotspot_data_plan_view)).check(matches(isDisplayed())).check(matches(withText(resources.getString(R.string.wifi_hotspot_data_plan, "10", "GB"))));
        onView(withId(R.id.activity_wifi_hotspot_data_plan_renewal_or_expire_details_view)).check(matches(isDisplayed())).check(matches(withText(resources.getString(R.string.wifi_hotspot_data_plan_renews_on, "07/04/17"))));
        onView(withId(R.id.wifi_usage_bar_usage_text)).check(matches(isDisplayed())).check(matches(withText(resources.getString(R.string.wifi_hotspot_data_plan_usage_of_total, "2", "GB", "10", "GB"))));
        onView(withId(R.id.wifi_usage_bar_start_text)).check(matches(CoreMatchers.allOf(isDisplayed(), withText("0GB"))));
    }

    @Test
    public void onUsageInfoIconClicked_paidSharedWithRenewalAndAvailableData_showsInfoDialogWithCloseAndSeeDetailsButtons() {
        launchActivityWithExtraIntent(NgsdnWifiHotspotResponseBuilder.ResponseType.PAID_SHARED_PLAN_SHORT_RENEWAL_CYCLE_FORMAT);

        onView(withId(R.id.wifi_usage_bar_info_icon)).perform(scrollTo(), click());

        verifyWifiInfoDialogShown_closeButtonDismissesDialog(getString(R.string.wifi_hotspot_data_usage_info_dialog_limited_plan_title, "10", "GB"),
                getString(R.string.wifi_hotspot_limited_data_dialog_paid_shared_before_exceeded_message, "10", "GB", "07/04/17"));
    }

    @Test
    public void onUsageInfoIconClicked_trialDataPlan_showsInfoDialogWithCloseAndSeeDetailsButtons() {
        launchActivityWithExtraIntent(NgsdnWifiHotspotResponseBuilder.ResponseType.TRIAL_PLAN);

        onView(withId(R.id.wifi_usage_bar_info_icon)).perform(scrollTo(), click());

        verifyWifiInfoDialogShown_closeButtonDismissesDialog(getString(R.string.wifi_hotspot_data_usage_info_dialog_limited_plan_title, "10", "GB"),
                getString(R.string.wifi_hotspot_trial_or_shared_without_renewal_dialog_message, "10", "GB", "07/04/17"));
    }

    @Test
    public void onUsageInfoIconClicked_paidSessionWithoutRenewalDatePlan_showsInfoDialogWithCloseAndSeeDetailsButtons() {
        launchActivityWithExtraIntent(NgsdnWifiHotspotResponseBuilder.ResponseType.LIMITED_PLAN_NO_RENEWAL);

        onView(withId(R.id.wifi_usage_bar_info_icon)).perform(scrollTo(), click());

        verifyWifiInfoDialogShown_closeButtonDismissesDialog(getString(R.string.wifi_hotspot_data_usage_info_dialog_limited_plan_title, "10", "GB"),
                getString(R.string.wifi_hotspot_trial_or_shared_without_renewal_dialog_message, "10", "GB", "07/04/17"));
    }

    @Test
    public void onUsageInfoIconClicked_paidSessionWithRenewalAndNoRemainingData_showsInfoDialogWithCloseAndSeeDetailsButtons() {
        launchActivityWithExtraIntent(NgsdnWifiHotspotResponseBuilder.ResponseType.LIMITED_PLAN_OVERAGE_USAGE);

        onView(withId(R.id.wifi_usage_bar_info_icon)).perform(scrollTo(), click());

        verifyWifiInfoDialogShown_closeButtonDismissesDialog(EspressoFordApplication.getApplication().getResources().getString(R.string.wifi_hotspot_data_usage_info_dialog_limited_plan_title, "10", "GB"),
                EspressoFordApplication.getApplication().getResources().getString(R.string.wifi_hotspot_limited_data_dialog_renewal_exceeded_message, "02/12/18"));
    }

    // region Helper Methods

    private NgsdnWifiDataUsageResponse getNgsdnWifiDataUsageResponse(NgsdnWifiHotspotResponseBuilder.ResponseType responseType) {
        String wifiDataUsageResponse = new NgsdnWifiHotspotResponseBuilder().getResponse(responseType);
        return mNgsdnGsonProvider.getGson().fromJson(wifiDataUsageResponse, NgsdnWifiDataUsageResponse.class);
    }

    private void launchActivityWithExtraIntent(NgsdnWifiHotspotResponseBuilder.ResponseType responseType) {
        Intent intent = new Intent();
        intent.putExtra(WifiHotspotActivity.Extras.VEHICLE, new NgsdnVehicleImpl("vin_test"));
        NgsdnWifiDataUsageResponse ngsdnWifiDataUsageResponse = getNgsdnWifiDataUsageResponse(responseType);
        intent.putExtra(WifiHotspotActivity.Extras.WIFI_DATA_PLAN, ngsdnWifiDataUsageResponse.getWifiDataPlan());
        intent.putExtra(WifiHotspotActivity.Extras.WIFI_DATA_PLAN_USAGE, ngsdnWifiDataUsageResponse.getWifiDataPlanUsage());
        intent.putExtra(WifiHotspotActivity.Extras.WIFI_DATA_USAGE_RESPONSE_STATUS, ngsdnWifiDataUsageResponse.getStatus());
        mFordActivityTestRule.launchActivity(intent);
    }

    private void verifyWifiInfoDialogShown_closeButtonDismissesDialog(String expectedTitle, String expectedMessage) {
        onView(withId(R.id.ford_dialog_title_textview)).check(matches(withText(expectedTitle)));
        onView(withId(R.id.ford_dialog_message_textview)).check(matches(withText(expectedMessage)));
        onView(CoreMatchers.allOf(isDescendantOfA(withId(R.id.ford_dialog_button_container)), withText(R.string.dialog_close))).check(matches(isDisplayed()));
        onView(CoreMatchers.allOf(isDescendantOfA(withId(R.id.ford_dialog_button_container)), withText(R.string.dialog_see_details))).check(matches(isDisplayed()));

        onView(CoreMatchers.allOf(isDescendantOfA(withId(R.id.ford_dialog_button_container)), withText(R.string.dialog_close)))
                .perform(click());
        onView(withId(R.id.ford_dialog_message_textview)).check(doesNotExist());
    }

    // endregion
}