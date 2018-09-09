package com.iexpress.android.flagquiz;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Set;

public class FlagQuiz extends AppCompatActivity {

    // keys for reading data from SharedPreferences
    public static final String CHOICES = "pref_numberOfChoices";
    public static final String REGIONS = "pref_regionsToInclude";
    public static final String QUESTIONS = "pref_numberOfQuestions";

    // used to force portrait mode
    private boolean phoneDevice = true;
    private boolean preferencesChanged = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flag_quiz);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // set default values in the app's SharedPreferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // register listener for SharedPreferences changes
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        int screenSz = getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;

        if (screenSz == Configuration.SCREENLAYOUT_SIZE_LARGE
                || screenSz == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
            phoneDevice = false;
        }

        if (phoneDevice) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (preferencesChanged) {
            // default preferences already set; init Flag fragment
            FlagQuizFragment quizFragment =
                (FlagQuizFragment) getSupportFragmentManager().findFragmentById(R.id.quizFragment);

            quizFragment.updateGuessRows(PreferenceManager.getDefaultSharedPreferences(this));

            quizFragment.updateRegions(PreferenceManager.getDefaultSharedPreferences(this));

            quizFragment.updateNumberOfQuestions(PreferenceManager.getDefaultSharedPreferences(this));

            quizFragment.resetQuiz();

            preferencesChanged = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // current orientation
        int orientation = getResources().getConfiguration().orientation;

        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.menu_flag_quiz, menu);
            return true;
        }

        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent preferencesIntent = new Intent(this, SettingsActivity.class);
        startActivity(preferencesIntent);

        return super.onOptionsItemSelected(item);
    }

    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                // user changes app's preferences
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    preferencesChanged = true;

                    FlagQuizFragment quizFragment = (FlagQuizFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.quizFragment);

                    // number of choice to display changes
                    if (key.equals(CHOICES)) {
                        quizFragment.updateGuessRows(sharedPreferences);
                        quizFragment.resetQuiz();
                    } else if (key.equals(REGIONS)) {
                        Set<String> regions=
                                sharedPreferences.getStringSet(REGIONS, null);

                        if (regions != null && !regions.isEmpty()) {
                            quizFragment.updateRegions(sharedPreferences);
                            quizFragment.resetQuiz();
                        } else {
                            SharedPreferences.Editor editor = sharedPreferences.edit();

                            regions.add(getString(R.string.default_region));

                            editor.putStringSet(REGIONS, regions);

                            editor.apply();

                            Toast.makeText(FlagQuiz.this, R.string.default_region_message, Toast.LENGTH_SHORT).show();
                        }
                    } else if (key.equals(QUESTIONS)) {
                        // when questions reset, no need to reset game?  (but that is risky, because what if perference changed to FEWER questions, how game finish, never)
                        quizFragment.updateNumberOfQuestions(sharedPreferences);
                        quizFragment.resetQuiz();
                    }

                    Toast.makeText(FlagQuiz.this, R.string.restarting_quiz, Toast.LENGTH_SHORT).show();
                }
            };
}
