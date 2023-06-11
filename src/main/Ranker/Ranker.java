import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.conversions.Bson;
import com.mongodb.*;

import java.lang.Math;
import static com.mongodb.client.model.Filters.*;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bson.Document;
import org.javatuples.Pair;

public class Ranker {
//    static Comparator<Entry<String, Double>> valueComparator = new Comparator<Entry<String,Double>>()
//    {
//        @Override public int compare(Entry<String, Double> e1, Entry<String, Double> e2)
//        { //hna2
//            Double v1 = e1.getValue();
//            Double v2 = e2.getValue();
//            return -Double.compare(v1, v2);
//        } };
//    public static List<Entry<String, Double>> calcScore(ArrayList<Document> docs) {
//        // ...
//
//        Map<String, Double> documentScores = new HashMap<>();
//
//        for (int i = 0; i < docs.size(); i++) {
//            Document tempDoc = docs.get(i);
//            int counter = (int) tempDoc.get("No of documents");
//            ArrayList<ArrayList<Object>> arr = (ArrayList<ArrayList<Object>>) tempDoc.get("URLs_Data");
//
//            double documentScore = 0.0;
//
//            for (int j = 0; j < arr.size(); j++) {
//                String url = (String) arr.get(j).get(0);
//                Bson filter = eq("URLs_Data.", url);
//                FindIterable f = collection.find(filter);
//                Document doc = (Document) f.first();
//                Double Popularity = Double.parseDouble(doc.get("popularity").toString());
//
//                double tfidf = (Double) arr.get(j).get(1) * Math.log10(6000.0 / counter) * Popularity;
//
//                documentScore += tfidf;
//            }
//
//            String url = (String) tempDoc.get("url");
//            documentScores.put(url, documentScore);
//        }
//
//        Set<Entry<String, Double>> entries = documentScores.entrySet();
//        List<Entry<String, Double>> sortedEntries = new ArrayList<>(entries);
//
//        Collections.sort(sortedEntries, valueComparator);
//
//        return sortedEntries;
    }