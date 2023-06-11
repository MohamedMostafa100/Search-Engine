import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import opennlp.tools.stemmer.PorterStemmer;

import static com.mongodb.client.model.Filters.eq;

public class Query_Processor {
    public static List<String> splitStringExceptBackticks(String input) {
        List<String> words = new ArrayList<>();

        Pattern pattern = Pattern.compile("`([^`]+)`|(\\S+)");
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                words.add(matcher.group(1)); // Preserve words within backticks
            } else {
//                words.add(matcher.group(2)); // Split other words
                String[] splitWords = matcher.group(2).split("-");
                for (String splitWord : splitWords)
                    words.add(splitWord); // Split other words including hyphens
            }
        }

        return words;
    }
    public static List<String> preprocess(String rawData) {
        String filePath = "stopwords.txt"; // Replace with the actual file path

        Map<String, Boolean> linesDictionary = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                linesDictionary.put(line, true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

//        String baseWord = "Hello, `how are you` today?";
        PorterStemmer porterStemmer = new PorterStemmer();
        List<String> split = splitStringExceptBackticks(rawData);
        String [] temp = split.toArray(new String[0]);
        ArrayList<String> wordsList = new ArrayList<String>();
        for (int i = 0; i < temp.length; i++) {
            temp[i] = temp[i].replaceAll("[(){}_.:;'!/@,^$%^&*?><-]", "").trim();
            if(linesDictionary.get(temp[i]) == null)
            {
                if(temp[i].contains(" "))
                {
                    wordsList.add(temp[i]);
                }
                else {
                    wordsList.add(porterStemmer.stem(temp[i]));
                }
            }
        }
        List<String> finalWordsList = wordsList.stream().distinct().collect(Collectors.toList());
        return finalWordsList;
    }
    public static ArrayList<Document> Search(List<String> s) {
        MongoClient client = new MongoClient("mongodb://localhost:27017");
        MongoDatabase database = client.getDatabase("MyGoogle");
        MongoCollection collection = database.getCollection("words");
        ArrayList<Document> allDocs = new ArrayList<Document>();
        for (int i = 0; i < s.size(); i++) {
            Bson filter = eq("word", s.get(i));
            FindIterable f = collection.find(filter);
            Document doc = (Document) f.first();
            allDocs.add(doc);
//            ArrayList<ArrayList<Object>> UrlsAndTF = (ArrayList<ArrayList<Object>>) doc.get("array");
        }
        return allDocs;
    }

    public static void main(String[] args) {
        String rawData = "hello my is name my is traveling lolo lol";
        List<String> finalWordsList = preprocess(rawData);
        System.out.println(finalWordsList);
        ArrayList<Document> alldocs = Search(finalWordsList);
        System.out.println(alldocs);
//        String input = "I am `playing football` now";
//        List<String> result = splitStringExceptBackticks(input);
//
//        for (String word : result) {
//            System.out.println(word);
//        }
//        ArrayList<Document> docs = new ArrayList<Document>();
//        docs.add();
    }
}
