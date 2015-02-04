package com.mekilah.codepath.instagramreader.Models;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.mekilah.codepath.instagramreader.R;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.List;

/**
 * Created by mekilah on 2/3/15.
 */
public class FeedItem{

    private String username;
    private String imageURL;
    private String caption;
    private long timestamp;

    public FeedItem(String username, String imageURL, String caption, long timestamp){
        this.username = username;
        this.imageURL = imageURL;
        this.caption = caption;
        this.timestamp = timestamp;
    }

    public FeedItem(){}

    public static FeedItem BuildFromJSON(JSONObject object){
        FeedItem item = new FeedItem();
        Log.i("INSTA","feedItemBuild: " + object.toString());
        try{
            // object => {createdtime, user=> {username}, caption => {text}, images => {standard => {url}}}
            item.timestamp = object.getInt(InstagramSpecifics.APIResponseKeys.TIMESTAMP);

            //captions are optional
            JSONObject captionObj = object.getJSONObject(InstagramSpecifics.APIResponseKeys.CAPTION);
            if(captionObj == null){
                item.caption = "";
            }else{
                item.caption = captionObj.getString(InstagramSpecifics.APIResponseKeys.TEXT);
            }

            item.imageURL = object.getJSONObject(InstagramSpecifics.APIResponseKeys.IMAGES).getJSONObject(InstagramSpecifics.APIResponseKeys.STANDARD_RES).getString(InstagramSpecifics.APIResponseKeys.URL);
            item.username = object.getJSONObject(InstagramSpecifics.APIResponseKeys.USER).getString(InstagramSpecifics.APIResponseKeys.USERNAME);

        }catch(JSONException e){
            Log.e("INSTA", "error reading json of FeedItem.", e);
            return null;
        }

        return item;
    }

    public static class FeedItemAdapter extends ArrayAdapter<FeedItem>{

        public FeedItemAdapter(Context context, List<FeedItem> objects){
            super(context, 0, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){

            FeedItem item = this.getItem(position);

            if(convertView == null){
                convertView = LayoutInflater.from(this.getContext()).inflate(R.layout.feed_item, parent, false);
                FeedItemViewTag viewTag = new FeedItemViewTag();
                viewTag.tvUsername = (TextView) convertView.findViewById(R.id.tvFeedUsername);
                viewTag.tvTimestamp = (TextView) convertView.findViewById(R.id.tvFeedTimestamp);
                viewTag.tvCaption = (TextView) convertView.findViewById(R.id.tvFeedCaption);
                viewTag.ivMainImage = (ImageView) convertView.findViewById(R.id.ivFeedMainImage);
                convertView.setTag(viewTag);
            }

            FeedItemViewTag viewTag = (FeedItemViewTag) convertView.getTag();
            viewTag.tvUsername.setText(item.username);
            viewTag.tvCaption.setText(item.caption);
            viewTag.tvTimestamp.setText(DateUtils.getRelativeTimeSpanString(item.timestamp*1000, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS));
            //TODO fetch image and store it
            Picasso.with(this.getContext()).load(item.imageURL).into(viewTag.ivMainImage);

            return convertView;
        }

        class FeedItemViewTag{
            TextView tvUsername;
            TextView tvTimestamp;
            ImageView ivMainImage;
            TextView tvCaption;
        }
    }
}
