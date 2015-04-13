import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

public class HelloLucene {
  public static void main(String[] args) throws IOException, ParseException {
    // 0. Specify the analyzer for tokenizing text.
    //    The same analyzer should be used for indexing and searching
    StandardAnalyzer analyzer = new StandardAnalyzer();

    // 1. create the index
    Directory index = new RAMDirectory();

    IndexWriterConfig config = new IndexWriterConfig( analyzer);

    IndexWriter w = new IndexWriter(index, config);
    addDocCollection(w);    
    w.close();

    // 2. query
    String querystr = args.length > 0 ? args[0] : "Copa";

    // the "title" arg specifies the default field to use
    // when no field is explicitly specified in the query.
    Query q = new QueryParser( "text", analyzer).parse(querystr);

    // 3. search
    int hitsPerPage = 200;
    IndexReader reader = DirectoryReader.open(index);
    IndexSearcher searcher = new IndexSearcher(reader);
    TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage);
    searcher.search(q, collector);
    ScoreDoc[] hits = collector.topDocs().scoreDocs;
   
    // 4. display results
    System.out.println("Found " + hits.length + " hits.");
    for(int i=0;i<hits.length;++i) {
      int docId = hits[i].doc;
      Document d = searcher.doc(docId);
      System.out.println((i + 1) + ". " + d.get("path") + "\t" + d.get("title"));
    }

    // reader can only be closed when there
    // is no need to access the documents any more.
    reader.close();
  }

  private static void addDocCollection(IndexWriter w) throws IOException {    
    File dir = new File("/home/rcrapacki/Downloads/efe"); 
    File[] files = dir.listFiles();
	for (File file : files) {
		DocumentParser docParser = new DocumentParser(file);
		
		for (SaxDocument saxDocument : docParser.getDocumentList()) {
			Document document = new Document();

			String path = file.getCanonicalPath();
		    // use a string field for path because we don't want it tokenized
			document.add(new StringField("path", path, Field.Store.YES));
			
			document.add(new TextField("text", saxDocument.getText(), Field.Store.YES));
			document.add(new TextField("title", saxDocument.getTitle(), Field.Store.YES));

			w.addDocument(document);
		}
	}
  }
}