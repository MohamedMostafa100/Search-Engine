# Search Engine Project

This project aims to develop a simple Crawler-based search engine that demonstrates the main features of a search engine, including web crawling, indexing, and ranking, as well as the interaction between these components. The project is written in Java and is intended to enhance your Java programming skills.

## Features

The project consists of the following modules:

- **Web Crawler:** A software agent that collects documents from the web. The crawler starts with a list of URL addresses (seed set) and downloads the documents identified by these URLs while extracting hyperlinks from them. The extracted URLs are added to the list of URLs to be downloaded, making web crawling a recursive process. The crawler must take care not to visit the same page more than once, normalize URLs, and check if they are referring to the same page. It can only crawl documents of specific types, such as HTML. The crawler must maintain its state so that it can be started again if interrupted, and it must exclude pages from the search that have been chosen to be excluded by web administrators. The crawler is a separate program or process from the Indexer. The number of crawled pages is 6000 pages.

- **Indexer:** The output of the web crawling process is a set of downloaded HTML documents. The contents of these documents are indexed in a data structure that stores the words contained in each document and their importance, such as whether they are in the title, in a header, or in plain text. The index must be maintained in secondary storage and optimized for responding to queries. It must also allow incremental updates. When designing the Indexer, consider how you will store your result by looking ahead on Ranker and Searching.

- **Query Processor:** This module receives search queries, performs necessary preprocessing, and searches the index for relevant documents. It retrieves documents containing words that share the same stem with those in the search query.

- **Phrase Searching:** This module enables the search engine to search for words as phrases when quotation marks are placed around the phrase. Results obtained from phrase searching with quotation marks should return only the web pages having a sentence with the same order of words.

- **Ranker:** This module sorts documents based on their popularity and relevance to the search query. Relevance is a relation between the query words and the result page and could be calculated in several ways such as tf-idf of the query word in the result page or simply whether the query word appeared in the title, heading, or body. Popularity is a measure for the importance of any web page regardless of the requested query. The ranker module can use pagerank algorithm or other ranking algorithms to calculate each page's popularity.

- **Web Interface:** The search engine includes a web interface that receives user queries and displays the resulting pages returned by the engine. The results appear with snippets of the text containing query words, with the website's title and URL, and the paragraph containing the query words from the website having the query words in bold. The web interface also supports pagination of results and a suggestion mechanism that stores queries submitted by all users.

## Getting Started

To get started with the search engine, follow these steps:

1. Clone this repository to your local machine.
2. Open the project in an IDE of your choice, such as Eclipse or IntelliJ.
3. Build the project using Maven or Gradle.
4. Run the SearchEngine.java file in the src/main/java directory to start the search engine.

## Dependencies

The search engine project relies on the following dependencies:

- Jsoup: A Java library for working with real-world HTML.
- [Lucene](https://lucene.apache.org): A powerful Java library for full-text search indexing and search.```
