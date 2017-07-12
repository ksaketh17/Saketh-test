
package com.ford.espresso.activities.myvehicles.wifihotspot.activities;

import android.content.Intent;
import android.support.test.espresso.intent.Intents;

import com.ford.espresso.EspressoFordModule;
import com.ford.espresso.builders.NgsdnWifiHotspotResponseBuilder;
import com.ford.espresso.utils.FordActivityTestRule;
import com.ford.espresso.utils.FordTestConfigurationRule;
import com.ford.ngsdn.models.NgsdnVehicleImpl;
import com.ford.ngsdn.providers.NgsdnGsonProvider;
import com.ford.oa.R;
import com.ford.oa.managers.ObjectGraphManager;
import com.ford.oa.myvehicles.wifihotspot.activities.WifiHotspotActivity;
import com.ford.utils.CalendarProvider;
import com.ford.utils.providers.DateFormatProvider;
import com.ford.wifihotspot.models.NgsdnWifiDataUsageResponse;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Calendar;

import javax.inject.Inject;

import dagger.Module;
import dagger.Provides;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasData;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WifiHotspotActivityTest {

    private static final long TIMESTAMP = 1497634929000L; //06/16/2017 (UTC)

    @Module(includes = EspressoFordModule.class, overrides = true)
    class OverrideModule {
        @Provides
        CalendarProvider provideCalendarProvider() {
            return mMockCalendarProvider;
        }
    }

    @Rule
    public FordTestConfigurationRule mFordTestConfigurationRule = new FordTestConfigurationRule(new OverrideModule(), true);

    @Rule
    public FordActivityTestRule<WifiHotspotActivity> mFordActivityTestRule = new FordActivityTestRule<>(WifiHotspotActivity.class);

    @Inject
    DateFormatProvider mDateFormatProvider;

    @Inject
    NgsdnGsonProvider mNgsdnGsonProvider;

    private CalendarProvider mMockCalendarProvider;

    @Before
    public void setup() {
        ObjectGraphManager.inject(this);
        Intents.init();

        mMockCalendarProvider = mock(CalendarProvider.class);
        Calendar mockCalendar = mock(Calendar.class);
        when(mMockCalendarProvider.getInstance()).thenReturn(mockCalendar);
        when(mockCalendar.getTimeInMillis()).thenReturn(TIMESTAMP);
    }

    @After
    public void teardown() {
        Intents.release();
    }

    @Test
    public void onContactCarrierClicked_launchesDialerWithUnicomHotline() {
        launchActivityWithExtraIntent(NgsdnWifiHotspotResponseBuilder.ResponseType.LIMITED_PLAN_NORMAL_USAGE);

        onView(withId(R.id.activity_wifi_hotspot_contact_carrier)).perform(scrollTo(), click());
        Intents.intended(CoreMatchers.allOf(hasAction(Intent.ACTION_DIAL), hasData("tel:400-092-0198")));
    }

    //region Helper methods

    private void launchActivityWithExtraIntent(NgsdnWifiHotspotResponseBuilder.ResponseType responseType) {
        Intent intent = new Intent();
        intent.putExtra(WifiHotspotActivity.Extras.VEHICLE, new NgsdnVehicleImpl("vin_test"));
        NgsdnWifiDataUsageResponse ngsdnWifiDataUsageResponse = getNgsdnWifiDataUsageResponse(responseType);
        intent.putExtra(WifiHotspotActivity.Extras.WIFI_DATA_PLAN, ngsdnWifiDataUsageResponse.getWifiDataPlan());
        intent.putExtra(WifiHotspotActivity.Extras.WIFI_DATA_PLAN_USAGE, ngsdnWifiDataUsageResponse.getWifiDataPlanUsage());
        intent.putExtra(WifiHotspotActivity.Extras.WIFI_DATA_USAGE_RESPONSE_STATUS, ngsdnWifiDataUsageResponse.getStatus());
        mFordActivityTestRule.launchActivity(intent);
    }

    private NgsdnWifiDataUsageResponse getNgsdnWifiDataUsageResponse(NgsdnWifiHotspotResponseBuilder.ResponseType responseType) {
        String wifiDataUsageResponse = new NgsdnWifiHotspotResponseBuilder().getResponse(responseType);
        return mNgsdnGsonProvider.getGson().fromJson(wifiDataUsageResponse, NgsdnWifiDataUsageResponse.class);
    }

    //endregion
}
