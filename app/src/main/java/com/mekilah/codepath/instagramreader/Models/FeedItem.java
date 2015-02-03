package com.mekilah.codepath.instagramreader.Models;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.mekilah.codepath.instagramreader.R;

import org.json.JSONObject;

import java.util.Calendar;

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
        //TODO traverse api response and build object
        FeedItem item = new FeedItem();

        return item;
    }

    public class FeedItemAdapter extends ArrayAdapter<FeedItem>{

        public FeedItemAdapter(Context context, int resource){
            super(context, 0, resource);
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
            viewTag.tvTimestamp.setText(DateUtils.getRelativeTimeSpanString(item.timestamp, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS));
            //TODO fetch image and store it

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
