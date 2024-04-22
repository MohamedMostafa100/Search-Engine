import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.jsoup.Jsoup;
import org.bson.Document;
import org.tartarus.snowball.ext.PorterStemmer;

import java.util.ArrayList;
import java.util.List;

import java.io.IOException;


public class Indexer {
    public static void main(String[] args) {

        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = mongoClient.getDatabase("MyGoogle");
        MongoCollection<Document> URLCollection = database.getCollection("CrawledUrls");
        MongoCollection<Document> WordCollection = database.getCollection("words");



        FindIterable<Document> fi = URLCollection.find();
        MongoCursor<Document> cursor = fi.iterator();
        int i=0;

        try {
            while(cursor.hasNext()) {
                Document links = cursor.next();
                ParseHTML(links.get("url").toString(), links.getInteger("popularity"), i, WordCollection);
                i++;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            cursor.close();
        }

    }

    public static String[] getSnippet(int index,String[] Word){
        List<String> list = new ArrayList<String>();
        int j=0;
        int start = index - 10;
        if(start < 0){
            start = 0;
        }
        int end = index + 10;
        if(end> Word.length){
            end = Word.length-1;
        }
        for(int i=start;i<end;i++){
            list.add(Word[i]);
            j++;
        }
        String[] snippet = list.toArray(new String[list.size()]);
        return snippet;
    }

    public static void ParseHTML(String url, int Popularity, int no_indexed,  MongoCollection<Document> WordCollection) throws IOException {
        org.jsoup.nodes.Document doc;
        try {
            doc = Jsoup.connect(url).get();
        } catch (java.net.UnknownHostException e) {
            // Handle the exception
            System.err.println("Skipping URL due to UnknownHostException: " + url);
            return;
        } catch (Exception e) {
            // Handle other exceptions, if necessary
            System.err.println("Error processing URL: " + url);
            e.printStackTrace();
            return;
        }
        String title = doc.title();
        String body = doc.body().text();
        String[] titleWords = title.split(" ");
        String[] bodyWords = body.split(" ");


        int i = 0;

        for (String word : titleWords) {
            if (word.equals("")) {
                continue;
            }
            word = word.toLowerCase();

            PorterStemmer stemmer = new PorterStemmer();
            stemmer.setCurrent(word);
            stemmer.stem();
            String stemmedWord = stemmer.getCurrent();

            Document foundWord = WordCollection.find(Filters.eq("word", stemmedWord)).first();

            if (foundWord == null) {
                Document newWord = new Document("word", stemmedWord);
                Document data = new Document();
                data.append("url", url);
                data.append("TF", 2);
                data.append("Snippet", String.join(" ",getSnippet(i, titleWords)));
                data.append("Popularity", Popularity);
                List<Document> URLS = new ArrayList<>();
                URLS.add(data);
                newWord.append("URLs_Data", URLS);
                newWord.append("No of documents", 1);
                WordCollection.insertOne(newWord);
            } else {
                int index = foundWord.getInteger("No of documents");
                String cmpUrl = foundWord.getList("URLs_Data", Document.class).get(index - 1).get("url").toString();
                //found in current document
                if (cmpUrl.equals(url)) {
                    WordCollection.updateOne(new Document("word", stemmedWord), new Document("$inc", new Document("URLs_Data." + (index - 1) + ".TF", 2)));
                }
                //found but in a different document
                else {
                    Document data = new Document();
                    data.append("url", url);
                    data.append("TF", 2);
                    data.append("Snippet", String.join(" ",getSnippet(i, titleWords)));
                    data.append("Popularity", Popularity);
                    WordCollection.updateOne(new Document("word", stemmedWord), new Document("$push", new Document("URLs_Data", data)));
                    WordCollection.updateOne(new Document("word", stemmedWord), new Document("$inc", new Document("No of documents", 1)));
                }

            }
            i++;
        }

        i = 0;

        if(title.isEmpty() || title.isBlank()){ //If no title intialize the key to the first non null word in the body (just key for searching)
            for (int j=0;j<bodyWords.length;j++) {
                if (bodyWords[j].equals("")) {
                    continue;
                }
                title = bodyWords[j];
            }
        }

        for (String word : bodyWords) {
            if (word.equals("")) {
                continue;
            }

            word = word.toLowerCase();

            PorterStemmer stemmer = new PorterStemmer();
            stemmer.setCurrent(word);
            stemmer.stem();
            String stemmedWord = stemmer.getCurrent();

            Document foundWord = WordCollection.find(Filters.eq("word", stemmedWord)).first();

            if (foundWord == null) {
                Document newWord = new Document("word", stemmedWord);
                Document data = new Document();
                data.append("url", url);
                data.append("TF", 1);
                data.append("Snippet",  String.join(" ",getSnippet(i, bodyWords)));
                data.append("Popularity", Popularity);
                List<Document> URLS = new ArrayList<>();
                URLS.add(data);
                newWord.append("URLs_Data", URLS);
                newWord.append("No of documents", 1);
                WordCollection.insertOne(newWord);
            } else {
                int index = foundWord.getInteger("No of documents");
                String cmpUrl = foundWord.getList("URLs_Data", Document.class).get(index - 1).get("url").toString();
                //found in current document
                if (cmpUrl.equals(url)) {
                    WordCollection.updateOne(new Document("word", stemmedWord), new Document("$inc", new Document("URLs_Data." + (index - 1) + ".TF", 1)));
                }
                //found but in a different document
                else {
                    Document data = new Document();
                    data.append("url", url);
                    data.append("TF", 1);
                    data.append("Snippet", String.join(" ",getSnippet(i, bodyWords)));
                    data.append("Popularity", Popularity);
                    WordCollection.updateOne(new Document("word", stemmedWord), new Document("$push", new Document("URLs_Data", data)));
                    WordCollection.updateOne(new Document("word", stemmedWord), new Document("$inc", new Document("No of documents", 1)));
                }

            }
            i++;
        }
    }



};

