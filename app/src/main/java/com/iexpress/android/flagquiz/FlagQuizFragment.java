package com.iexpress.android.flagquiz;

import android.animation.Animator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A placeholder fragment containing a simple view.
 */
public class FlagQuizFragment extends Fragment {
    private static final String TAG = "FlagQuiz Activity";

    private static final int FLAGS_IN_QUIZ = 3;  // for dev test, change from 10 to 3 to speed up test runs.

    private List<String> fileNameList;
    private List<String> quizCountriesList;
    private Set<String> regionsSet;
    private String correctAnswer;
    private int totalGuesses;
    private int correctAnswers;
    private int guessRows;
    private SecureRandom random;
    private Handler handler;  // to delay loading next flag
    private Animation shakeAnimation;

    private LinearLayout quizLinearLayout;
    private TextView questionNumberTextView;
    private ImageView flagImageView;
    private LinearLayout[] guessLinearLayouts;
    private TextView answerTextView;

    public FlagQuizFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_flag_quiz, container, false);

        fileNameList = new ArrayList<>();
        quizCountriesList = new ArrayList<>();
        random = new SecureRandom();
        handler = new Handler();

        shakeAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.incorrect_shake);
        shakeAnimation.setRepeatCount(3);

        quizLinearLayout = (LinearLayout) view.findViewById(R.id.quizConstraintLayout);

        questionNumberTextView = (TextView) view.findViewById(R.id.questionNumberTextView);

        flagImageView = (ImageView) view.findViewById(R.id.flagImageView);

        guessLinearLayouts = new LinearLayout[4];

        guessLinearLayouts[0] = (LinearLayout) view.findViewById(R.id.row1LinearLayout);
        guessLinearLayouts[1] = (LinearLayout) view.findViewById(R.id.row2LinearLayout);
        guessLinearLayouts[2] = (LinearLayout) view.findViewById(R.id.row3LinearLayout);
        guessLinearLayouts[3] = (LinearLayout) view.findViewById(R.id.row4LinearLayout);

        answerTextView = (TextView) view.findViewById(R.id.answerTextView);

        for (LinearLayout row : guessLinearLayouts) {
            for (int col = 0; col < row.getChildCount(); col++) {
                Button btn = (Button) row.getChildAt(col);
                btn.setOnClickListener(guesButtonListener);
            }
        }

        questionNumberTextView.setText(getString(R.string.question, 1, FLAGS_IN_QUIZ));

        return view;
    }

    public void updateGuessRows(SharedPreferences sharedPreferences) {
        String choices = sharedPreferences.getString(FlagQuiz.CHOICES, null);
        guessRows = Integer.parseInt(choices) / 2;

        // hide all guess button layouts
        for (LinearLayout layout : guessLinearLayouts) {
            layout.setVisibility(View.GONE);
        }

        // display appropriate guess button layout
        for (int row = 0; row < guessRows; row++) {
            guessLinearLayouts[row].setVisibility(View.VISIBLE);
        }
    }

    public void updateRegions(SharedPreferences sharedPreferences) {
        regionsSet = sharedPreferences.getStringSet(FlagQuiz.REGIONS, null);
    }

    public void resetQuiz() {
        AssetManager assets = getActivity().getAssets();

        try {
            for (String region : regionsSet) {
                String[] paths = assets.list(region);

                for (String path : paths) {
                    fileNameList.add(path.replace(".png", ""));
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading image file names", e);
        }

        correctAnswers = 0;
        totalGuesses = 0;
        quizCountriesList.clear();

        int flagCounter = 1;
        int numberOfFlags = fileNameList.size();

        while (flagCounter <= FLAGS_IN_QUIZ) {
            int randomIdx = random.nextInt(numberOfFlags);

            String filename = fileNameList.get(randomIdx);

            if (!quizCountriesList.contains(filename)) {
                quizCountriesList.add(filename);
                ++flagCounter;
            }
        }

        loadNextFlag();
    }

    private View.OnClickListener guesButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Button guessBtn = ((Button) v);

            String guess = guessBtn.getText().toString();
            String answer = getCountryName(correctAnswer);

            ++totalGuesses;

            if (answer.contains(guess)) {
                ++correctAnswers;

                // display correct answer in green text
                answerTextView.setText(answer + "!");
                answerTextView.setTextColor(
                        getResources().getColor(R.color.correct_answer));

                disableButtons();

                if (correctAnswers == FLAGS_IN_QUIZ) {

                    AlertDialog.Builder build = new AlertDialog.Builder(getContext());

                    build.setMessage(getString(R.string.results, totalGuesses, (1000 / (double) totalGuesses)));


                    build.setMessage(R.string.reset_quiz).setPositiveButton("Rest Quiz?", dialogClickListener)
                            .setNegativeButton("No", dialogClickListener).show();


                } else {
                    handler.postDelayed( new Runnable() {
                        @Override
                        public void run() {
                                    animate(true);
                            }
                        }, 2000);
                }
            } else {
                // answer incorrect
                flagImageView.startAnimation(shakeAnimation);

                answerTextView.setText(R.string.incorrect_answer);
                answerTextView.setTextColor(getResources().getColor(R.color.incorrect_answer));

                guessBtn.setEnabled(false);
            }

        }
    };

    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    Toast.makeText(getActivity(), getString(R.string.reset_quiz), Toast.LENGTH_SHORT).show();
                    resetQuiz();
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    Toast.makeText(getActivity(), "Game completed; select setting and back to restart game."
                                        + getString(R.string.reset_quiz), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private void animate(boolean animateOut) {
        if (correctAnswers == 0) {
            return;
        }

        int centerX = (quizLinearLayout.getLeft() + quizLinearLayout.getRight()) / 2;
        int centerY = (quizLinearLayout.getTop() + quizLinearLayout.getBottom()) / 2;

        int radius = Math.max(quizLinearLayout.getWidth(), quizLinearLayout.getHeight());

        Animator animator;

        if (animateOut) {
            animator = ViewAnimationUtils.createCircularReveal(
                            quizLinearLayout, centerX, centerY, radius, 0);

            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    loadNextFlag();
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        } else {
            animator = ViewAnimationUtils.createCircularReveal(
                            quizLinearLayout, centerX, centerY, 0, radius);
        }

        animator.setDuration(250);
        animator.start();

    }

    private void disableButtons() {
        for (int row = 0; row < guessRows; row++) {
            LinearLayout guessRow = guessLinearLayouts[row];

            for (int i = 0; i < guessRow.getChildCount(); i++) {
                guessRow.getChildAt(i).setEnabled(false);
            }
        }
    }

    // parses the country flag file name and returns the country name
    private String getCountryName(String name) {
        return name.substring(name.indexOf('-') + 1).replace('_', ' ');
    }


    // after the user guesses a correct flag, load the next flag
    private void loadNextFlag() {
        // get file name of the next flag and remove it from the list
        String nextImage = quizCountriesList.remove(0);
        correctAnswer = nextImage; // update the correct answer
        answerTextView.setText(""); // clear answerTextView

        // display current question number
        questionNumberTextView.setText(getString(
                R.string.question, (correctAnswers + 1), FLAGS_IN_QUIZ));

        // extract the region from the next image's name
        String region = nextImage.substring(0, nextImage.indexOf('-'));

        // use AssetManager to load next image from assets folder
        AssetManager assets = getActivity().getAssets();

        // get an InputStream to the asset representing the next flag
        // and try to use the InputStream
        try (InputStream stream = assets.open(region + "/" + nextImage + ".png")) {
            // load the asset as a Drawable and display on the flagImageView
            Drawable flag = Drawable.createFromStream(stream, nextImage);
            flagImageView.setImageDrawable(flag);

            animate(false); // animate the flag onto the screen
        }
        catch (IOException exception) {
            Log.e(TAG, "Error loading " + nextImage, exception);
        }

        Collections.shuffle(fileNameList); // shuffle file names

        // put the correct answer at the end of fileNameList
        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));

        // add 2, 4, 6 or 8 guess Buttons based on the value of guessRows
        for (int row = 0; row < guessRows; row++) {
            // place Buttons in currentTableRow
            for (int column = 0; column < guessLinearLayouts[row].getChildCount(); column++) {
                // get reference to Button to configure
                Button newGuessButton = (Button) guessLinearLayouts[row].getChildAt(column);
                newGuessButton.setEnabled(true);

                // get country name and set it as newGuessButton's text
                String filename = fileNameList.get((row * 2) + column);
                newGuessButton.setText(getCountryName(filename));
            }
        }

        // randomly replace one Button with the correct answer
        int row = random.nextInt(guessRows); // pick random row
        int column = random.nextInt(2); // pick random column
        LinearLayout randomRow = guessLinearLayouts[row]; // get the row
        ((Button) randomRow.getChildAt(column)).setText(getCountryName(correctAnswer));
    }

}
