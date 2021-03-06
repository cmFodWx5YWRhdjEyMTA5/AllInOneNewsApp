package app.news.allinone.craftystudio.allinonenewsapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.tweetui.TweetUtils;
import com.twitter.sdk.android.tweetui.TweetView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import utils.ClickListener;
import utils.DatabaseHandlerFirebase;
import utils.NewsInfo;
import utils.NewsMetaInfo;
import utils.NewsSourceList;
import utils.NewsSourcesRecyclerAdapter;
import utils.RecyclerTouchListener;
import utils.UrlShortner;

public class NewsFeedActivity extends AppCompatActivity {
    public NewsMetaInfo newsMetaInfo = new NewsMetaInfo();
    public NewsInfo newsInfo = null;
    NewsSourcesRecyclerAdapter newsSourcesRecyclerAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_feed);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onShareClick();
            }
        });

        resolveIntent();


    }

    private void resolveIntent() {

        Intent intent = getIntent();
        boolean isDynamicLink = intent.getBooleanExtra("ByLink", false);
        newsMetaInfo.setNewsPushKeyId(intent.getStringExtra("PushKeyId"));
        if (isDynamicLink) {
            fetchNewsInfo(false, true);
            //fetchNewsMetaInfo(newsMetaInfo.getNewsPushKeyId());
        } else {
            newsMetaInfo.setNewsHeading(intent.getStringExtra("NewsHeading"));
            newsMetaInfo.setNewsPushKeyId(intent.getStringExtra("PushKeyId"));

            newsMetaInfo.setNewsImageLocalPath(intent.getStringExtra("NewsImageLocalPath"));
            newsMetaInfo.setNewsTime(intent.getLongExtra("NewsTime", 0L));
            newsMetaInfo.setNewsSourceimageIndex(intent.getIntExtra("NewsSourceIndex", 0));

            if (!newsMetaInfo.resolvenewsLocalImage()) {
                fetchNewsInfo(false, false);
            }

            fetchNewsInfo(true, false);

            TextView textView = (TextView) findViewById(R.id.newsFeed_newsHeading_textView);
            textView.setText(newsMetaInfo.getNewsHeading());

            ImageView imageView = (ImageView) findViewById(R.id.newsFeed_newsImage_ImageView);
            imageView.setImageBitmap(newsMetaInfo.getNewsImage());

            //imageView = (ImageView) findViewById(R.id.newsFeed_newsSourceImage_ImageView);
            //imageView.setImageDrawable(NewsSourceList.resolveIconImage(this, newsMetaInfo.getNewsSourceimageIndex()));

        }

    }

    private void fetchNewsInfo(boolean isfetchImage, boolean isFetchNewsMetaInfo) {
        DatabaseHandlerFirebase databaseHandlerFirebase = new DatabaseHandlerFirebase();
        databaseHandlerFirebase.addNewsListListner(new DatabaseHandlerFirebase.DataBaseHandlerNewsListListner() {
            @Override
            public void onNewsList(ArrayList<NewsMetaInfo> newsMetaInfoArrayList) {
                newsMetaInfo = newsMetaInfoArrayList.get(0);
                TextView textView = (TextView) findViewById(R.id.newsFeed_newsHeading_textView);
                textView.setText(newsMetaInfo.getNewsHeading());


            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onNoticePost(boolean isSuccessful) {

            }

            @Override
            public void onNewsImageFetched(boolean isFetchedImage) {

                ImageView imageView = (ImageView) findViewById(R.id.newsFeed_newsImage_ImageView);
                imageView.setImageBitmap(newsMetaInfo.getNewsImage());


            }

            @Override
            public void onNewsInfo(NewsInfo newsInfo) {

                NewsFeedActivity.this.newsInfo = newsInfo;
                initializeActivity();
            }

            @Override
            public void ongetNewsListMore(ArrayList<NewsMetaInfo> newsMetaInfoArrayListMore) {

            }
        });
        databaseHandlerFirebase.getNewsInfo(newsMetaInfo.getNewsPushKeyId());
        if (isfetchImage) {
            databaseHandlerFirebase.downloadImageFromFireBase(newsMetaInfo);
        }
        if (isFetchNewsMetaInfo) {
            databaseHandlerFirebase.getNewsMetaInfo(newsMetaInfo.getNewsPushKeyId());
        }
    }

    private void initializeActivity() {
        TextView textView = (TextView) findViewById(R.id.newsFeed_newsHeading_textView);
        textView.setText(newsInfo.getNewsHeadline());
        textView = (TextView) findViewById(R.id.newsFeed_newsSummary_textView);
        textView.setText(newsInfo.getNewsSummary());
        textView = (TextView) findViewById(R.id.newsFeed_newsSource_textView);
        //textView.setText(newsInfo.getNewsSource());
        textView.setText(newsInfo.getNewsSource());

        textView = (TextView) findViewById(R.id.newsFeed_newsDate_textView);
        textView.setText(NewsInfo.resolveDateString(newsMetaInfo.getNewsTime()));

        textView = (TextView) findViewById(R.id.newsFeed_newsSourceshort_textView);
        textView.setText(newsInfo.getNewsSourceShort());

        initializeRecyclerView();

        initializeTweets();

    }

    private void initializeRecyclerView() {
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.newsFeed_newsSourceList_recyclerView);
        newsInfo.resolveHashmap();
        newsSourcesRecyclerAdapter = new NewsSourcesRecyclerAdapter(newsInfo.getNewsSourceListArrayList(), this);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        //recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        recyclerView.setAdapter(newsSourcesRecyclerAdapter);

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                //Toast.makeText(NewsFeedActivity.this, "Item clicked = " + position, Toast.LENGTH_SHORT).show();
                openNewsIndex(position);

            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));


    }

    private void openNewsIndex(int position) {

        /*boolean currentValue = newsInfo.getNewsSourceListArrayList().get(position).isExpanded();

        for (NewsSourceList newsSourceList : newsInfo.getNewsSourceListArrayList()){
            newsSourceList.setExpanded(false);
        }

        if (currentValue) {
            newsInfo.getNewsSourceListArrayList().get(position).setExpanded(false);
        } else {
            newsInfo.getNewsSourceListArrayList().get(position).setExpanded(true);
        }


        newsSourcesRecyclerAdapter.notifyDataSetChanged();
*/

        Intent intent = new Intent(this, NewsSourceFeedActivity.class);
        intent.putExtra("NewsHeading", newsInfo.getNewsSourceListArrayList().get(position).getNewsListHeading());
        intent.putExtra("NewsArticle", newsInfo.getNewsSourceListArrayList().get(position).getNewsListArticle());
        intent.putExtra("NewsSourceIndex", newsInfo.getNewsSourceListArrayList().get(position).getSourceIndex());
        intent.putExtra("NewsImageLocalPath", newsMetaInfo.getNewsImageLocalPath());
        intent.putExtra("NewsTime", newsMetaInfo.getNewsTime());
        intent.putExtra("NewsSourceShort", newsInfo.getNewsSourceListArrayList().get(position).getNewsSourceShort());
        intent.putExtra("NewsSource", newsInfo.getNewsSourceListArrayList().get(position).getNewsListSource());


        startActivity(intent);

    }


    public void onShareClick() {

        String appCode = getString(R.string.app_code);
        String appName = getString(R.string.app_name);
        String packageName = getString(R.string.package_name);

        String utmSource = getString(R.string.utm_source);
        String utmCampaign = getString(R.string.utm_campaign);
        String utmMedium = getString(R.string.utm_medium);

        String url = "https://" + appCode + ".app.goo.gl/?link=https://AllInOneNews.com/"
                + newsMetaInfo.getNewsPushKeyId()
                + "&apn=" +
                packageName + "&st=" +
                newsInfo.getNewsHeadline()
                + "&sd=" +
                appName + "&utm_source=" +
                utmSource + "&utm_medium=" +
                utmMedium + "&utm_campaign=" +
                utmCampaign;

        /*String url = "https://" + appCode + ".app.goo.gl/?link=https://AllInOneNews.com/"
                + newsMetaInfo.getNewsPushKeyId()
                + "&apn=" +
                packageName;
       */ // Toast.makeText(this, "Shared an article " + url, Toast.LENGTH_SHORT).show();

        url = url.replaceAll(" ", "+");
        url = url.replace("\n" ,"");

       /* Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");

        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Download the app and Start reading");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, url + " \nRead More ");
        startActivity(Intent.createChooser(sharingIntent, "Share Editorial via"));
        */

        final ProgressDialog pd = new ProgressDialog(NewsFeedActivity.this);
        pd.setMessage("Creating link ...");
        pd.show();

        new UrlShortner(url, new UrlShortner.UrlShortnerListner() {


            @Override
            public void onCancel(String longUrl) {
                pd.dismiss();
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");

                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Download the app and Start reading");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, longUrl + " \nRead More ");
                startActivity(Intent.createChooser(sharingIntent, "Share News via"));


            }

            @Override
            public void onUrlShort(String shortUrl, String longUrl) {
                pd.dismiss();
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");

                //sharingIntent.putExtra(Intent.EXTRA_STREAM, newsMetaInfo.getNewsImageLocalPath());

                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Download the app and Start reading");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shortUrl + " \nRead More ");
                startActivity(Intent.createChooser(sharingIntent, "Share News via"));

            }
        }).execute("");

    }


    public void initializeTweets() {
        final LinearLayout myLayout
                = (LinearLayout) findViewById(R.id.my_tweet_layout);


        // TODO: Use a more specific parent

        // TODO: Base this Tweet ID on some data from elsewhere in your app


        if (newsInfo.getNewsTweetListHashMap() != null) {
            for (Long tweetId : newsInfo.getNewsTweetListHashMap().values()) {


                TweetUtils.loadTweet(tweetId, new Callback<Tweet>() {
                    @Override
                    public void success(Result<Tweet> result) {
                        TweetView tweetView = new TweetView(NewsFeedActivity.this, result.data);

                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

                        layoutParams.setMargins(16, 8, 16, 8);


                        myLayout.addView(tweetView, layoutParams);
                    }

                    @Override
                    public void failure(TwitterException exception) {
                        Log.d("TwitterKit", "Load Tweet failure", exception);
                    }
                });
            }
        }


    }



}