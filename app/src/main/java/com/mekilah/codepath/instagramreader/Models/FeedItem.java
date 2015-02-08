package com.mekilah.codepath.instagramreader.Models;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.makeramen.RoundedImageView;
import com.mekilah.codepath.instagramreader.R;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Created by mekilah on 2/3/15.
 */
public class FeedItem{

    private String username;
    private String mediaURL;//image or video
    private String caption;
    private long timestampInSecs;
    private ArrayList<InstagramComment> comments;
    private String userProfilePictureURL;
    private long likesCount;
    private long totalCommentCount;
    private ArrayList<String> hashtags;
    private enum MediaType {video, image};
    private MediaType mediaType;

    private final static String USERNAME_HTML_TAGS_BEGIN = "<b><font color='#1C587E'>";
    private final static String USERNAME_HTML_TAGS_END = "</b></font>";

    private final static String HASHTAG_HTML_TAGS_BEGIN = "<font color='#0077CC'>";
    private final static String HASHTAG_HTML_TAGS_END = "</font>";

    private final static int MAX_COMMENTS_TO_SHOW = 5;

    public FeedItem(){
        comments = new ArrayList<InstagramComment>();
        hashtags = new ArrayList<String>();
    }

    public static FeedItem BuildFromJSON(JSONObject object){
        FeedItem item = new FeedItem();
        Log.i("INSTA", "feedItemBuild: " + object.toString());
        try{
            // see http://instagram.com/developer/endpoints/media/ for response format

            //hashtags might be optional
            JSONArray tagsObj = object.optJSONArray(InstagramSpecifics.APIResponseKeys.HASHTAGS);Log.i("INSTA", "Tags: " + tagsObj.toString());
            if(tagsObj != null  && tagsObj.length() > 0){
                for(int i = 0; i < tagsObj.length(); ++i){
                    if(tagsObj.getString(i) == null){
                        Log.w("INSTA", "Tag index " + i + " null.");
                        continue;
                    }
                    item.hashtags.add(tagsObj.getString(i));
                }
            }

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

            //image or video?
            item.mediaType = (object.getString(InstagramSpecifics.APIResponseKeys.TYPE).equalsIgnoreCase(InstagramSpecifics.APIResponseKeys.TYPE_VIDEO) ? MediaType.video : MediaType.image);
            if(item.mediaType == MediaType.image){
                item.mediaURL = object.getJSONObject(InstagramSpecifics.APIResponseKeys.IMAGES).getJSONObject(InstagramSpecifics.APIResponseKeys.STANDARD_RES).getString(InstagramSpecifics.APIResponseKeys.URL);
            }else{
                item.mediaURL = object.getJSONObject(InstagramSpecifics.APIResponseKeys.VIDEOS).getJSONObject(InstagramSpecifics.APIResponseKeys.STANDARD_RES).getString(InstagramSpecifics.APIResponseKeys.URL);
            }

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
            final FeedItemViewTag viewTag;
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
                viewTag.vvMainVideo = (VideoView) convertView.findViewById(R.id.vvFeedMainVideo);
                viewTag.ivPlayButton = (ImageView) convertView.findViewById(R.id.ivPlayButton);
                convertView.setTag(viewTag);
            }else{
                viewTag = (FeedItemViewTag) convertView.getTag();
            }

            viewTag.tvUsername.setText(item.username);
            if(item.caption.length() > 0){
                viewTag.tvCaption.setText(Html.fromHtml(FeedItem.USERNAME_HTML_TAGS_BEGIN + item.username + FeedItem.USERNAME_HTML_TAGS_END + "  " + this.getHTMLStringForTextWithHashtags(item.caption, item.hashtags)));
            }else{
                viewTag.tvCaption.setVisibility(View.GONE);
            }
            viewTag.tvTimestamp.setText(DateUtils.getRelativeTimeSpanString(item.timestampInSecs *1000, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS));

            //video or picture?
            if(item.mediaType == MediaType.image){
                Picasso.with(this.getContext()).load(item.mediaURL).placeholder(R.drawable.loading_animation).into(viewTag.ivMainImage);
                viewTag.vvMainVideo.setVisibility(View.GONE);
                viewTag.ivMainImage.setVisibility(View.VISIBLE);
                viewTag.ivPlayButton.setVisibility(View.GONE);
            }else{
                Log.w("INSTA", "setting up video.");

                viewTag.vvMainVideo.clearAnimation();
                viewTag.vvMainVideo.destroyDrawingCache();
                Picasso.with(this.getContext()).load(R.drawable.loading_animation).into(viewTag.ivMainImage);
                viewTag.vvMainVideo.setVisibility(View.VISIBLE);//hide until load
                viewTag.vvMainVideo.setZOrderMediaOverlay(true);

                viewTag.vvMainVideo.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){
                    @Override
                    public void onPrepared(MediaPlayer mp){
                        Log.w("INSTA", "video prepared, clickable");
                        viewTag.vvMainVideo.setClickable(true);
                        viewTag.ivMainImage.setVisibility(View.INVISIBLE);//invisible, not gone, so the size of the image view keeps the space for the video
                        viewTag.ivPlayButton.setVisibility(View.VISIBLE);

                        //height needs to be reset now that the view has the video to check for size with
                        ViewGroup.LayoutParams params = viewTag.vvMainVideo.getLayoutParams();
                        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                        viewTag.vvMainVideo.setLayoutParams(params);
                        //viewTag.vvMainVideo.pause();
                    }
                });

                viewTag.vvMainVideo.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
                    @Override
                    public void onCompletion(MediaPlayer mp){
                        viewTag.ivPlayButton.setVisibility(View.VISIBLE);
                    }
                });

                viewTag.vvMainVideo.setOnTouchListener(new View.OnTouchListener(){
                    @Override
                    public boolean onTouch(View v, MotionEvent event){
                        if(event.getAction() == MotionEvent.ACTION_DOWN){
                            //this allows us to actually process the UP later
                            return true;
                        }else if(event.getAction() == MotionEvent.ACTION_UP){
                            if(viewTag.vvMainVideo.isPlaying()){
                                Log.w("INSTA", "video stopping");
                                viewTag.vvMainVideo.pause();
                                viewTag.ivPlayButton.setVisibility(View.VISIBLE);
                            }else{
                                Log.w("INSTA", "video starting");
                                viewTag.vvMainVideo.start();
                                viewTag.ivPlayButton.setVisibility(View.GONE);

                            }
                            return true;
                        }
                        return false;
                    }
                });

                viewTag.vvMainVideo.setOnErrorListener(new MediaPlayer.OnErrorListener(){
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra){
                        Log.w("INSTA", "error with video");
                        return false;
                    }
                });
                viewTag.vvMainVideo.stopPlayback();
                viewTag.vvMainVideo.setVideoURI(Uri.parse(item.mediaURL));
                viewTag.vvMainVideo.requestFocus();
                //viewTag.vvMainVideo.start();
                //viewTag.vvMainVideo.setClickable(false);
            }
            
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
                textView.setText(Html.fromHtml(FeedItem.USERNAME_HTML_TAGS_BEGIN + item.comments.get(i).username + FeedItem.USERNAME_HTML_TAGS_END + "  " + this.getHTMLStringForTextWithHashtags(item.comments.get(i).commentText)));
                viewTag.llFeedComments.addView(commentView);
            }

            return convertView;
        }

        /**
         * Searches the string for the provided hashtags, and makes the string's matching hashtags pretty in HTML
         * @param stringWithHashtags
         * @param hashtags
         * @return the formatted String
         */
        private String getHTMLStringForTextWithHashtags(String stringWithHashtags, List<String> hashtags){
            if(hashtags != null && stringWithHashtags != null){
                for(String tag : hashtags){
                    String regex = "(?i)#" + tag;
                    stringWithHashtags = stringWithHashtags.replaceAll(regex, HASHTAG_HTML_TAGS_BEGIN + "$0" + HASHTAG_HTML_TAGS_END);
                }
            }
            return stringWithHashtags;
        }

        /**
         * Searches the string for hashtags without a list of hashtags to look for, and makes them pretty in HTML
         * @param stringWithHashtags
         * @return formatted String
         */
        private String getHTMLStringForTextWithHashtags(String stringWithHashtags){
            if(stringWithHashtags == null){
                return stringWithHashtags;
            }
            stringWithHashtags = stringWithHashtags.replaceAll("(?i)#\\w[[\\d\\w]&&[^#\\s]]*", HASHTAG_HTML_TAGS_BEGIN + "$0" + HASHTAG_HTML_TAGS_END);
            return stringWithHashtags;
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
            VideoView vvMainVideo;
            ImageView ivPlayButton;
        }
    }

    static class InstagramComment{
        String username;
        String profilePictureURL;
        String commentText;
    }
}
