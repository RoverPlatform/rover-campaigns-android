package io.rover;


import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import io.rover.model.Action;
import io.rover.model.Block;
import io.rover.model.BlockPressEvent;
import io.rover.model.Experience;
import io.rover.model.ExperienceDismissEvent;
import io.rover.model.ExperienceLaunchEvent;
import io.rover.model.Screen;
import io.rover.model.ScreenViewEvent;
import io.rover.network.JsonResponseHandler;
import io.rover.network.NetworkTask;
import io.rover.ui.ScreenFragment;

/**
 * Created by ata_n on 2016-08-15.
 */
public class ExperienceActivity extends AppCompatActivity implements ScreenFragment.OnBlockListener {

    private static String EXPERIENCE_STATE_KEY = "EXPERIENCE_STATE_KEY";

    private RelativeLayout mLayout;
    private FetchExperienceTask mFetchTask;
    private Experience mExperience;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        RelativeLayout layout = new RelativeLayout(this);
        layout.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        layout.setId(R.id.screen_layout);

        mLayout = layout;

        setContentView(layout);

        if (savedInstanceState == null) {
            Uri data = getIntent().getData();
            if (data != null) {
                String experienceId = data.getPath();
                if (experienceId != null) {
                    mFetchTask = new FetchExperienceTask();
                    mFetchTask.execute(experienceId);

                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing() && mExperience != null) {
            Rover.submitEvent(new ExperienceDismissEvent(mExperience, new Date()));
        }
    }

    @Override
    public void onBlockClick(Block block, Screen screen) {
        Action action = block.getAction();
        if (action == null) {
            return;
        }

        switch (action.getType()) {
            case Action.GOTO_SCREEN_ACTION: {
                String screenId = action.getUrl();
                Screen newScreen = mExperience.getScreen(screenId);
                if (screen != null) {
                    Fragment screenFragment = ScreenFragment.newInstance(newScreen);

                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(mLayout.getId(), screenFragment)
                            .addToBackStack(null)
                            .commit();

                    Rover.submitEvent(new ScreenViewEvent(newScreen, mExperience, screen, block, new Date()));
                }
                break;
            }
            default: {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(action.getUrl()));
                startActivity(intent);
                break;
            }
        }

        Rover.submitEvent(new BlockPressEvent(block, screen, mExperience, new Date()));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getSupportFragmentManager().popBackStack();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(EXPERIENCE_STATE_KEY, mExperience);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mExperience = savedInstanceState.getParcelable(EXPERIENCE_STATE_KEY);
    }

    private class FetchExperienceTask extends AsyncTask<String, Void, Experience> implements JsonResponseHandler.JsonCompletionHandler {

        private ObjectMapper mObjectMapper;
        private Experience experience = null;

        @Override
        protected Experience doInBackground(String... params) {
            String experienceId = params[0];
            if (experienceId == null) {
                return null;
            }

            mObjectMapper = new ObjectMapper();

            JsonResponseHandler responseHandler = new JsonResponseHandler();
            responseHandler.setCompletionHandler(this);

            NetworkTask networkTask = Router.getExperienceNetworkTask(experienceId);
            networkTask.setResponseHandler(responseHandler);

            networkTask.run();

            return experience;
        }


        @Override
        public void onReceivedJSONObject(JSONObject jsonObject) {
            try {
                JSONObject data = jsonObject.getJSONObject("data");
                experience = (Experience) mObjectMapper.getObject("experiences", data.getString("id"),
                        data.getJSONObject("attributes"));
            } catch (JSONException e) {
                Log.e("ExperienceActivity", "Error downloading experience");
            }
        }

        @Override
        public void onReceivedJSONArray(JSONArray jsonArray) {}

        @Override
        protected void onPostExecute(Experience experience) {
            if (experience == null) { return; }

            mExperience = experience;

            Rover.submitEvent(new ExperienceLaunchEvent(experience, new Date()));

            Screen homeScreen = experience.getHomeScreen();
            if (homeScreen != null) {

                Fragment screenFragment = ScreenFragment.newInstance(homeScreen);

                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(mLayout.getId(), screenFragment, "SCREEN")
                        .commit();

                Rover.submitEvent(new ScreenViewEvent(homeScreen, experience, null, null, new Date()));
            }
        }
    }
}
