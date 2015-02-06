package com.mekilah.codepath.instagramreader;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpRequest;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.mekilah.codepath.instagramreader.Models.FeedItem;
import com.mekilah.codepath.instagramreader.Models.InstagramSpecifics;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;


public class FeedActivity extends ActionBarActivity {

    ArrayList<FeedItem> feedItems;
    FeedItem.FeedItemAdapter feedItemAdapter;

    ListView lvFeedItems;
    PullToRefreshLayout ptrFeedItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);
        feedItems = new ArrayList<FeedItem>();
        feedItemAdapter = new FeedItem.FeedItemAdapter(this, feedItems);

        lvFeedItems = (ListView) this.findViewById(R.id.lvFeedItems);
        lvFeedItems.setAdapter(feedItemAdapter);

        ptrFeedItems = (PullToRefreshLayout) this.findViewById(R.id.ptrFeedItems);
        // Now setup the PullToRefreshLayout
        ActionBarPullToRefresh.from(this)
                // Mark All Children as pullable
                .allChildrenArePullable()
                        // Set a OnRefreshListener
                .listener(new OnRefreshListener(){
                    @Override
                    public void onRefreshStarted(View view){
                        asyncRefresh();
                    }
                })
        // Finally commit the setup to our PullToRefreshLayout
        .setup(ptrFeedItems);

        asyncRefresh();


    }

    private void asyncRefresh(){
        //GET to API
        feedItems.clear();
        AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
        asyncHttpClient.get(InstagramSpecifics.APIURLs.PUBLIC_POPULAR + InstagramSpecifics.APIRequestParameters.CLIENT_ID + InstagramSpecifics.APITokens.CLIENT_ID, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response){
                try{
                    JSONArray dataArray = response.getJSONArray(InstagramSpecifics.APIResponseKeys.DATA);
                    for(int i=0; i < dataArray.length(); ++i){
                        JSONObject feedObj = (JSONObject)dataArray.get(i);
                        FeedItem item  = FeedItem.BuildFromJSON(feedObj);
                        if(item != null){
                            feedItems.add(item);
                        }
                    }
                }catch(JSONException e){
                    Log.e("INSTA", "error while decoding JSON response.", e);
                }

                feedItemAdapter.notifyDataSetChanged();
                ptrFeedItems.setRefreshComplete();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable){
                Log.e("INSTA", "Failure requesting from Instagram. code=" + statusCode + ", response=" + responseString, throwable);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_feed, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
