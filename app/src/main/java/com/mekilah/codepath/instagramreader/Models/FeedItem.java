package com.mekilah.codepath.instagramreader.Models;

import android.content.Context;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.makeramen.RoundedImageView;
import com.mekilah.codepath.instagramreader.R;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mekilah on 2/3/15.
 */
public class FeedItem{

    private String username;
    private String imageURL;
    private String caption;
    private long timestampInSecs;
    private ArrayList<InstagramComment> comments;
    private String userProfilePictureURL;
    private long likesCount;
    private long totalCommentCount;

    private final static String USERNAME_HTML_TAGS_BEGIN = "<b><font color='#1C587E'>";
    private final static String USERNAME_HTML_TAGS_END = "</b></font>";

    private final static int MAX_COMMENTS_TO_SHOW = 5;


    public FeedItem(){
        comments = new ArrayList<>();
    }

    public static FeedItem BuildFromJSON(JSONObject object){
        FeedItem item = new FeedItem();
        Log.i("INSTA", "feedItemBuild: " + object.toString());
        try{
            // see http://instagram.com/developer/endpoints/media/ for response format

            //comments are optional
            JSONObject commentsObj = object.optJSONObject(InstagramSpecifics.APIResponseKeys.COMMENTS);
            if(commentsObj != null){
                JSONArray commentsArrayObj = commentsObj.getJSONArray(InstagramSpecifics.APIResponseKeys.DATA);
                item.totalCommentCount = commentsObj.getLong(InstagramSpecifics.APIResponseKeys.COUNT); //only 8 comments sent down ever with feed items, but total count also sent
                if(item.totalCommentCount < 0){
                    item.totalCommentCount = commentsArrayObj.length(); //just in case server response is bad here
                }
                item.comments.ensureCapacity(commentsArrayObj.length());
                for(int i=0; i<commentsArrayObj.length(); i++){
                    JSONObject commentObj = commentsArrayObj.getJSONObject(i);

                    if(commentObj != null){
                        InstagramComment instagramComment = new InstagramComment();
                        instagramComment.commentText = commentObj.getString(InstagramSpecifics.APIResponseKeys.TEXT);
                        instagramComment.profilePictureURL = commentObj.getJSONObject(InstagramSpecifics.APIResponseKeys.FROM).getString(InstagramSpecifics.APIResponseKeys.PROFILE_PICTURE);
                        instagramComment.username = commentObj.getJSONObject(InstagramSpecifics.APIResponseKeys.FROM).getString(InstagramSpecifics.APIResponseKeys.USERNAME);
                        item.comments.add(instagramComment);
                    }
                }
            }

            item.timestampInSecs = object.getLong(InstagramSpecifics.APIResponseKeys.TIMESTAMP);

            //captions are optional
            JSONObject captionObj = object.optJSONObject(InstagramSpecifics.APIResponseKeys.CAPTION);
            if(captionObj == null){
                item.caption = "";
            }else{
                item.caption = captionObj.getString(InstagramSpecifics.APIResponseKeys.TEXT);
            }

            item.imageURL = object.getJSONObject(InstagramSpecifics.APIResponseKeys.IMAGES).getJSONObject(InstagramSpecifics.APIResponseKeys.STANDARD_RES).getString(InstagramSpecifics.APIResponseKeys.URL);
            item.username = object.getJSONObject(InstagramSpecifics.APIResponseKeys.USER).getString(InstagramSpecifics.APIResponseKeys.USERNAME);
            item.userProfilePictureURL = object.getJSONObject(InstagramSpecifics.APIResponseKeys.USER).getString(InstagramSpecifics.APIResponseKeys.PROFILE_PICTURE);

            //likes are optional
            JSONObject likesObj = object.optJSONObject(InstagramSpecifics.APIResponseKeys.LIKES);
            if(likesObj != null){
                item.likesCount = likesObj.getLong(InstagramSpecifics.APIResponseKeys.COUNT);
            }else{
                item.likesCount = 0;
            }

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
            FeedItemViewTag viewTag;
            LayoutInflater layoutInflater = LayoutInflater.from(this.getContext());

            if(convertView == null){
                convertView = layoutInflater.inflate(R.layout.feed_item, parent, false);
                viewTag = new FeedItemViewTag();
                viewTag.tvUsername = (TextView) convertView.findViewById(R.id.tvFeedUsername);
                viewTag.tvTimestamp = (TextView) convertView.findViewById(R.id.tvFeedItemTimestamp);
                viewTag.tvCaption = (TextView) convertView.findViewById(R.id.tvFeedCaption);
                viewTag.ivMainImage = (ImageView) convertView.findViewById(R.id.ivFeedMainImage);
                viewTag.tvLikesCount = (TextView) convertView.findViewById(R.id.tvLikesCount);
                viewTag.rivFeedUserPicture = (RoundedImageView) convertView.findViewById(R.id.rivFeedUserImage);
                viewTag.tvCommentsCount = (TextView) convertView.findViewById(R.id.tvFeedCommentsCount);
                viewTag.llFeedComments = (LinearLayout) convertView.findViewById(R.id.llFeedComments);
                convertView.setTag(viewTag);
            }else{
                viewTag = (FeedItemViewTag) convertView.getTag();
            }

            viewTag.tvUsername.setText(item.username);
            if(item.caption.length() > 0){
                viewTag.tvCaption.setText(Html.fromHtml(FeedItem.USERNAME_HTML_TAGS_BEGIN + item.username + FeedItem.USERNAME_HTML_TAGS_END + "  " + item.caption));
            }else{
                viewTag.tvCaption.setVisibility(View.GONE);
            }
            viewTag.tvTimestamp.setText(DateUtils.getRelativeTimeSpanString(item.timestampInSecs *1000, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS));
            Picasso.with(this.getContext()).load(item.imageURL).placeholder(R.drawable.loading_animation).into(viewTag.ivMainImage);
            Picasso.with(this.getContext()).load(item.userProfilePictureURL).placeholder(R.drawable.loading_animation).into(viewTag.rivFeedUserPicture);

            viewTag.tvLikesCount.setText("♥  " + NumberFormat.getNumberInstance(convertView.getResources().getConfiguration().locale).format(item.likesCount) + " likes");

            viewTag.llFeedComments.removeAllViews();

            //comments
            int showCommentsStartingAtIndex = 0;
            if(item.totalCommentCount > FeedItem.MAX_COMMENTS_TO_SHOW){
                //show last FeedItem.MAX_COMMENTS_TO_SHOW comments only
                showCommentsStartingAtIndex = item.comments.size() - FeedItem.MAX_COMMENTS_TO_SHOW; //comments array can have less than totalCommentCount items (server doesn't send them all). this still works either way, even if our max number of comments to show is the same number of comments received but less than the total count
                viewTag.tvCommentsCount.setVisibility(View.VISIBLE);
                viewTag.tvCommentsCount.setText(NumberFormat.getNumberInstance(convertView.getResources().getConfiguration().locale).format(item.totalCommentCount - FeedItem.MAX_COMMENTS_TO_SHOW) + " other comments not shown");

            }else{
                //comments count should only show if there are more hidden
                viewTag.tvCommentsCount.setVisibility(View.GONE);
            }

            for(int i = showCommentsStartingAtIndex; i < item.comments.size(); ++i){
                View commentView = layoutInflater.inflate(R.layout.feed_item_comment, viewTag.llFeedComments, false);
                TextView textView = (TextView) commentView.findViewById(R.id.tvFeedItemComment);
                textView.setText(Html.fromHtml(FeedItem.USERNAME_HTML_TAGS_BEGIN + item.comments.get(i).username + FeedItem.USERNAME_HTML_TAGS_END + "  " + item.comments.get(i).commentText));
                viewTag.llFeedComments.addView(commentView);
            }

            return convertView;
        }

        class FeedItemViewTag{
            TextView tvUsername;
            TextView tvTimestamp;
            ImageView ivMainImage;
            TextView tvCaption;
            TextView tvLikesCount;
            RoundedImageView rivFeedUserPicture;
            TextView tvCommentsCount;
            LinearLayout llFeedComments;
        }
    }

    static class InstagramComment{
        String username;
        String profilePictureURL;
        String commentText;
    }
}
