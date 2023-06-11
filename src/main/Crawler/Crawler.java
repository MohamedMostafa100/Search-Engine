import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import org.jsoup.*;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import crawlercommons.filters.basic.BasicURLNormalizer;

public class Crawler implements Runnable {

    private final String SEED_URLS = "seed_urls.txt";
    private final static int THREADS_COUNT = 10;
    private final static int NUM_OF_PAGES_TO_CRAWL=6000;
    private static Set<String> crawledUrlsSet = new HashSet<>();

    private static Set<String> compactStrings = new HashSet<>();
    private static Queue<String> toBeCrawledQueue = new LinkedList<String>();

    private static DbManager mongoDbManager;


    private static class DbManager {
        private MongoClient mongoClient;
        private MongoDatabase db;
        private MongoCollection<Document> toBeCrawledCollection;
        private MongoCollection<Document> crawlerCurrentState;
        
        private MongoCollection<Document> crawledUrlsCollection;



        public DbManager() {
            this.mongoClient = new MongoClient(new MongoClientURI("mongodb://localhost:27017"));
            this.db = mongoClient.getDatabase("MyGoogle");
            this.toBeCrawledCollection=db.getCollection("ToBeCrawledUrls");
            this.crawledUrlsCollection=db.getCollection("CrawledUrls");
            this.crawlerCurrentState=db.getCollection("CrawlerState");
        }

        private void restoreHistory(){
            Iterator toBeCrawledIterator = toBeCrawledCollection.find().iterator();
            while (toBeCrawledIterator.hasNext()) {
                Document document = (Document) toBeCrawledIterator.next();
                if (document.getString("url") != null)
                {
                    synchronized (toBeCrawledQueue) {
                        toBeCrawledQueue.add(document.getString("url"));
                    }
                }
            }

            Iterator crawledIterator = crawledUrlsCollection.find().iterator();
            while (crawledIterator.hasNext())
            {
                Document document = (Document) crawledIterator.next();
                synchronized (crawledUrlsSet){
                    crawledUrlsSet.add(document.getString("url"));
                }

                String content = getContent( document.getString("url"));
                synchronized (compactStrings) {
                    compactStrings.add(compactString(content));
                }
            }




        }
        private String getContent(String myUrl)
        {
            if (myUrl == null) {return null;}
            Connection urlConnection = Jsoup.connect(myUrl);
            try {
                org.jsoup.nodes.Document pageContent = urlConnection.get();
                return  pageContent.toString();
            }
            catch(HttpStatusException e){
                return null;
            }
            catch (IOException e) {
                return null;
            }
        }

        private String compactString(String pageContent)
        {
            // The MessageDigest class provides functionalities for hashing data.
            MessageDigest md5 = null;
            try {
                md5 = MessageDigest.getInstance("MD5");
                // The pageContent string is converted to a byte array using the UTF-8 encoding, and resulting hash
                // is returned as byte array
                byte[] hashed = md5.digest(pageContent.getBytes(StandardCharsets.UTF_8));

            /*
            This part iterates over each byte in the hashed array and converts each byte to a two-digit hexadecimal string
            using the String.format("%02x", b) expression.
            This ensures that each byte is represented by two characters.
             */
                StringBuilder stringBuilder = new StringBuilder();
                for (byte hashedByte: hashed){
                    stringBuilder.append(String.format("%02x", hashedByte));
                }
                return stringBuilder.toString();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return null;
            }


        }



        public int getCrawlerState() {
            Document stateDocument = crawlerCurrentState.find(new Document("_id", "state")).first();
            if (stateDocument != null) {
                return stateDocument.getInteger("state", 0);
            } else {
                return 0;
            }
        }

        public void setCrawlerState(int newState) {
            crawlerCurrentState.updateOne( new Document("_id", "state"),
                    new Document("$set", new Document("state", newState))
            );
        }

        public void initializeWithSeedUrls() {
            FindIterable<Document> documents = toBeCrawledCollection.find();
            for (Document document : documents) {
                String url = document.getString("url");
                synchronized (toBeCrawledQueue) {
                    if (!toBeCrawledQueue.contains(url)) {
                        toBeCrawledQueue.add(url);
                    }
                }
            }
        }

        public void addToCrawlingLog(String myUrl)
        {
            BasicURLNormalizer urlNormalizer = new BasicURLNormalizer();
            String normalizedUrl = urlNormalizer.filter(myUrl);
//            System.out.println("Normalized Url of " + myUrl + " is " + normalizedUrl);

            synchronized (toBeCrawledQueue){
                if (toBeCrawledQueue.contains(normalizedUrl)){
                    System.out.println(normalizedUrl + " ignored: already added to the queue");
                    return;
                }
            }

            String compactStr;
            if ( myUrl.contains("http") || myUrl.contains("https") )
            {
                String urlVariant = myUrl + "/";
                synchronized (toBeCrawledQueue){
                    synchronized (crawledUrlsSet) {
                        if (!toBeCrawledQueue.contains(myUrl) && !crawledUrlsSet.contains(myUrl) && !toBeCrawledQueue.contains(urlVariant) && !crawledUrlsSet.contains(urlVariant))
                        {
                            String content = getContent(myUrl);
                            if (content == null ) {
                                System.out.println(myUrl + " ignore: No content found");
                                return;}
                            compactStr=compactString(content);
                            if (compactStrings.contains(compactStr)){
                                System.out.println(myUrl + " ignored: Content repeated in another url");
                                return;}
                            toBeCrawledQueue.add(normalizedUrl);
                            toBeCrawledCollection.insertOne(new Document("url",normalizedUrl).append("popularity",0));
                            System.out.println("Added " + normalizedUrl + " to the crawled log");
                            synchronized (compactStrings){
                                compactStrings.add(compactStr);
                            }
                        }
                    }
                }
            }

        }

        public String getNextToBeCrawled(){
            String nextUrl;
            boolean urlExists = false;
            synchronized (toBeCrawledQueue) {
                if ( (nextUrl = toBeCrawledQueue.poll()) != null ){
                    urlExists=true;
                }
            }

            if (urlExists)
            {
                synchronized (toBeCrawledCollection) {
                    // Remove from toBeCrawled collection
                    toBeCrawledCollection.deleteOne(new Document("url", nextUrl));
                }
                crawledUrlsCollection.insertOne(new Document("url", nextUrl).append("popularity",0));
            }

            synchronized (crawledUrlsSet){
                if (urlExists) { crawledUrlsSet.add(nextUrl); }
            }

            return nextUrl;

        }


    }




    private boolean includesRobotFile(URL myUrl)
    {
        try {
            String host=myUrl.getHost();
            String protocol=myUrl.getProtocol();
            URL robotFileUrl = new URL(protocol + "://" + host + "/robots.txt");
            HttpURLConnection robotFileUrlConnection = (HttpURLConnection) robotFileUrl.openConnection();
            robotFileUrlConnection.connect();
            int robotUrlStatus = robotFileUrlConnection.getResponseCode();
            if(robotUrlStatus >= 200 && robotUrlStatus < 300){ return true; }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean urlExcludedByRobotFile(URL myUrl){
        if(!includesRobotFile(myUrl)){
            return false; //Not excluded
        }
        try {
            String myPath = myUrl.getPath();
            String host=myUrl.getHost();
            String protocol=myUrl.getProtocol();
            URL robotFileUrl = new URL(protocol + "://" + host + "/robots.txt");
            HttpURLConnection robotFileUrlConnection = (HttpURLConnection) robotFileUrl.openConnection();
            robotFileUrlConnection.setRequestMethod("GET");
            robotFileUrlConnection.connect();
            int robotUrlStatus = robotFileUrlConnection.getResponseCode();
            if (robotUrlStatus==200)
            {
                BufferedReader robotContentReader = new BufferedReader(new InputStreamReader(robotFileUrlConnection.getInputStream()));
                String robotLine;
                boolean generalUserAgentsFound = false;

                while ( (robotLine = robotContentReader.readLine()) != null)
                {
                    // If true (which means the "User-agent: *" line has been found), and the current line contains "User-agent:",
                    // It means that it has reached the section for a specific user-agent, so it breaks out of the loop.
                    // This is done to avoid checking subsequent user-agent-specific rules.
                    if (generalUserAgentsFound && robotLine.contains("User-agent:"))
                    {
                        break;
                    }
                    else if (generalUserAgentsFound && robotLine.contains("Disallow: " + myPath))
                    {
                        return true; // Excluded
                    }
                    else if (robotLine.contains("User-agent: *"))
                    {
                        generalUserAgentsFound = true;
                    }
                }
                robotContentReader.close();
                return false; // Not excluded
            }

        } catch (IOException e){
            e.printStackTrace();
        }
        return true; // Excluded
    }


    @Override
    public void run() {
        crawl();
    }

    public void crawl() {

        // Continue until there are no more links to crawl in the database or you reached the number of links to crawl
        while(true)
        {
            synchronized (crawledUrlsSet)
            {
                    if (crawledUrlsSet.size()>= NUM_OF_PAGES_TO_CRAWL) {
                        return; }
            }

            String currentUrl=mongoDbManager.getNextToBeCrawled();
            // Get next url to be crawled
            // The database and lists synchronizations are inside the functions
            synchronized (toBeCrawledQueue) {
                synchronized (crawledUrlsSet) {
                    if (toBeCrawledQueue.size() + crawledUrlsSet.size() >= NUM_OF_PAGES_TO_CRAWL)
                        if (currentUrl == null) {
                            return;
                    }
                }

            }

            String pageContent= mongoDbManager.getContent(currentUrl);
            if (pageContent == null) {continue;}

            try {
                if (urlExcludedByRobotFile(new URL(currentUrl)))
                {
                    System.out.println("The following url is excluded by robot.txt : " + currentUrl);
                    continue;
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            String compactStr = mongoDbManager.compactString(pageContent);

            synchronized (compactStrings)
            {
                compactStrings.add(compactStr);
            }


            try {
                org.jsoup.nodes.Document htmlOfUrl = Jsoup.connect(currentUrl).get();

//                // This line retrieves the title of the HTML document and splits it by the delimiter " - ".
//                // It takes the first part of the split, which is assumed to be the website name.
//                String title = htmlOfUrl.title().split(" - ")[0];

                //  Find all the anchor tags (<a>) in the HTML document that have an href attribute.
                //  Returns a collection of Element objects representing these anchor tags.
                Elements pagesInside = htmlOfUrl.select("a[href]");
                System.out.println("Found " + pagesInside.size() + " links inside the url " + currentUrl);

                // Update the rank
                mongoDbManager.crawledUrlsCollection.updateOne(new Document("url", currentUrl), new Document("$set", new Document("popularity", pagesInside.size()) ) );


                for (Element page: pagesInside)
                {
                    String extractedUrl = page.attr("abs:href");
                    if ( (extractedUrl.contains("http") || extractedUrl.contains("https") ) && extractedUrl!=null)
                    {
                        synchronized (toBeCrawledQueue)
                        {
                                if (toBeCrawledQueue.size()  >= NUM_OF_PAGES_TO_CRAWL) {
                                    // I can't add more
                                    continue;
                                }

                        }

                        mongoDbManager.addToCrawlingLog(extractedUrl);

                    }
                }


                mongoDbManager.toBeCrawledCollection.deleteOne(new Document("url", currentUrl));



            } catch (IOException e) {
                e.printStackTrace();
            }


        }

    }

    public static void main(String[] args) {

        mongoDbManager = new DbManager();

        // Fill toBeCrawled with seedUrls
        int state = mongoDbManager.getCrawlerState();
        if (state==0) {
            System.out.println("First Run: Filling Seed Set");
            mongoDbManager.initializeWithSeedUrls();
        }
        else {
            mongoDbManager.restoreHistory();
        }


        Thread[] crawlerThreads = new Thread[THREADS_COUNT];
        for (int currentThread = 0; currentThread < THREADS_COUNT; currentThread++) {
            crawlerThreads[currentThread] = new Thread(new Crawler());
            crawlerThreads[currentThread].start();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run()
            {
                mongoDbManager.setCrawlerState(1);
            }
        });

        try{
            for (int currentThread = 0; currentThread < THREADS_COUNT ; currentThread++) {
                crawlerThreads[currentThread].join();
            }
        }
        catch (InterruptedException e) {
            System.out.println("Thread interrupted");
        }

        System.out.println("Finished Crawling");


    }




}