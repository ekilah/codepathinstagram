package com.mekilah.codepath.instagramreader.Models;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.graphics.Color;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.Toast;


import com.makeramen.RoundedImageView;
import com.mekilah.codepath.instagramreader.R;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.net.URI;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            JSONArray tagsObj = object.optJSONArray(InstagramSpecifics.APIResponseKeys.HASHTAGS);
            if(tagsObj != null  && tagsObj.length() > 0){
                for(int i = 0; i < tagsObj.length(); ++i){
                    if(tagsObj.getString(i) == null){
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
                viewTag.rlTopBar = (RelativeLayout) convertView.findViewById(R.id.rlTopBar);
                convertView.setTag(viewTag);
            }else{
                viewTag = (FeedItemViewTag) convertView.getTag();
            }

            SpannableStringBuilder stringBuilder = new SpannableStringBuilder();

            viewTag.tvUsername.setText(FeedItem.getSpannableStringForUsername(item.username));
            viewTag.tvUsername.setMovementMethod(LinkMovementMethod.getInstance());
            viewTag.tvUsername.setHighlightColor(Color.TRANSPARENT);

            stringBuilder.clearSpans();
            stringBuilder.clear();
            if(item.caption.length() > 0){
                //viewTag.tvCaption.setText(Html.fromHtml(FeedItem.USERNAME_HTML_TAGS_BEGIN + item.username + FeedItem.USERNAME_HTML_TAGS_END + "  " + FeedItem.getHTMLStringForTextWithHashtags(item.caption, item.hashtags)));
                stringBuilder.append(FeedItem.getSpannableStringForUsername(item.username));
                stringBuilder.append(" ");
                stringBuilder.append(FeedItem.getSpannableStringForHashtagsAndUsernames(item.caption));
                viewTag.tvCaption.setText(stringBuilder);
                viewTag.tvCaption.setMovementMethod(LinkMovementMethod.getInstance());
                viewTag.tvCaption.setHighlightColor(Color.TRANSPARENT);
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

            viewTag.tvLikesCount.setText("♥  " + NumberFormat.getNumberInstance(convertView.getResources().getConfiguration().locale).format(item.likesCount) + " " + this.getContext().getText(R.string.likes_lowercase));

            viewTag.llFeedComments.removeAllViews();

            //comments
            int showCommentsStartingAtIndex = 0;
            if(item.totalCommentCount > FeedItem.MAX_COMMENTS_TO_SHOW){
                //show last FeedItem.MAX_COMMENTS_TO_SHOW comments only
                showCommentsStartingAtIndex = item.comments.size() - FeedItem.MAX_COMMENTS_TO_SHOW; //comments array can have less than totalCommentCount items (server doesn't send them all). this still works either way, even if our max number of comments to show is the same number of comments received but less than the total count
                viewTag.tvCommentsCount.setVisibility(View.VISIBLE);
                viewTag.tvCommentsCount.setText(NumberFormat.getNumberInstance(convertView.getResources().getConfiguration().locale).format(item.totalCommentCount - FeedItem.MAX_COMMENTS_TO_SHOW) + " " + this.getContext().getText(R.string.other_comments_not_shown_lowercase));

            }else{
                //comments count should only show if there are more hidden
                viewTag.tvCommentsCount.setVisibility(View.GONE);
            }

            for(int i = showCommentsStartingAtIndex; i < item.comments.size(); ++i){
                View commentView = layoutInflater.inflate(R.layout.feed_item_comment, viewTag.llFeedComments, false);
                TextView textView = (TextView) commentView.findViewById(R.id.tvFeedItemComment);
                stringBuilder.clear();
                stringBuilder.clearSpans();
                stringBuilder.append(FeedItem.getSpannableStringForUsername(item.comments.get(i).username));
                stringBuilder.append(" ");
                stringBuilder.append(FeedItem.getSpannableStringForHashtagsAndUsernames(item.comments.get(i).commentText));

                textView.setText(stringBuilder);
                textView.setMovementMethod(LinkMovementMethod.getInstance());
                textView.setHighlightColor(Color.TRANSPARENT);
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
            VideoView vvMainVideo;
            ImageView ivPlayButton;
            RelativeLayout rlTopBar;
        }
    }

    static class InstagramComment{
        String username;
        String profilePictureURL;
        String commentText;
    }


    /**
     * Searches the string for the provided hashtags, and makes the string's matching hashtags pretty in HTML
     * @param stringWithHashtags
     * @param hashtags
     * @return the formatted String
     */
    private static String getHTMLStringForTextWithHashtags(String stringWithHashtags, List<String> hashtags){
        if(hashtags != null && stringWithHashtags != null){

            //sort so that longest hashtags are first. this prevents #foobar from being messed up by #foo
            java.util.Collections.sort(hashtags, new Comparator<String>(){
                @Override
                public int compare(String lhs, String rhs){
                    return rhs.length() - lhs.length();
                }
            });
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
    private static String getHTMLStringForTextWithHashtags(String stringWithHashtags){
        if(stringWithHashtags == null){
            return stringWithHashtags;
        }
        //regex: ignore case, search for # followed by a word character, followed by 0 or more word or digit characters that aren't spaces or another #, and be greedy
        stringWithHashtags = stringWithHashtags.replaceAll("(?i)#\\w[[\\d\\w]&&[^#\\s]]*", HASHTAG_HTML_TAGS_BEGIN + "$0" + HASHTAG_HTML_TAGS_END);
        return stringWithHashtags;
    }

    public static CharSequence getSpannableStringForHashtagsAndUsernames(String input){
        if(input == null || input.length() <= 0){
            return input;
        }

        SpannableStringBuilder stringBuilder = new SpannableStringBuilder(input);

        Pattern pattern = Pattern.compile("(?i)([#@])(\\w[[\\d\\w]&&[^#@\\s]]*)");
        Matcher matcher = pattern.matcher(input);

        while(matcher.find()){
            ClickableSpan span;
            if(matcher.group(1).equals("#")){
                span = new HashtagSpan();
            }else{
                span = new UsernameSpan();
            }

            stringBuilder.setSpan(span, matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return stringBuilder;
    }

    public static CharSequence getSpannableStringForUsername(String input){
        if(input == null || input.length() <= 0){
            return input;
        }

        SpannableStringBuilder stringBuilder = new SpannableStringBuilder(input);
        ClickableSpan span = new UsernameSpan(true);

        stringBuilder.setSpan(span, 0, input.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return stringBuilder;
    }

    static class HashtagSpan extends ClickableSpan{

        @Override
        public void onClick(View widget){
            Log.i("INSTA", "Hashtag clicked");
            TextView tv = (TextView) widget;
            Spanned spanned = (Spanned) tv.getText();

            Toast.makeText(widget.getContext(), "Hashtag: " + spanned.subSequence(spanned.getSpanStart(this), spanned.getSpanEnd(this)), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void updateDrawState(TextPaint ds){
            ds.setUnderlineText(false);
            ds.setColor(Color.parseColor("#1C587E"));
        }
    }

    static class UsernameSpan extends ClickableSpan{
        private boolean bold;
        public UsernameSpan(boolean bold){
            this.bold = bold;
        }
        public UsernameSpan(){
            this.bold = false;
        }

        @Override
        public void onClick(View widget){
            Log.i("INSTA", "username clicked");
            TextView tv = (TextView) widget;
            Spanned spanned = (Spanned) tv.getText();

            Toast.makeText(widget.getContext(), "Username: " + spanned.subSequence(spanned.getSpanStart(this), spanned.getSpanEnd(this)), Toast.LENGTH_SHORT).show();

        }

        @Override
        public void updateDrawState(TextPaint ds){
            ds.setUnderlineText(false);
            ds.setColor(Color.parseColor("#1C587E"));
            ds.setFakeBoldText(this.bold);
        }
    }
}
