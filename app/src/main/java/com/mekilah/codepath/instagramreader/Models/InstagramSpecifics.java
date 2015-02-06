package com.mekilah.codepath.instagramreader.Models;

/**
 * Created by mekilah on 2/3/15.
 */
public class InstagramSpecifics{

    public static class APITokens{
        public static final String CLIENT_ID = "102f53db3bbc4b5a86e2540c7c299a9a";

    }
    public static class APIResponseKeys{
        public static final String DATA =  "data";
        public static final String USERNAME =  "username";
        public static final String TIMESTAMP = "created_time";
        public static final String URL = "url";
        public static final String CAPTION = "caption";
        public static final String TEXT = "text";
        public static final String IMAGES = "images";
        public static final String STANDARD_RES = "standard_resolution";
        public static final String USER = "user";
        public static final String COMMENTS = "comments";
        public static final String FROM = "from";
        public static final String PROFILE_PICTURE = "profile_picture";
        public static final String COUNT = "count";
        public static final String LIKES = "likes";

    }

    public static class APIRequestParameters{
        public static final String CLIENT_ID = "client_id=";
    }

    public static class APIURLs{
        public static final String PUBLIC_POPULAR = "https://api.instagram.com/v1/media/popular?";

    }
}
